package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import org.signal.libsignal.protocol.InvalidMessageException;
import asia.coolapp.chat.database.GroupDatabase;
import asia.coolapp.chat.database.GroupDatabase.GroupRecord;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint;
import asia.coolapp.chat.profiles.AvatarHelper;
import org.signal.core.util.Hex;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentRemoteId;
import org.whispersystems.signalservice.api.push.exceptions.MissingConfigurationException;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class AvatarGroupsV1DownloadJob extends BaseJob {

  public static final String KEY = "AvatarDownloadJob";

  private static final String TAG = Log.tag(AvatarGroupsV1DownloadJob.class);

  private static final String KEY_GROUP_ID = "group_id";

  @NonNull private final GroupId.V1 groupId;

  public AvatarGroupsV1DownloadJob(@NonNull GroupId.V1 groupId) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(10)
                           .build(),
         groupId);
  }

  private AvatarGroupsV1DownloadJob(@NonNull Job.Parameters parameters, @NonNull GroupId.V1 groupId) {
    super(parameters);
    this.groupId = groupId;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putString(KEY_GROUP_ID, groupId.toString()).build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException {
    GroupDatabase         database   = SignalDatabase.groups();
    Optional<GroupRecord> record     = database.getGroup(groupId);
    File                  attachment = null;

    try {
      if (record.isPresent()) {
        long             avatarId    = record.get().getAvatarId();
        String           contentType = record.get().getAvatarContentType();
        byte[]           key         = record.get().getAvatarKey();
        String           relay       = record.get().getRelay();
        Optional<byte[]> digest      = Optional.ofNullable(record.get().getAvatarDigest());
        Optional<String> fileName    = Optional.empty();

        if (avatarId == -1 || key == null) {
          return;
        }

        if (digest.isPresent()) {
          Log.i(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
        }

        attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
        attachment.deleteOnExit();

        SignalServiceMessageReceiver   receiver    = ApplicationDependencies.getSignalServiceMessageReceiver();
        SignalServiceAttachmentPointer pointer     = new SignalServiceAttachmentPointer(0, new SignalServiceAttachmentRemoteId(avatarId), contentType, key, Optional.of(0), Optional.empty(), 0, 0, digest, fileName, false, false, false, Optional.empty(), Optional.empty(), System.currentTimeMillis());
        InputStream                    inputStream = receiver.retrieveAttachment(pointer, attachment, AvatarHelper.AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE);

        AvatarHelper.setAvatar(context, record.get().getRecipientId(), inputStream);
        SignalDatabase.groups().onAvatarUpdated(groupId, true);

        inputStream.close();
      }
    } catch (NonSuccessfulResponseCodeException | InvalidMessageException | MissingConfigurationException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onFailure() {}

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  public static final class Factory implements Job.Factory<AvatarGroupsV1DownloadJob> {
    @Override
    public @NonNull AvatarGroupsV1DownloadJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new AvatarGroupsV1DownloadJob(parameters, GroupId.parseOrThrow(data.getString(KEY_GROUP_ID)).requireV1());
    }
  }
}
