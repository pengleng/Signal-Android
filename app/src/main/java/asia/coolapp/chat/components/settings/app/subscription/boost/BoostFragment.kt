package asia.coolapp.chat.components.settings.app.subscription.boost

import android.content.DialogInterface
import android.text.SpannableStringBuilder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.signal.core.util.DimensionUnit
import org.signal.core.util.logging.Log
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.badges.models.BadgePreview
import asia.coolapp.chat.components.KeyboardAwareLinearLayout
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.app.subscription.DonationEvent
import asia.coolapp.chat.components.settings.app.subscription.DonationPaymentComponent
import asia.coolapp.chat.components.settings.app.subscription.errors.DonationError
import asia.coolapp.chat.components.settings.app.subscription.errors.DonationErrorDialogs
import asia.coolapp.chat.components.settings.app.subscription.errors.DonationErrorSource
import asia.coolapp.chat.components.settings.app.subscription.models.CurrencySelection
import asia.coolapp.chat.components.settings.app.subscription.models.GooglePayButton
import asia.coolapp.chat.components.settings.app.subscription.models.NetworkFailure
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.components.settings.models.Progress
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.util.BottomSheetUtil.requireCoordinatorLayout
import asia.coolapp.chat.util.CommunicationActions
import asia.coolapp.chat.util.LifecycleDisposable
import asia.coolapp.chat.util.Projection
import asia.coolapp.chat.util.SpanUtil
import asia.coolapp.chat.util.fragments.requireListener
import asia.coolapp.chat.util.navigation.safeNavigate

/**
 * UX to allow users to donate ephemerally.
 */
