package asia.coolapp.chat.groups.ui.migration;

import androidx.annotation.NonNull;

import asia.coolapp.chat.recipients.Recipient;

import java.util.List;

/**
 * Represents the migration state of a group. Namely, which users will be invited or left behind.
 */
final class MigrationState {
  private final List<Recipient> needsInvite;
  private final List<Recipient> ineligible;

  MigrationState(@NonNull List<Recipient> needsInvite,
                 @NonNull List<Recipient> ineligible)
  {
    this.needsInvite = needsInvite;
    this.ineligible  = ineligible;
  }

  public @NonNull List<Recipient> getNeedsInvite() {
    return needsInvite;
  }

  public @NonNull List<Recipient> getIneligible() {
    return ineligible;
  }
}
