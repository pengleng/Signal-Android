package asia.coolapp.chat.jobs

import androidx.core.content.ContextCompat
import org.signal.core.util.logging.Log
import asia.coolapp.chat.R
import asia.coolapp.chat.avatar.Avatar
import asia.coolapp.chat.avatar.AvatarRenderer
import asia.coolapp.chat.avatar.Avatars
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.jobmanager.Data
import asia.coolapp.chat.jobmanager.Job
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.profiles.AvatarHelper
import asia.coolapp.chat.profiles.ProfileName
import asia.coolapp.chat.providers.BlobProvider
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.transport.RetryLaterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Creates the Release Channel (Signal) recipient.
 */
class CreateReleaseChannelJob private constructor(parameters: Parameters) : BaseJob(parameters) {
  companion object {
    const val KEY = "CreateReleaseChannelJob"

    private val TAG = Log.tag(CreateReleaseChannelJob::class.java)

    fun create(): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(
        Parameters.Builder()
          .setQueue("CreateReleaseChannelJob")
          .setMaxInstancesForFactory(1)
          .setMaxAttempts(3)
          .build()
      )
    }
  }

  override fun serialize(): Data = Data.EMPTY

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    if (!SignalStore.account().isRegistered) {
      Log.i(TAG, "Not registered, skipping.")
      return
    }

    if (SignalStore.releaseChannelValues().releaseChannelRecipientId != null) {
      Log.i(TAG, "Already created Release Channel recipient ${SignalStore.releaseChannelValues().releaseChannelRecipientId}")

      val recipient = Recipient.resolved(SignalStore.releaseChannelValues().releaseChannelRecipientId!!)
      if (recipient.profileAvatar == null || recipient.profileAvatar?.isEmpty() == true) {
        setAvatar(recipient.id)
      }
    } else {
      val recipients = SignalDatabase.recipients

      val releaseChannelId: RecipientId = recipients.insertReleaseChannelRecipient()
      SignalStore.releaseChannelValues().setReleaseChannelRecipientId(releaseChannelId)

      recipients.setProfileName(releaseChannelId, ProfileName.asGiven("Signal"))
      recipients.setMuted(releaseChannelId, Long.MAX_VALUE)
      setAvatar(releaseChannelId)
    }
  }

  private fun setAvatar(id: RecipientId) {
    val latch = CountDownLatch(1)
    AvatarRenderer.renderAvatar(
      context,
      Avatar.Resource(
        R.drawable.ic_signal_logo_large,
        Avatars.ColorPair(ContextCompat.getColor(context, R.color.core_ultramarine), ContextCompat.getColor(context, R.color.core_white), "")
      ),
      onAvatarRendered = { media ->
        AvatarHelper.setAvatar(context, id, BlobProvider.getInstance().getStream(context, media.uri))
        SignalDatabase.recipients.setProfileAvatar(id, "local")
        latch.countDown()
      },
      onRenderFailed = { t ->
        Log.w(TAG, t)
        latch.countDown()
      }
    )

    try {
      val completed: Boolean = latch.await(30, TimeUnit.SECONDS)
      if (!completed) {
        throw RetryLaterException()
      }
    } catch (e: InterruptedException) {
      throw RetryLaterException()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean = e is RetryLaterException

  class Factory : Job.Factory<CreateReleaseChannelJob> {
    override fun create(parameters: Parameters, data: Data): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(parameters)
    }
  }
}
