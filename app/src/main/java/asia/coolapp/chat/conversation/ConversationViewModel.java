package asia.coolapp.chat.conversation;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.annimon.stream.Stream;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.signal.core.util.MapUtil;
import org.signal.core.util.logging.Log;
import org.signal.paging.ObservablePagedData;
import org.signal.paging.PagedData;
import org.signal.paging.PagingConfig;
import org.signal.paging.PagingController;
import org.signal.paging.ProxyPagingController;
import org.signal.libsignal.protocol.util.Pair;
import asia.coolapp.chat.components.settings.app.notifications.profiles.NotificationProfilesRepository;
import asia.coolapp.chat.conversation.colors.ChatColors;
import asia.coolapp.chat.conversation.colors.ChatColorsPalette;
import asia.coolapp.chat.conversation.colors.NameColor;
import asia.coolapp.chat.database.DatabaseObserver;
import asia.coolapp.chat.database.GroupDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.database.model.MessageId;
import asia.coolapp.chat.database.model.StoryViewState;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.mediasend.MediaRepository;
import asia.coolapp.chat.notifications.profiles.NotificationProfile;
import asia.coolapp.chat.notifications.profiles.NotificationProfiles;
import asia.coolapp.chat.ratelimit.RecaptchaRequiredEvent;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.SignalLocalMetrics;
import asia.coolapp.chat.util.SingleLiveEvent;
import asia.coolapp.chat.util.Util;
import asia.coolapp.chat.util.ViewUtil;
import asia.coolapp.chat.util.livedata.LiveDataUtil;
import asia.coolapp.chat.util.livedata.Store;
import asia.coolapp.chat.wallpaper.ChatWallpaper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class ConversationViewModel extends ViewModel {

  private static final String TAG = Log.tag(ConversationViewModel.class);

  private final Application                           context;
  private final MediaRepository                       mediaRepository;
  private final ConversationRepository                conversationRepository;
  private final MutableLiveData<List<Media>>          recentMedia;
  private final BehaviorSubject<Long>                 threadId;
  private final Observable<MessageData>               messageData;
  private final MutableLiveData<Boolean>              showScrollButtons;
  private final MutableLiveData<Boolean>              hasUnreadMentions;
  private final Observable<Boolean>                   canShowAsBubble;
  private final ProxyPagingController<MessageId>      pagingController;
  private final DatabaseObserver.Observer             conversationObserver;
  private final DatabaseObserver.MessageObserver      messageUpdateObserver;
  private final DatabaseObserver.MessageObserver      messageInsertObserver;
  private final BehaviorSubject<RecipientId>          recipientId;
  private final Observable<Optional<ChatWallpaper>>   wallpaper;
  private final SingleLiveEvent<Event>                events;
  private final Observable<ChatColors>                chatColors;
  private final MutableLiveData<Integer>              toolbarBottom;
  private final MutableLiveData<Integer>              inlinePlayerHeight;
  private final LiveData<Integer>                     conversationTopMargin;
  private final Store<ThreadAnimationState>           threadAnimationStateStore;
  private final Observer<ThreadAnimationState>        threadAnimationStateStoreDriver;
  private final NotificationProfilesRepository        notificationProfilesRepository;
  private final MutableLiveData<String>               searchQuery;

  private final Map<GroupId, Set<Recipient>> sessionMemberCache = new HashMap<>();

  private ConversationIntents.Args args;
  private int                      jumpToPosition;

  private ConversationViewModel() {
    this.context                        = ApplicationDependencies.getApplication();
    this.mediaRepository                = new MediaRepository();
    this.conversationRepository         = new ConversationRepository();
    this.recentMedia                    = new MutableLiveData<>();
    this.showScrollButtons              = new MutableLiveData<>(false);
    this.hasUnreadMentions              = new MutableLiveData<>(false);
    this.events                         = new SingleLiveEvent<>();
    this.pagingController               = new ProxyPagingController<>();
    this.conversationObserver           = pagingController::onDataInvalidated;
    this.messageUpdateObserver          = pagingController::onDataItemChanged;
    this.messageInsertObserver          = messageId -> pagingController.onDataItemInserted(messageId, 0);
    this.toolbarBottom                  = new MutableLiveData<>();
    this.inlinePlayerHeight             = new MutableLiveData<>();
    this.conversationTopMargin          = Transformations.distinctUntilChanged(LiveDataUtil.combineLatest(toolbarBottom, inlinePlayerHeight, Integer::sum));
    this.threadAnimationStateStore      = new Store<>(new ThreadAnimationState(-1L, null, false));
    this.notificationProfilesRepository = new NotificationProfilesRepository();
    this.searchQuery                    = new MutableLiveData<>();
    this.recipientId                    = BehaviorSubject.create();
    this.threadId                       = BehaviorSubject.create();

    BehaviorSubject<Recipient> recipientCache = BehaviorSubject.create();

    recipientId
        .observeOn(Schedulers.io())
        .distinctUntilChanged()
        .map(Recipient::resolved)
        .subscribe(recipientCache);

    BehaviorSubject<ConversationData> conversationMetadata = BehaviorSubject.create();

    Observable.combineLatest(threadId, recipientCache, Pair::new)
        .observeOn(Schedulers.io())
        .distinctUntilChanged()
        .map(threadIdAndRecipient -> {
          SignalLocalMetrics.ConversationOpen.onMetadataLoadStarted();
          ConversationData conversationData = conversationRepository.getConversationData(threadIdAndRecipient.first(), threadIdAndRecipient.second(), jumpToPosition);
          SignalLocalMetrics.ConversationOpen.onMetadataLoaded();

          jumpToPosition = -1;

          return conversationData;
        })
        .subscribe(conversationMetadata);

    ApplicationDependencies.getDatabaseObserver().registerMessageUpdateObserver(messageUpdateObserver);

    messageData = conversationMetadata
        .observeOn(Schedulers.io())
        .switchMap(data -> {
          int startPosition;

          ConversationData.MessageRequestData messageRequestData = data.getMessageRequestData();

          if (data.shouldJumpToMessage()) {
            startPosition = data.getJumpToPosition();
          } else if (messageRequestData.isMessageRequestAccepted() && data.shouldScrollToLastSeen()) {
            startPosition = data.getLastSeenPosition();
          } else if (messageRequestData.isMessageRequestAccepted()) {
            startPosition = data.getLastScrolledPosition();
          } else {
            startPosition = data.getThreadSize();
          }

          ApplicationDependencies.getDatabaseObserver().unregisterObserver(conversationObserver);
          ApplicationDependencies.getDatabaseObserver().unregisterObserver(messageInsertObserver);
          ApplicationDependencies.getDatabaseObserver().registerConversationObserver(data.getThreadId(), conversationObserver);
          ApplicationDependencies.getDatabaseObserver().registerMessageInsertObserver(data.getThreadId(), messageInsertObserver);

          ConversationDataSource dataSource = new ConversationDataSource(context, data.getThreadId(), messageRequestData, data.showUniversalExpireTimerMessage(), data.getThreadSize());
          PagingConfig config = new PagingConfig.Builder().setPageSize(25)
                                                          .setBufferPages(2)
                                                          .setStartIndex(Math.max(startPosition, 0))
                                                          .build();

          Log.d(TAG, "Starting at position: " + startPosition + " || jumpToPosition: " + data.getJumpToPosition() + ", lastSeenPosition: " + data.getLastSeenPosition() + ", lastScrolledPosition: " + data.getLastScrolledPosition());
          ObservablePagedData<MessageId, ConversationMessage> pagedData = PagedData.createForObservable(dataSource, config);

          pagingController.set(pagedData.getController());
          return pagedData.getData();
        })
        .observeOn(Schedulers.io())
        .withLatestFrom(conversationMetadata, (messages, metadata) ->  new MessageData(metadata, messages))
        .doOnNext(a -> SignalLocalMetrics.ConversationOpen.onDataLoaded());

    Observable<Recipient> liveRecipient = recipientId.distinctUntilChanged().switchMap(id -> Recipient.live(id).asObservable());

    canShowAsBubble = threadId.observeOn(Schedulers.io()).map(conversationRepository::canShowAsBubble);
    wallpaper       = liveRecipient.map(r -> Optional.ofNullable(r.getWallpaper())).distinctUntilChanged();
    chatColors      = liveRecipient.map(Recipient::getChatColors).distinctUntilChanged();

    threadAnimationStateStore.update(threadId, (id, state) -> {
      if (state.getThreadId() == id) {
        return state;
      } else {
        return new ThreadAnimationState(id, null, false);
      }
    });

    threadAnimationStateStore.update(conversationMetadata, (m, state) -> {
      if (state.getThreadId() == m.getThreadId()) {
        return state.copy(state.getThreadId(), m, state.getHasCommittedNonEmptyMessageList());
      } else {
        return state.copy(m.getThreadId(), m, false);
      }
    });

    this.threadAnimationStateStoreDriver = state -> {};
    threadAnimationStateStore.getStateLiveData().observeForever(threadAnimationStateStoreDriver);

    EventBus.getDefault().register(this);
  }

  Observable<StoryViewState> getStoryViewState() {
    return recipientId
        .subscribeOn(Schedulers.io())
        .switchMap(StoryViewState::getForRecipientId)
        .distinctUntilChanged()
        .observeOn(AndroidSchedulers.mainThread());
  }

  void onMessagesCommitted(@NonNull List<ConversationMessage> conversationMessages) {
    if (Util.hasItems(conversationMessages)) {
      threadAnimationStateStore.update(state -> {
        long threadId = conversationMessages.stream()
                                            .filter(Objects::nonNull)
                                            .findFirst()
                                            .map(c -> c.getMessageRecord().getThreadId())
                                            .orElse(-2L);

        if (state.getThreadId() == threadId) {
          return state.copy(state.getThreadId(), state.getThreadMetadata(), true);
        } else {
          return state;
        }
      });
    }
  }

  boolean shouldPlayMessageAnimations() {
    return threadAnimationStateStore.getState().shouldPlayMessageAnimations();
  }

  void setToolbarBottom(int bottom) {
    toolbarBottom.setValue(bottom);
  }

  void setInlinePlayerVisible(boolean isVisible) {
    inlinePlayerHeight.setValue(isVisible ? ViewUtil.dpToPx(36) : 0);
  }

  void onAttachmentKeyboardOpen() {
    mediaRepository.getMediaInBucket(context, Media.ALL_MEDIA_BUCKET_ID, recentMedia::postValue);
  }

  @MainThread
  void onConversationDataAvailable(@NonNull RecipientId recipientId, long threadId, int startingPosition) {
    Log.d(TAG, "[onConversationDataAvailable] recipientId: " + recipientId + ", threadId: " + threadId + ", startingPosition: " + startingPosition);
    this.jumpToPosition = startingPosition;

    this.threadId.onNext(threadId);
    this.recipientId.onNext(recipientId);
  }

  void clearThreadId() {
    this.jumpToPosition = -1;
    this.threadId.onNext(-1L);
  }

  void setSearchQuery(@Nullable String query) {
    searchQuery.setValue(query);
  }

  @NonNull LiveData<String> getSearchQuery() {
    return searchQuery;
  }

  @NonNull LiveData<Integer> getConversationTopMargin() {
    return conversationTopMargin;
  }

  @NonNull Observable<Boolean> canShowAsBubble() {
    return canShowAsBubble
        .observeOn(AndroidSchedulers.mainThread());
  }

  @NonNull LiveData<Boolean> getShowScrollToBottom() {
    return Transformations.distinctUntilChanged(showScrollButtons);
  }

  @NonNull LiveData<Boolean> getShowMentionsButton() {
    return Transformations.distinctUntilChanged(LiveDataUtil.combineLatest(showScrollButtons, hasUnreadMentions, (a, b) -> a && b));
  }

  @NonNull Observable<Optional<ChatWallpaper>> getWallpaper() {
    return wallpaper
        .observeOn(AndroidSchedulers.mainThread());
  }

  @NonNull LiveData<Event> getEvents() {
    return events;
  }

  @NonNull Observable<ChatColors> getChatColors() {
    return chatColors
        .observeOn(AndroidSchedulers.mainThread());
  }

  void setHasUnreadMentions(boolean hasUnreadMentions) {
    this.hasUnreadMentions.setValue(hasUnreadMentions);
  }

  boolean getShowScrollButtons() {
    return this.showScrollButtons.getValue();
  }

  void setShowScrollButtons(boolean showScrollButtons) {
    this.showScrollButtons.setValue(showScrollButtons);
  }

  @NonNull LiveData<List<Media>> getRecentMedia() {
    return recentMedia;
  }

  @NonNull Observable<MessageData> getMessageData() {
    return messageData
        .observeOn(AndroidSchedulers.mainThread());
  }

  @NonNull PagingController<MessageId> getPagingController() {
    return pagingController;
  }

  @NonNull Observable<Map<RecipientId, NameColor>> getNameColorsMap() {
    return recipientId
        .observeOn(Schedulers.io())
        .distinctUntilChanged()
        .map(Recipient::resolved)
        .map(Recipient::getGroupId)
        .map(groupId -> {
          if (groupId.isPresent()) {
            List<Recipient> fullMembers   = SignalDatabase.groups().getGroupMembers(groupId.get(), GroupDatabase.MemberSet.FULL_MEMBERS_INCLUDING_SELF);
            Set<Recipient>  cachedMembers = MapUtil.getOrDefault(sessionMemberCache, groupId.get(), new HashSet<>());

            cachedMembers.addAll(fullMembers);
            sessionMemberCache.put(groupId.get(), cachedMembers);

            return cachedMembers;
          } else {
            return Collections.<Recipient>emptySet();
          }
        })
        .map(members -> {
          List<Recipient> sorted = Stream.of(members)
                                         .filter(member -> !Objects.equals(member, Recipient.self()))
                                         .sortBy(Recipient::requireStringId)
                                         .toList();

          List<NameColor>             names  = ChatColorsPalette.Names.getAll();
          Map<RecipientId, NameColor> colors = new HashMap<>();

          for (int i = 0; i < sorted.size(); i++) {
            colors.put(sorted.get(i).getId(), names.get(i % names.size()));
          }

          return colors;
        })
        .observeOn(AndroidSchedulers.mainThread());
  }

  @NonNull LiveData<Optional<NotificationProfile>> getActiveNotificationProfile() {
    final Observable<Optional<NotificationProfile>> activeProfile = Observable.combineLatest(Observable.interval(0, 30, TimeUnit.SECONDS), notificationProfilesRepository.getProfiles(), (interval, profiles) -> profiles)
                                                                              .map(profiles -> Optional.ofNullable(NotificationProfiles.getActiveProfile(profiles)));

    return LiveDataReactiveStreams.fromPublisher(activeProfile.toFlowable(BackpressureStrategy.LATEST));
  }

  void setArgs(@NonNull ConversationIntents.Args args) {
    this.args = args;
  }

  @NonNull ConversationIntents.Args getArgs() {
    return Objects.requireNonNull(args);
  }

  @Subscribe(threadMode = ThreadMode.POSTING)
  public void onRecaptchaRequiredEvent(@NonNull RecaptchaRequiredEvent event) {
    events.postValue(Event.SHOW_RECAPTCHA);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    threadAnimationStateStore.getStateLiveData().removeObserver(threadAnimationStateStoreDriver);
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(conversationObserver);
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(messageUpdateObserver);
    ApplicationDependencies.getDatabaseObserver().unregisterObserver(messageInsertObserver);
    EventBus.getDefault().unregister(this);
  }

  enum Event {
    SHOW_RECAPTCHA
  }

  static class MessageData {
    private final List<ConversationMessage> messages;
    private final ConversationData          metadata;

    MessageData(@NonNull ConversationData metadata, @NonNull List<ConversationMessage> messages) {
      this.metadata = metadata;
      this.messages = messages;
    }

    public @NonNull List<ConversationMessage> getMessages() {
      return messages;
    }

    public @NonNull ConversationData getMetadata() {
      return metadata;
    }
  }

  static class Factory extends ViewModelProvider.NewInstanceFactory {
    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection ConstantConditions
      return modelClass.cast(new ConversationViewModel());
    }
  }
}
