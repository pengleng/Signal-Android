package asia.coolapp.chat.groups.v2;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.zkgroup.profiles.ProfileKey;
import org.signal.libsignal.zkgroup.profiles.ProfileKeyCredential;
import asia.coolapp.chat.crypto.ProfileKeyUtil;
import asia.coolapp.chat.database.RecipientDatabase;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.groupsv2.GroupCandidate;
import org.whispersystems.signalservice.api.push.ServiceId;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class GroupCandidateHelper {
  private final SignalServiceAccountManager signalServiceAccountManager;
  private final RecipientDatabase           recipientDatabase;

  public GroupCandidateHelper(@NonNull Context context) {
    signalServiceAccountManager = ApplicationDependencies.getSignalServiceAccountManager();
    recipientDatabase           = SignalDatabase.recipients();
  }

  private static final String TAG = Log.tag(GroupCandidateHelper.class);

  /**
   * Given a recipient will create a {@link GroupCandidate} which may or may not have a profile key credential.
   * <p>
   * It will try to find missing profile key credentials from the server and persist locally.
   */
  @WorkerThread
  public @NonNull GroupCandidate recipientIdToCandidate(@NonNull RecipientId recipientId)
      throws IOException
  {
    final Recipient recipient = Recipient.resolved(recipientId);

    ServiceId serviceId = recipient.getServiceId().orElse(null);
    if (serviceId == null) {
      throw new AssertionError("Non UUID members should have need detected by now");
    }

    Optional<ProfileKeyCredential> profileKeyCredential = Optional.ofNullable(recipient.getProfileKeyCredential());
    GroupCandidate                 candidate            = new GroupCandidate(serviceId.uuid(), profileKeyCredential);

    if (!candidate.hasProfileKeyCredential()) {
      ProfileKey profileKey = ProfileKeyUtil.profileKeyOrNull(recipient.getProfileKey());

      if (profileKey != null) {
        Log.i(TAG, String.format("No profile key credential on recipient %s, fetching", recipient.getId()));

        Optional<ProfileKeyCredential> profileKeyCredentialOptional = signalServiceAccountManager.resolveProfileKeyCredential(serviceId, profileKey, Locale.getDefault());

        if (profileKeyCredentialOptional.isPresent()) {
          boolean updatedProfileKey = recipientDatabase.setProfileKeyCredential(recipient.getId(), profileKey, profileKeyCredentialOptional.get());

          if (!updatedProfileKey) {
            Log.w(TAG, String.format("Failed to update the profile key credential on recipient %s", recipient.getId()));
          } else {
            Log.i(TAG, String.format("Got new profile key credential for recipient %s", recipient.getId()));
            candidate = candidate.withProfileKeyCredential(profileKeyCredentialOptional.get());
          }
        }
      }
    }

    return candidate;
  }

  @WorkerThread
  public @NonNull Set<GroupCandidate> recipientIdsToCandidates(@NonNull Collection<RecipientId> recipientIds)
      throws IOException
  {
    Set<GroupCandidate> result = new HashSet<>(recipientIds.size());

    for (RecipientId recipientId : recipientIds) {
      result.add(recipientIdToCandidate(recipientId));
    }

    return result;
  }
}
