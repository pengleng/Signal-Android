package asia.coolapp.chat.profiles.manage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.signal.core.util.StreamUtil;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.badges.models.Badge;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.jobs.RetrieveProfileJob;
import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.profiles.AvatarHelper;
import asia.coolapp.chat.profiles.ProfileName;
import asia.coolapp.chat.providers.BlobProvider;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientForeverObserver;
import asia.coolapp.chat.util.DefaultValueLiveData;
import asia.coolapp.chat.util.FeatureFlags;
import asia.coolapp.chat.util.SingleLiveEvent;
import asia.coolapp.chat.util.livedata.LiveDataUtil;
import org.whispersystems.signalservice.api.util.StreamDetails;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

class ManageProfileViewModel extends ViewModel {

  private static final String TAG = Log.tag(ManageProfileViewModel.class);

  private final MutableLiveData<InternalAvatarState> internalAvatarState;
  private final MutableLiveData<ProfileName>         profileName;
  private final MutableLiveData<String>              username;
  private final MutableLiveData<String>              about;
  private final MutableLiveData<String>              aboutEmoji;
  private final LiveData<AvatarState>                avatarState;
  private final SingleLiveEvent<Event>               events;
  private final RecipientForeverObserver             observer;
  private final ManageProfileRepository          repository;
  private final MutableLiveData<Optional<Badge>> badge;

  private byte[] previousAvatar;

  public ManageProfileViewModel() {
    this.internalAvatarState = new MutableLiveData<>();
    this.profileName         = new MutableLiveData<>();
    this.username            = new MutableLiveData<>();
    this.about               = new MutableLiveData<>();
    this.aboutEmoji          = new MutableLiveData<>();
    this.events              = new SingleLiveEvent<>();
    this.repository          = new ManageProfileRepository();
    this.badge               = new DefaultValueLiveData<>(Optional.empty());
    this.observer            = this::onRecipientChanged;
    this.avatarState         = LiveDataUtil.combineLatest(Recipient.self().live().getLiveData(), internalAvatarState, (self, state) -> new AvatarState(state, self));

    SignalExecutors.BOUNDED.execute(() -> {
      onRecipientChanged(Recipient.self().fresh());
      ApplicationDependencies.getJobManager().add(RetrieveProfileJob.forRecipient(Recipient.self().getId()));
    });

    Recipient.self().live().observeForever(observer);
  }

  public @NonNull LiveData<AvatarState> getAvatar() {
    return Transformations.distinctUntilChanged(avatarState);
  }

  public @NonNull LiveData<ProfileName> getProfileName() {
    return profileName;
  }

  public @NonNull LiveData<String> getUsername() {
    return username;
  }

  public @NonNull LiveData<String> getAbout() {
    return about;
  }

  public @NonNull LiveData<String> getAboutEmoji() {
    return aboutEmoji;
  }

  public @NonNull LiveData<Optional<Badge>> getBadge() {
    return badge;
  }

  public @NonNull LiveData<Event> getEvents() {
    return events;
  }

  public boolean shouldShowUsername() {
    return FeatureFlags.usernames();
  }

  public void onAvatarSelected(@NonNull Context context, @Nullable Media media) {
    previousAvatar = internalAvatarState.getValue() != null ? internalAvatarState.getValue().getAvatar() : null;

    if (media == null) {
      internalAvatarState.postValue(InternalAvatarState.loading(null));
      repository.clearAvatar(context, result -> {
        switch (result) {
          case SUCCESS:
            internalAvatarState.postValue(InternalAvatarState.loaded(null));
            previousAvatar = null;
            break;
          case FAILURE_NETWORK:
            internalAvatarState.postValue(InternalAvatarState.loaded(previousAvatar));
            events.postValue(Event.AVATAR_NETWORK_FAILURE);
            break;
        }
      });
    } else {
      SignalExecutors.BOUNDED.execute(() -> {
        try {
          InputStream stream = BlobProvider.getInstance().getStream(context, media.getUri());
          byte[]      data   = StreamUtil.readFully(stream);

          internalAvatarState.postValue(InternalAvatarState.loading(data));

          repository.setAvatar(context, data, media.getMimeType(), result -> {
            switch (result) {
              case SUCCESS:
                internalAvatarState.postValue(InternalAvatarState.loaded(data));
                previousAvatar = data;
                break;
              case FAILURE_NETWORK:
                internalAvatarState.postValue(InternalAvatarState.loaded(previousAvatar));
                events.postValue(Event.AVATAR_NETWORK_FAILURE);
                break;
            }
          });
        } catch (IOException e) {
          Log.w(TAG, "Failed to save avatar!", e);
          events.postValue(Event.AVATAR_DISK_FAILURE);
        }
      });
    }
  }

