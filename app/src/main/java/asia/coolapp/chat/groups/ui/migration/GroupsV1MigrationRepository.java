package asia.coolapp.chat.groups.ui.migration;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupChangeBusyException;
import asia.coolapp.chat.groups.GroupsV1MigrationUtil;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.jobs.RetrieveProfileJob;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.recipients.RecipientUtil;
import asia.coolapp.chat.transport.RetryLaterException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class GroupsV1MigrationRepository {

  private static final String TAG = Log.tag(GroupsV1MigrationRepository.class);

  void getMigrationState(@NonNull RecipientId groupRecipientId, @NonNull Consumer<MigrationState> callback) {
    SignalExecutors.BOUNDED.execute(() -> callback.accept(getMigrationState(groupRecipientId)));
  }

  void upgradeGroup(@NonNull RecipientId recipientId, @NonNull Consumer<MigrationResult> callback) {
    SignalExecutors.UNBOUNDED.execute(() -> {
      if (!NetworkConstraint.isMet(ApplicationDependencies.getApplication())) {
        Log.w(TAG, "No network!");
        callback.accept(MigrationResult.FAILURE_NETWORK);
        return;
      }

      if (!Recipient.resolved(recipientId).isPushV1Group()) {
        Log.w(TAG, "Not a V1 group!");
        callback.accept(MigrationResult.FAILURE_GENERAL);
        return;
      }

      try {
        GroupsV1MigrationUtil.migrate(ApplicationDependencies.getApplication(), recipientId, true);
        callback.accept(MigrationResult.SUCCESS);
      } catch (IOException | RetryLaterException | GroupChangeBusyException e) {
        callback.accept(MigrationResult.FAILURE_NETWORK);
      } catch (GroupsV1MigrationUtil.InvalidMigrationStateException e) {
        callback.accept(MigrationResult.FAILURE_GENERAL);
      }
    });
  }

  @WorkerThread
  private MigrationState getMigrationState(@NonNull RecipientId groupRecipientId) {
    Recipient group = Recipient.resolved(groupRecipientId);

    if (!group.isPushV1Group()) {
      return new MigrationState(Collections.emptyList(), Collections.emptyList());
    }

    Set<RecipientId> needsRefresh = Stream.of(group.getParticipants())
                                          .filter(r -> r.getGroupsV1MigrationCapability() != Recipient.Capability.SUPPORTED)
                                          .map(Recipient::getId)
                                          .collect(Collectors.toSet());

    List<Job> jobs = RetrieveProfileJob.forRecipients(needsRefresh);

    for (Job job : jobs) {
      if (!ApplicationDependencies.getJobManager().runSynchronously(job, TimeUnit.SECONDS.toMillis(3)).isPresent()) {
        Log.w(TAG, "Failed to refresh capabilities in time!");
      }
    }

    try {
      List<Recipient> registered = Stream.of(group.getParticipants())
                                         .filter(Recipient::isRegistered)
                                         .toList();

      RecipientUtil.ensureUuidsAreAvailable(ApplicationDependencies.getApplication(), registered);
    } catch (IOException e) {
      Log.w(TAG, "Failed to refresh UUIDs!", e);
    }

    group = group.fresh();

    List<Recipient> ineligible = Stream.of(group.getParticipants())
                                       .filter(r -> !r.hasServiceId() ||
                                                    r.getGroupsV1MigrationCapability() != Recipient.Capability.SUPPORTED ||
                                                    r.getRegistered() != RecipientDatabase.RegisteredState.REGISTERED)
                                       .toList();

    List<Recipient> invites = Stream.of(group.getParticipants())
                                    .filterNot(ineligible::contains)
                                    .filterNot(Recipient::isSelf)
                                    .filter(r -> r.getProfileKey() == null)
                                    .toList();

    return new MigrationState(invites, ineligible);
  }
}
