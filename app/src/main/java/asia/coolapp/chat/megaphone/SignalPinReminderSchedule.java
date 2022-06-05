package asia.coolapp.chat.megaphone;

import asia.coolapp.chat.keyvalue.SignalStore;

final class SignalPinReminderSchedule implements MegaphoneSchedule {

  @Override
  public boolean shouldDisplay(int seenCount, long lastSeen, long firstVisible, long currentTime) {
    if (SignalStore.kbsValues().hasOptedOut()) {
      return false;
    }

    if (!SignalStore.kbsValues().hasPin()) {
      return false;
    }

    if (!SignalStore.pinValues().arePinRemindersEnabled()) {
      return false;
    }

    if (!SignalStore.account().isRegistered()) {
      return false;
    }

    long lastSuccessTime = SignalStore.pinValues().getLastSuccessfulEntryTime();
    long interval        = SignalStore.pinValues().getCurrentInterval();

    return currentTime - lastSuccessTime >= interval;
  }
}