  public boolean canRemoveAvatar() {
    return internalAvatarState.getValue() != null;
  }

  private void onRecipientChanged(@NonNull Recipient recipient) {
    profileName.postValue(recipient.getProfileName());
    username.postValue(recipient.getUsername().orElse(null));
    about.postValue(recipient.getAbout());
    aboutEmoji.postValue(recipient.getAboutEmoji());
    badge.postValue(Optional.ofNullable(recipient.getFeaturedBadge()));
    renderAvatar(AvatarHelper.getSelfProfileAvatarStream(ApplicationDependencies.getApplication()));
  }

  private void renderAvatar(@Nullable StreamDetails details) {
    if (details != null) {
      try {
        internalAvatarState.postValue(InternalAvatarState.loaded(StreamUtil.readFully(details.getStream())));
      } catch (IOException e) {
        Log.w(TAG, "Failed to read avatar!");
        internalAvatarState.postValue(InternalAvatarState.none());
      }
    } else {
      internalAvatarState.postValue(InternalAvatarState.none());
    }
  }

  @Override
  protected void onCleared() {
    Recipient.self().live().removeForeverObserver(observer);
  }

  public final static class AvatarState {
    private final InternalAvatarState internalAvatarState;
    private final Recipient           self;

    public AvatarState(@NonNull InternalAvatarState internalAvatarState,
                       @NonNull Recipient self)
    {
      this.internalAvatarState = internalAvatarState;
      this.self                = self;
    }

    public @Nullable byte[] getAvatar() {
      return internalAvatarState.avatar;
    }

    public @NonNull LoadingState getLoadingState() {
      return internalAvatarState.loadingState;
    }

    public @NonNull Recipient getSelf() {
      return self;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final AvatarState that = (AvatarState) o;
      return Objects.equals(internalAvatarState, that.internalAvatarState) && Objects.equals(self, that.self);
    }

    @Override
    public int hashCode() {
      return Objects.hash(internalAvatarState, self);
    }
  }

  private final static class InternalAvatarState {
    private final byte[]       avatar;
    private final LoadingState loadingState;

    public InternalAvatarState(@Nullable byte[] avatar, @NonNull LoadingState loadingState) {
      this.avatar       = avatar;
      this.loadingState = loadingState;
    }

    private static @NonNull InternalAvatarState none() {
      return new InternalAvatarState(null, LoadingState.LOADED);
    }

    private static @NonNull InternalAvatarState loaded(@Nullable byte[] avatar) {
      return new InternalAvatarState(avatar, LoadingState.LOADED);
    }

    private static @NonNull InternalAvatarState loading(@Nullable byte[] avatar) {
      return new InternalAvatarState(avatar, LoadingState.LOADING);
    }

    public @Nullable byte[] getAvatar() {
      return avatar;
    }

    public LoadingState getLoadingState() {
      return loadingState;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final InternalAvatarState that = (InternalAvatarState) o;
      return Arrays.equals(avatar, that.avatar) && loadingState == that.loadingState;
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(loadingState);
      result = 31 * result + Arrays.hashCode(avatar);
      return result;
    }
  }

  public enum LoadingState {
    LOADING, LOADED
  }

  enum Event {
    AVATAR_NETWORK_FAILURE, AVATAR_DISK_FAILURE
  }

  static class Factory extends ViewModelProvider.NewInstanceFactory {
    @Override
    public @NonNull<T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return Objects.requireNonNull(modelClass.cast(new ManageProfileViewModel()));
    }
  }

}
