package asia.coolapp.chat.groups.ui.creategroup.details;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import asia.coolapp.chat.groups.ui.GroupMemberEntry;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.mediasend.Media;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.DefaultValueLiveData;
import asia.coolapp.chat.util.SingleLiveEvent;
import asia.coolapp.chat.util.livedata.LiveDataUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AddGroupDetailsViewModel extends ViewModel {

  private final LiveData<List<GroupMemberEntry.NewGroupCandidate>> members;
  private final DefaultValueLiveData<Set<RecipientId>>             deleted                   = new DefaultValueLiveData<>(new HashSet<>());
  private final MutableLiveData<String>                            name                      = new MutableLiveData<>("");
  private final MutableLiveData<byte[]>                            avatar                    = new MutableLiveData<>();
  private final SingleLiveEvent<GroupCreateResult>                 groupCreateResult         = new SingleLiveEvent<>();
  private final MutableLiveData<Integer>                           disappearingMessagesTimer = new MutableLiveData<>(SignalStore.settings().getUniversalExpireTimer());
  private final LiveData<Boolean>                                  isMms;
  private final LiveData<Boolean>                                  canSubmitForm;
  private final AddGroupDetailsRepository                          repository;

  private Media avatarMedia;

  private AddGroupDetailsViewModel(@NonNull Collection<RecipientId> recipientIds,
                                   @NonNull AddGroupDetailsRepository repository)
  {
    this.repository = repository;

    MutableLiveData<List<GroupMemberEntry.NewGroupCandidate>> initialMembers = new MutableLiveData<>();
    LiveData<Boolean>                                         isValidName    = Transformations.map(name, name -> !TextUtils.isEmpty(name));

    members = LiveDataUtil.combineLatest(initialMembers, deleted, AddGroupDetailsViewModel::filterDeletedMembers);
    isMms   = Transformations.map(members, AddGroupDetailsViewModel::isAnyForcedSms);

    LiveData<List<GroupMemberEntry.NewGroupCandidate>> membersToCheckGv2CapabilityOf = LiveDataUtil.combineLatest(isMms, members, (forcedMms, memberList) -> {
      if (SignalStore.internalValues().gv2DoNotCreateGv2Groups() || forcedMms) {
        return Collections.emptyList();
      } else {
        return memberList;
      }
    });

    canSubmitForm = LiveDataUtil.combineLatest(isMms, isValidName, (mms, validName) -> mms || validName);

    repository.resolveMembers(recipientIds, initialMembers::postValue);
  }

  @NonNull LiveData<List<GroupMemberEntry.NewGroupCandidate>> getMembers() {
    return members;
  }

  @NonNull LiveData<Boolean> getCanSubmitForm() {
    return canSubmitForm;
  }

  @NonNull LiveData<GroupCreateResult> getGroupCreateResult() {
    return groupCreateResult;
  }

  @NonNull LiveData<byte[]> getAvatar() {
    return avatar;
  }

  @NonNull LiveData<Boolean> getIsMms() {
    return isMms;
  }

  @NonNull LiveData<Integer> getDisappearingMessagesTimer() {
    return disappearingMessagesTimer;
  }

  void setAvatar(@Nullable byte[] avatar) {
    this.avatar.setValue(avatar);
  }

  boolean hasAvatar() {
    return avatar.getValue() != null;
  }

  void setName(@NonNull String name) {
    this.name.setValue(name);
  }

  void delete(@NonNull RecipientId recipientId) {
    Set<RecipientId> deleted = this.deleted.getValue();

    deleted.add(recipientId);
    this.deleted.setValue(deleted);
  }

  void create() {
    List<GroupMemberEntry.NewGroupCandidate> members           = Objects.requireNonNull(this.members.getValue());
    Set<RecipientId>                         memberIds         = Stream.of(members).map(member -> member.getMember().getId()).collect(Collectors.toSet());
    byte[]                                   avatarBytes       = avatar.getValue();
    boolean                                  isGroupMms        = isMms.getValue() == Boolean.TRUE;
    String                                   groupName         = name.getValue();
    Integer                                  disappearingTimer = disappearingMessagesTimer.getValue();

    if (!isGroupMms && TextUtils.isEmpty(groupName)) {
      groupCreateResult.postValue(GroupCreateResult.error(GroupCreateResult.Error.Type.ERROR_INVALID_NAME));
      return;
    }

    repository.createGroup(memberIds,
                           avatarBytes,
                           groupName,
                           isGroupMms,
                           disappearingTimer,
                           groupCreateResult::postValue);
  }

  private static @NonNull List<GroupMemberEntry.NewGroupCandidate> filterDeletedMembers(@NonNull List<GroupMemberEntry.NewGroupCandidate> members, @NonNull Set<RecipientId> deleted) {
    return Stream.of(members)
                 .filterNot(member -> deleted.contains(member.getMember().getId()))
                 .toList();
  }

  private static boolean isAnyForcedSms(@NonNull List<GroupMemberEntry.NewGroupCandidate> members) {
    return Stream.of(members)
                 .anyMatch(member -> !member.getMember().isRegistered());
  }

  public void setDisappearingMessageTimer(int timer) {
    disappearingMessagesTimer.setValue(timer);
  }

  public void setAvatarMedia(@Nullable Media media) {
    this.avatarMedia = media;
  }

  public @Nullable Media getAvatarMedia() {
    return avatarMedia;
  }

  static final class Factory implements ViewModelProvider.Factory {

    private final Collection<RecipientId>   recipientIds;
    private final AddGroupDetailsRepository repository;

    Factory(@NonNull Collection<RecipientId> recipientIds, @NonNull AddGroupDetailsRepository repository) {
      this.recipientIds = recipientIds;
      this.repository   = repository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return Objects.requireNonNull(modelClass.cast(new AddGroupDetailsViewModel(recipientIds, repository)));
    }
  }
}
