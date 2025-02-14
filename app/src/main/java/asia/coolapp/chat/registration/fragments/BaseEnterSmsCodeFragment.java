package asia.coolapp.chat.registration.fragments;

import android.animation.Animator;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.LoggingFragment;
import asia.coolapp.chat.R;
import asia.coolapp.chat.components.registration.CallMeCountDownView;
import asia.coolapp.chat.components.registration.VerificationCodeView;
import asia.coolapp.chat.components.registration.VerificationPinKeyboard;
import asia.coolapp.chat.registration.ReceivedSmsEvent;
import asia.coolapp.chat.registration.VerifyAccountRepository;
import asia.coolapp.chat.registration.viewmodel.BaseRegistrationViewModel;
import asia.coolapp.chat.util.CommunicationActions;
import asia.coolapp.chat.util.LifecycleDisposable;
import asia.coolapp.chat.util.SupportEmailUtil;
import asia.coolapp.chat.util.ViewUtil;
import asia.coolapp.chat.util.concurrent.AssertedSuccessListener;
import org.whispersystems.signalservice.internal.push.LockedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import static asia.coolapp.chat.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView;
import static asia.coolapp.chat.registration.fragments.RegistrationViewDelegate.showConfirmNumberDialogIfTranslated;

/**
 * Base fragment used by registration and change number flow to input an SMS verification code or request a
 * phone code after requesting SMS.
 *
 * @param <ViewModel> - The concrete view model used by the subclasses, for ease of access in said subclass
 */
public abstract class BaseEnterSmsCodeFragment<ViewModel extends BaseRegistrationViewModel> extends LoggingFragment implements SignalStrengthPhoneStateListener.Callback {

  private static final String TAG = Log.tag(BaseEnterSmsCodeFragment.class);

  private ScrollView              scrollView;
  private TextView                header;
  private VerificationCodeView    verificationCodeView;
  private VerificationPinKeyboard keyboard;
  private CallMeCountDownView     callMeCountDown;
  private View                    wrongNumber;
  private View                    noCodeReceivedHelp;
  private View                    serviceWarning;
  private boolean                 autoCompleting;

  private ViewModel viewModel;

  protected final LifecycleDisposable disposables = new LifecycleDisposable();

  public BaseEnterSmsCodeFragment(@LayoutRes int contentLayoutId) {
    super(contentLayoutId);
  }

  @Override
  @CallSuper
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setDebugLogSubmitMultiTapView(view.findViewById(R.id.verify_header));

    scrollView           = view.findViewById(R.id.scroll_view);
    header               = view.findViewById(R.id.verify_header);
    verificationCodeView = view.findViewById(R.id.code);
    keyboard             = view.findViewById(R.id.keyboard);
    callMeCountDown      = view.findViewById(R.id.call_me_count_down);
    wrongNumber          = view.findViewById(R.id.wrong_number);
    noCodeReceivedHelp   = view.findViewById(R.id.no_code);
    serviceWarning       = view.findViewById(R.id.cell_service_warning);

    new SignalStrengthPhoneStateListener(this, this);

    connectKeyboard(verificationCodeView, keyboard);
    ViewUtil.hideKeyboard(requireContext(), view);

    setOnCodeFullyEnteredListener(verificationCodeView);

    wrongNumber.setOnClickListener(v -> onWrongNumber());

    callMeCountDown.setOnClickListener(v -> handlePhoneCallRequest());

    callMeCountDown.setListener((v, remaining) -> {
      if (remaining <= 30) {
        scrollView.smoothScrollTo(0, v.getBottom());
        callMeCountDown.setListener(null);
      }
    });

    noCodeReceivedHelp.setOnClickListener(v -> sendEmailToSupport());

