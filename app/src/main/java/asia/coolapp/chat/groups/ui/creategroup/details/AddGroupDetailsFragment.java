package asia.coolapp.chat.groups.ui.creategroup.details;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dd.CircularProgressButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.EditTextUtil;
import asia.coolapp.chat.LoggingFragment;
import asia.coolapp.chat.R;
import asia.coolapp.chat.avatar.picker.AvatarPickerFragment;
import asia.coolapp.chat.components.settings.app.privacy.expire.ExpireTimerSettingsFragment;
import asia.coolapp.chat.groups.ui.GroupMemberListView;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.mms.DecryptableStreamUriLoader;
import asia.coolapp.chat.mms.GlideApp;
import asia.coolapp.chat.profiles.AvatarHelper;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.recipients.ui.disappearingmessages.RecipientDisappearingMessagesActivity;
import asia.coolapp.chat.util.BitmapUtil;
import asia.coolapp.chat.util.ExpirationUtil;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.ViewUtil;
import asia.coolapp.chat.util.navigation.SafeNavigation;
import asia.coolapp.chat.util.text.AfterTextChanged;
import asia.coolapp.chat.util.views.LearnMoreTextView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AddGroupDetailsFragment extends LoggingFragment {

  private static final int   AVATAR_PLACEHOLDER_INSET_DP = 18;
  private static final short REQUEST_DISAPPEARING_TIMER  = 28621;

  private CircularProgressButton   create;
  private Callback                 callback;
  private AddGroupDetailsViewModel viewModel;
  private Drawable                 avatarPlaceholder;
  private EditText                 name;
  private Toolbar                  toolbar;
  private View                     disappearingMessagesRow;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    if (context instanceof Callback) {
      callback = (Callback) context;
    } else {
      throw new ClassCastException("Parent context should implement AddGroupDetailsFragment.Callback");
    }
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.add_group_details_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    create                  = view.findViewById(R.id.create);
    name                    = view.findViewById(R.id.name);
    toolbar                 = view.findViewById(R.id.toolbar);
    disappearingMessagesRow = view.findViewById(R.id.group_disappearing_messages_row);

    setCreateEnabled(false, false);

    GroupMemberListView members                  = view.findViewById(R.id.member_list);
    ImageView           avatar                   = view.findViewById(R.id.group_avatar);
    View                mmsWarning               = view.findViewById(R.id.mms_warning);
    LearnMoreTextView   gv2Warning               = view.findViewById(R.id.gv2_warning);
    View                addLater                 = view.findViewById(R.id.add_later);
    TextView            disappearingMessageValue = view.findViewById(R.id.group_disappearing_messages_value);

    members.initializeAdapter(getViewLifecycleOwner());
    avatarPlaceholder = VectorDrawableCompat.create(getResources(), R.drawable.ic_camera_outline_32_ultramarine, requireActivity().getTheme());

    if (savedInstanceState == null) {
      avatar.setImageDrawable(new InsetDrawable(avatarPlaceholder, ViewUtil.dpToPx(AVATAR_PLACEHOLDER_INSET_DP)));
    }

    initializeViewModel();

    avatar.setOnClickListener(v -> showAvatarPicker());
    members.setRecipientClickListener(this::handleRecipientClick);
    EditTextUtil.addGraphemeClusterLimitFilter(name, FeatureFlags.getMaxGroupNameGraphemeLength());
    name.addTextChangedListener(new AfterTextChanged(editable -> viewModel.setName(editable.toString())));
    toolbar.setNavigationOnClickListener(unused -> callback.onNavigationButtonPressed());
    create.setOnClickListener(v -> handleCreateClicked());
    viewModel.getMembers().observe(getViewLifecycleOwner(), list -> {
      addLater.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
      members.setMembers(list);
    });
    viewModel.getCanSubmitForm().observe(getViewLifecycleOwner(), isFormValid -> setCreateEnabled(isFormValid, true));
    viewModel.getIsMms().observe(getViewLifecycleOwner(), isMms -> {
      disappearingMessagesRow.setVisibility(isMms ? View.GONE : View.VISIBLE);
      mmsWarning.setVisibility(isMms ? View.VISIBLE : View.GONE);
      name.setHint(isMms ? R.string.AddGroupDetailsFragment__group_name_optional : R.string.AddGroupDetailsFragment__group_name_required);
      toolbar.setTitle(isMms ? R.string.AddGroupDetailsFragment__create_group : R.string.AddGroupDetailsFragment__name_this_group);
    });
    viewModel.getAvatar().observe(getViewLifecycleOwner(), avatarBytes -> {
      if (avatarBytes == null) {
        avatar.setImageDrawable(new InsetDrawable(avatarPlaceholder, ViewUtil.dpToPx(AVATAR_PLACEHOLDER_INSET_DP)));
      } else {
        GlideApp.with(this)
                .load(avatarBytes)
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(avatar);
      }
    });

    viewModel.getDisappearingMessagesTimer().observe(getViewLifecycleOwner(), timer -> disappearingMessageValue.setText(ExpirationUtil.getExpirationDisplayValue(requireContext(), timer)));
    disappearingMessagesRow.setOnClickListener(v -> {
      startActivityForResult(RecipientDisappearingMessagesActivity.forCreateGroup(requireContext(), viewModel.getDisappearingMessagesTimer().getValue()), REQUEST_DISAPPEARING_TIMER);
    });

    name.requestFocus();

    getParentFragmentManager().setFragmentResultListener(AvatarPickerFragment.REQUEST_KEY_SELECT_AVATAR,
                                                         getViewLifecycleOwner(),
                                                         (key, bundle) -> handleMediaResult(bundle));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_DISAPPEARING_TIMER && resultCode == Activity.RESULT_OK && data != null) {
      viewModel.setDisappearingMessageTimer(data.getIntExtra(ExpireTimerSettingsFragment.FOR_RESULT_VALUE, SignalStore.settings().getUniversalExpireTimer()));
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void handleMediaResult(Bundle data) {
    if (data.getBoolean(AvatarPickerFragment.SELECT_AVATAR_CLEAR)) {
      viewModel.setAvatarMedia(null);
      viewModel.setAvatar(null);
      return;
    }

    final Media result                                             = data.getParcelable(AvatarPickerFragment.SELECT_AVATAR_MEDIA);
    final DecryptableStreamUriLoader.DecryptableUri decryptableUri = new DecryptableStreamUriLoader.DecryptableUri(result.getUri());

    viewModel.setAvatarMedia(result);

    GlideApp.with(this)
            .asBitmap()
            .load(decryptableUri)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
            .override(AvatarHelper.AVATAR_DIMENSIONS, AvatarHelper.AVATAR_DIMENSIONS)
            .into(new CustomTarget<Bitmap>() {
              @Override
              public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                viewModel.setAvatar(Objects.requireNonNull(BitmapUtil.toByteArray(resource)));
              }

              @Override
              public void onLoadCleared(@Nullable Drawable placeholder) {
              }
            });
  }

  private void initializeViewModel() {
    AddGroupDetailsFragmentArgs      args       = AddGroupDetailsFragmentArgs.fromBundle(requireArguments());
    AddGroupDetailsRepository        repository = new AddGroupDetailsRepository(requireContext());
    AddGroupDetailsViewModel.Factory factory    = new AddGroupDetailsViewModel.Factory(Arrays.asList(args.getRecipientIds()), repository);

    viewModel = ViewModelProviders.of(this, factory).get(AddGroupDetailsViewModel.class);

    viewModel.getGroupCreateResult().observe(getViewLifecycleOwner(), this::handleGroupCreateResult);
  }

  private void handleCreateClicked() {
    create.setClickable(false);
    create.setIndeterminateProgressMode(true);
    create.setProgress(50);

    viewModel.create();
  }

  private void handleRecipientClick(@NonNull Recipient recipient) {
    new MaterialAlertDialogBuilder(requireContext())
        .setMessage(getString(R.string.AddGroupDetailsFragment__remove_s_from_this_group, recipient.getDisplayName(requireContext())))
        .setCancelable(true)
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
        .setPositiveButton(R.string.AddGroupDetailsFragment__remove, (dialog, which) -> {
          viewModel.delete(recipient.getId());
          dialog.dismiss();
        })
        .show();
  }

  private void handleGroupCreateResult(@NonNull GroupCreateResult groupCreateResult) {
    groupCreateResult.consume(this::handleGroupCreateResultSuccess, this::handleGroupCreateResultError);
  }

  private void handleGroupCreateResultSuccess(@NonNull GroupCreateResult.Success success) {
    callback.onGroupCreated(success.getGroupRecipient().getId(), success.getThreadId(), success.getInvitedMembers());
  }

  private void handleGroupCreateResultError(@NonNull GroupCreateResult.Error error) {
    switch (error.getErrorType()) {
      case ERROR_IO:
      case ERROR_BUSY:
        toast(R.string.AddGroupDetailsFragment__try_again_later);
        break;
      case ERROR_FAILED:
        toast(R.string.AddGroupDetailsFragment__group_creation_failed);
        break;
      case ERROR_INVALID_NAME:
        name.setError(getString(R.string.AddGroupDetailsFragment__this_field_is_required));
        break;
      default:
        throw new IllegalStateException("Unexpected error: " + error.getErrorType().name());
    }
  }

  private void toast(@StringRes int toastStringId) {
    Toast.makeText(requireContext(), toastStringId, Toast.LENGTH_SHORT)
         .show();
  }

  private void setCreateEnabled(boolean isEnabled, boolean animate) {
    if (create.isEnabled() == isEnabled) {
      return;
    }

    create.setEnabled(isEnabled);
    create.animate()
          .setDuration(animate ? 300 : 0)
          .alpha(isEnabled ? 1f : 0.5f);
  }

  private void showAvatarPicker() {
    Media media = viewModel.getAvatarMedia();

    SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), AddGroupDetailsFragmentDirections.actionAddGroupDetailsFragmentToAvatarPicker(null, media).setIsNewGroup(true));
  }

  public interface Callback {
    void onGroupCreated(@NonNull RecipientId recipientId, long threadId, @NonNull List<Recipient> invitedMembers);

    void onNavigationButtonPressed();
  }
}
