package asia.coolapp.chat.blocked;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.SingleLiveEvent;

import java.util.List;
import java.util.Objects;

public class BlockedUsersViewModel extends ViewModel {

  private final BlockedUsersRepository           repository;
  private final MutableLiveData<List<Recipient>> recipients;
  private final SingleLiveEvent<Event>           events = new SingleLiveEvent<>();

  private BlockedUsersViewModel(@NonNull BlockedUsersRepository repository) {
    this.repository  = repository;
    this.recipients = new MutableLiveData<>();

    loadRecipients();
   }

  public LiveData<List<Recipient>> getRecipients() {
    return recipients;
  }

  public LiveData<Event> getEvents() {
    return events;
  }

  void block(@NonNull RecipientId recipientId) {
    repository.block(recipientId,
                     () -> {
                       loadRecipients();
                       events.postValue(new Event(EventType.BLOCK_SUCCEEDED, Recipient.resolved(recipientId)));
                     },
                     () -> events.postValue(new Event(EventType.BLOCK_FAILED, Recipient.resolved(recipientId))));
  }

  void createAndBlock(@NonNull String number) {
    repository.createAndBlock(number, () -> {
      loadRecipients();
      events.postValue(new Event(EventType.BLOCK_SUCCEEDED, number));
    });
  }

  void unblock(@NonNull RecipientId recipientId) {
    repository.unblock(recipientId, () -> {
      loadRecipients();
      events.postValue(new Event(EventType.UNBLOCK_SUCCEEDED, Recipient.resolved(recipientId)));
    });
  }

  private void loadRecipients() {
    repository.getBlocked(recipients::postValue);
  }

  enum EventType {
    BLOCK_SUCCEEDED,
    BLOCK_FAILED,
    UNBLOCK_SUCCEEDED
  }

  public static final class Event {

    private final EventType eventType;
    private final Recipient recipient;
    private final String    number;

    private Event(@NonNull EventType eventType, @NonNull Recipient recipient) {
      this.eventType = eventType;
      this.recipient = recipient;
      this.number    = null;
    }

    private Event(@NonNull EventType eventType, @NonNull String number) {
      this.eventType = eventType;
      this.recipient = null;
      this.number    = number;
    }

    public @Nullable Recipient getRecipient() {
      return recipient;
    }

    public @Nullable String getNumber() {
      return number;
    }

    public @NonNull EventType getEventType() {
      return eventType;
    }
  }

  public static class Factory implements ViewModelProvider.Factory {

    private final BlockedUsersRepository repository;

    public Factory(@NonNull BlockedUsersRepository repository) {
      this.repository = repository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return Objects.requireNonNull(modelClass.cast(new BlockedUsersViewModel(repository)));
    }
  }
}