    disposables.bindTo(getViewLifecycleOwner().getLifecycle());
    viewModel = getViewModel();
    viewModel.getSuccessfulCodeRequestAttempts().observe(getViewLifecycleOwner(), (attempts) -> {
      if (attempts >= 3) {
        noCodeReceivedHelp.setVisibility(View.VISIBLE);
        scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, noCodeReceivedHelp.getBottom()), 15000);
      }
    });

    viewModel.onStartEnterCode();
  }

  protected abstract ViewModel getViewModel();

  protected abstract void handleSuccessfulVerify();

  protected abstract void navigateToCaptcha();

  protected abstract void navigateToRegistrationLock(long timeRemaining);

  protected abstract void navigateToKbsAccountLocked();

  private void onWrongNumber() {
    Navigation.findNavController(requireView()).navigateUp();
  }

  private void setOnCodeFullyEnteredListener(VerificationCodeView verificationCodeView) {
    verificationCodeView.setOnCompleteListener(code -> {

      callMeCountDown.setVisibility(View.INVISIBLE);
      wrongNumber.setVisibility(View.INVISIBLE);
      keyboard.displayProgress();

      Disposable verify = viewModel.verifyCodeWithoutRegistrationLock(code)
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .subscribe(processor -> {
                                     if (!processor.hasResult()) {
                                       Log.w(TAG, "post verify: ", processor.getError());
                                     }
                                     if (processor.hasResult()) {
                                       handleSuccessfulVerify();
                                     } else if (processor.rateLimit()) {
                                       handleRateLimited();
                                     } else if (processor.registrationLock() && !processor.isKbsLocked()) {
                                       LockedException lockedException = processor.getLockedException();
                                       handleRegistrationLock(lockedException.getTimeRemaining());
                                     } else if (processor.isKbsLocked()) {
                                       handleKbsAccountLocked();
                                     } else if (processor.authorizationFailed()) {
                                       handleIncorrectCodeError();
                                     } else {
                                       Log.w(TAG, "Unable to verify code", processor.getError());
                                       handleGeneralError();
                                     }
                                   });

      disposables.add(verify);
    });
  }

  protected void displaySuccess(@NonNull Runnable runAfterAnimation) {
    keyboard.displaySuccess().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        runAfterAnimation.run();
      }
    });
  }

  protected void handleRateLimited() {
    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean r) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        builder.setTitle(R.string.RegistrationActivity_too_many_attempts)
               .setMessage(R.string.RegistrationActivity_you_have_made_too_many_attempts_please_try_again_later)
               .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                 callMeCountDown.setVisibility(View.VISIBLE);
                 wrongNumber.setVisibility(View.VISIBLE);
                 verificationCodeView.clear();
                 keyboard.displayKeyboard();
               })
               .show();
      }
    });
  }

  protected void handleRegistrationLock(long timeRemaining) {
    keyboard.displayLocked().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean r) {
        navigateToRegistrationLock(timeRemaining);
      }
    });
  }

  protected void handleKbsAccountLocked() {
    navigateToKbsAccountLocked();
  }

  protected void handleIncorrectCodeError() {
    Toast.makeText(requireContext(), R.string.RegistrationActivity_incorrect_code, Toast.LENGTH_LONG).show();
    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        callMeCountDown.setVisibility(View.VISIBLE);
        wrongNumber.setVisibility(View.VISIBLE);
        verificationCodeView.clear();
        keyboard.displayKeyboard();
      }
    });
  }

  protected void handleGeneralError() {
    Toast.makeText(requireContext(), R.string.RegistrationActivity_error_connecting_to_service, Toast.LENGTH_LONG).show();
    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        callMeCountDown.setVisibility(View.VISIBLE);
        wrongNumber.setVisibility(View.VISIBLE);
        verificationCodeView.clear();
        keyboard.displayKeyboard();
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onVerificationCodeReceived(@NonNull ReceivedSmsEvent event) {
    verificationCodeView.clear();

    List<Integer> parsedCode = convertVerificationCodeToDigits(event.getCode());

    autoCompleting = true;

    final int size = parsedCode.size();

    for (int i = 0; i < size; i++) {
      final int index = i;
      verificationCodeView.postDelayed(() -> {
        verificationCodeView.append(parsedCode.get(index));
        if (index == size - 1) {
          autoCompleting = false;
        }
      }, i * 200L);
    }
  }

  private static List<Integer> convertVerificationCodeToDigits(@Nullable String code) {
    if (code == null || code.length() != 6) {
      return Collections.emptyList();
    }

    List<Integer> result = new ArrayList<>(code.length());

    try {
      for (int i = 0; i < code.length(); i++) {
        result.add(Integer.parseInt(Character.toString(code.charAt(i))));
      }
    } catch (NumberFormatException e) {
      Log.w(TAG, "Failed to convert code into digits.", e);
      return Collections.emptyList();
    }

    return result;
  }

  private void handlePhoneCallRequest() {
    showConfirmNumberDialogIfTranslated(requireContext(),
                                        R.string.RegistrationActivity_you_will_receive_a_call_to_verify_this_number,
                                        viewModel.getNumber().getE164Number(),
                                        this::handlePhoneCallRequestAfterConfirm,
                                        this::onWrongNumber);
  }

  private void handlePhoneCallRequestAfterConfirm() {
    Disposable request = viewModel.requestVerificationCode(VerifyAccountRepository.Mode.PHONE_CALL)
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(processor -> {
                                    if (processor.hasResult()) {
                                      Toast.makeText(requireContext(), R.string.RegistrationActivity_call_requested, Toast.LENGTH_LONG).show();
                                    } else if (processor.captchaRequired()) {
                                      navigateToCaptcha();
                                    } else if (processor.rateLimit()) {
                                      Toast.makeText(requireContext(), R.string.RegistrationActivity_rate_limited_to_service, Toast.LENGTH_LONG).show();
                                    } else {
                                      Log.w(TAG, "Unable to request phone code", processor.getError());
                                      Toast.makeText(requireContext(), R.string.RegistrationActivity_unable_to_connect_to_service, Toast.LENGTH_LONG).show();
                                    }
                                  });

    disposables.add(request);
  }

  private void connectKeyboard(VerificationCodeView verificationCodeView, VerificationPinKeyboard keyboard) {
    keyboard.setOnKeyPressListener(key -> {
      if (!autoCompleting) {
        if (key >= 0) {
          verificationCodeView.append(key);
        } else {
          verificationCodeView.delete();
        }
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    header.setText(requireContext().getString(R.string.RegistrationActivity_enter_the_code_we_sent_to_s, viewModel.getNumber().getFullFormattedNumber()));

    viewModel.getCanCallAtTime().observe(getViewLifecycleOwner(), callAtTime -> callMeCountDown.startCountDownTo(callAtTime));
  }

  private void sendEmailToSupport() {
    String body = SupportEmailUtil.generateSupportEmailBody(requireContext(),
                                                            R.string.RegistrationActivity_code_support_subject,
                                                            null,
                                                            null);
    CommunicationActions.openEmail(requireContext(),
                                   SupportEmailUtil.getSupportEmailAddress(requireContext()),
                                   getString(R.string.RegistrationActivity_code_support_subject),
                                   body);
  }

  @Override
  public void onNoCellSignalPresent() {
    if (serviceWarning.getVisibility() == View.VISIBLE) {
      return;
    }
    serviceWarning.setVisibility(View.VISIBLE);
    serviceWarning.animate()
                  .alpha(1)
                  .setListener(null)
                  .start();

    scrollView.postDelayed(() -> {
      if (serviceWarning.getVisibility() == View.VISIBLE) {
        scrollView.smoothScrollTo(0, serviceWarning.getBottom());
      }
    }, 1000);
  }

  @Override
  public void onCellSignalPresent() {
    if (serviceWarning.getVisibility() != View.VISIBLE) {
      return;
    }
    serviceWarning.animate()
                  .alpha(0)
                  .setListener(new Animator.AnimatorListener() {
                    @Override public void onAnimationEnd(Animator animation) {
                      serviceWarning.setVisibility(View.GONE);
                    }

                    @Override public void onAnimationStart(Animator animation) {}

                    @Override public void onAnimationCancel(Animator animation) {}

                    @Override public void onAnimationRepeat(Animator animation) {}
                  })
                  .start();
  }
}