class BoostFragment : DSLSettingsBottomSheetFragment(
  layoutId = R.layout.boost_bottom_sheet
) {

  private val viewModel: BoostViewModel by viewModels(
    factoryProducer = {
      BoostViewModel.Factory(BoostRepository(ApplicationDependencies.getDonationsService()), donationPaymentComponent.donationPaymentRepository, FETCH_BOOST_TOKEN_REQUEST_CODE)
    }
  )

  private val lifecycleDisposable = LifecycleDisposable()

  private lateinit var boost1AnimationView: LottieAnimationView
  private lateinit var boost2AnimationView: LottieAnimationView
  private lateinit var boost3AnimationView: LottieAnimationView
  private lateinit var boost4AnimationView: LottieAnimationView
  private lateinit var boost5AnimationView: LottieAnimationView
  private lateinit var boost6AnimationView: LottieAnimationView

  private lateinit var processingDonationPaymentDialog: AlertDialog
  private lateinit var donationPaymentComponent: DonationPaymentComponent

  private var errorDialog: DialogInterface? = null

  private val sayThanks: CharSequence by lazy {
    SpannableStringBuilder(requireContext().getString(R.string.BoostFragment__say_thanks_and_earn, 30))
      .append(" ")
      .append(
        SpanUtil.learnMore(requireContext(), ContextCompat.getColor(requireContext(), R.color.signal_accent_primary)) {
          CommunicationActions.openBrowserLink(requireContext(), getString(R.string.sustainer_boost_and_badges))
        }
      )
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    donationPaymentComponent = requireListener()
    viewModel.refresh()

    CurrencySelection.register(adapter)
    BadgePreview.register(adapter)
    Boost.register(adapter)
    GooglePayButton.register(adapter)
    Progress.register(adapter)
    NetworkFailure.register(adapter)
    BoostAnimation.register(adapter)

    processingDonationPaymentDialog = MaterialAlertDialogBuilder(requireContext())
      .setView(R.layout.processing_payment_dialog)
      .setCancelable(false)
      .create()

    recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS

    boost1AnimationView = requireView().findViewById(R.id.boost1_animation)
    boost2AnimationView = requireView().findViewById(R.id.boost2_animation)
    boost3AnimationView = requireView().findViewById(R.id.boost3_animation)
    boost4AnimationView = requireView().findViewById(R.id.boost4_animation)
    boost5AnimationView = requireView().findViewById(R.id.boost5_animation)
    boost6AnimationView = requireView().findViewById(R.id.boost6_animation)

    KeyboardAwareLinearLayout(requireContext()).apply {
      addOnKeyboardHiddenListener {
        recyclerView.post { recyclerView.requestLayout() }
      }

      addOnKeyboardShownListener {
        recyclerView.post { recyclerView.scrollToPosition(adapter.itemCount - 1) }
      }

      requireCoordinatorLayout().addView(this)
    }

    viewModel.state.observe(viewLifecycleOwner) { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }

    lifecycleDisposable.bindTo(viewLifecycleOwner.lifecycle)
    lifecycleDisposable += viewModel.events.subscribe { event: DonationEvent ->
      when (event) {
        is DonationEvent.PaymentConfirmationSuccess -> onPaymentConfirmed(event.badge)
        DonationEvent.RequestTokenSuccess -> Log.i(TAG, "Successfully got request token from Google Pay")
        DonationEvent.SubscriptionCancelled -> Unit
        is DonationEvent.SubscriptionCancellationFailed -> Unit
      }
    }
    lifecycleDisposable += donationPaymentComponent.googlePayResultPublisher.subscribe {
      viewModel.onActivityResult(it.requestCode, it.resultCode, it.data)
    }

    lifecycleDisposable += DonationError
      .getErrorsForSource(DonationErrorSource.BOOST)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { donationError ->
        onPaymentError(donationError)
      }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    processingDonationPaymentDialog.hide()
  }

  private fun getConfiguration(state: BoostState): DSLConfiguration {
    if (state.stage == BoostState.Stage.PAYMENT_PIPELINE) {
      processingDonationPaymentDialog.show()
    } else {
      processingDonationPaymentDialog.hide()
    }

    return configure {
      customPref(BoostAnimation.Model())

      sectionHeaderPref(
        title = DSLSettingsText.from(
          R.string.BoostFragment__give_signal_a_boost,
          DSLSettingsText.CenterModifier, DSLSettingsText.Title2BoldModifier
        )
      )

      noPadTextPref(
        title = DSLSettingsText.from(
          sayThanks,
          DSLSettingsText.CenterModifier
        )
      )

      space(DimensionUnit.DP.toPixels(28f).toInt())

      customPref(
        CurrencySelection.Model(
          selectedCurrency = state.currencySelection,
          isEnabled = state.stage == BoostState.Stage.READY,
          onClick = {
            findNavController().safeNavigate(BoostFragmentDirections.actionBoostFragmentToSetDonationCurrencyFragment(true, viewModel.getSupportedCurrencyCodes().toTypedArray()))
          }
        )
      )

      @Suppress("CascadeIf")
      if (state.stage == BoostState.Stage.INIT) {
        customPref(
          Boost.LoadingModel()
        )
      } else if (state.stage == BoostState.Stage.FAILURE) {
        space(DimensionUnit.DP.toPixels(20f).toInt())
        customPref(
          NetworkFailure.Model {
            viewModel.retry()
          }
        )
      } else {
        customPref(
          Boost.SelectionModel(
            boosts = state.boosts,
            selectedBoost = state.selectedBoost,
            currency = state.customAmount.currency,
            isCustomAmountFocused = state.isCustomAmountFocused,
            isEnabled = state.stage == BoostState.Stage.READY,
            onBoostClick = { view, boost ->
              startAnimationAboveSelectedBoost(view)
              viewModel.setSelectedBoost(boost)
            },
            onCustomAmountChanged = {
              viewModel.setCustomAmount(it)
            },
            onCustomAmountFocusChanged = {
              if (it) {
                viewModel.setCustomAmountFocused()
              }
            }
          )
        )
      }

      space(DimensionUnit.DP.toPixels(16f).toInt())

      customPref(
        GooglePayButton.Model(
          onClick = this@BoostFragment::onGooglePayButtonClicked,
          isEnabled = state.stage == BoostState.Stage.READY
        )
      )

      secondaryButtonNoOutline(
        text = DSLSettingsText.from(R.string.SubscribeFragment__more_payment_options),
        icon = DSLSettingsIcon.from(R.drawable.ic_open_20, R.color.signal_accent_primary),
        onClick = {
          CommunicationActions.openBrowserLink(requireContext(), getString(R.string.donate_url))
        }
      )
    }
  }

  private fun onGooglePayButtonClicked() {
    viewModel.requestTokenFromGooglePay(getString(R.string.preferences__signal_boost))
  }

  private fun onPaymentConfirmed(boostBadge: Badge) {
    findNavController().safeNavigate(
      BoostFragmentDirections.actionBoostFragmentToBoostThanksForYourSupportBottomSheetDialog(boostBadge).setIsBoost(true),
      NavOptions.Builder().setPopUpTo(R.id.boostFragment, true).build()
    )
  }

  private fun onPaymentError(throwable: Throwable?) {
    Log.w(TAG, "onPaymentError", throwable, true)

    if (errorDialog != null) {
      Log.i(TAG, "Already displaying an error dialog. Skipping.")
      return
    }

    errorDialog = DonationErrorDialogs.show(
      requireContext(), throwable,
      object : DonationErrorDialogs.DialogCallback() {
        override fun onDialogDismissed() {
          findNavController().popBackStack()
        }
      }
    )
  }

  private fun startAnimationAboveSelectedBoost(view: View) {
    val animationView = getAnimationContainer(view)
    val viewProjection = Projection.relativeToViewRoot(view, null)
    val animationProjection = Projection.relativeToViewRoot(animationView, null)
    val viewHorizontalCenter = viewProjection.x + viewProjection.width / 2f
    val animationHorizontalCenter = animationProjection.x + animationProjection.width / 2f
    val animationBottom = animationProjection.y + animationProjection.height

    animationView.translationY = -(animationBottom - viewProjection.y) + (viewProjection.height / 2f)
    animationView.translationX = viewHorizontalCenter - animationHorizontalCenter

    animationView.playAnimation()

    viewProjection.release()
    animationProjection.release()
  }

  private fun getAnimationContainer(view: View): LottieAnimationView {
    return when (view.id) {
      R.id.boost_1 -> boost1AnimationView
      R.id.boost_2 -> boost2AnimationView
      R.id.boost_3 -> boost3AnimationView
      R.id.boost_4 -> boost4AnimationView
      R.id.boost_5 -> boost5AnimationView
      R.id.boost_6 -> boost6AnimationView
      else -> throw AssertionError()
    }
  }

  companion object {
    private val TAG = Log.tag(BoostFragment::class.java)
    private const val FETCH_BOOST_TOKEN_REQUEST_CODE = 2000
  }
}
