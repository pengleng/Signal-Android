package asia.coolapp.chat.components.settings.app.internal

import android.content.Context
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.MessageDatabase
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.addStyle
import asia.coolapp.chat.database.model.databaseprotos.BodyRangeList
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.emoji.EmojiFiles
import asia.coolapp.chat.jobs.AttachmentDownloadJob
import asia.coolapp.chat.jobs.CreateReleaseChannelJob
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.releasechannel.ReleaseChannel

class InternalSettingsRepository(context: Context) {

  private val context = context.applicationContext

  fun getEmojiVersionInfo(consumer: (EmojiFiles.Version?) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      consumer(EmojiFiles.Version.readVersion(context))
    }
  }

  fun addSampleReleaseNote() {
    SignalExecutors.UNBOUNDED.execute {
      ApplicationDependencies.getJobManager().runSynchronously(CreateReleaseChannelJob.create(), 5000)

      val title = "Release Note Title"
      val bodyText = "Release note body. Aren't I awesome?"
      val body = "$title\n\n$bodyText"
      val bodyRangeList = BodyRangeList.newBuilder()
        .addStyle(BodyRangeList.BodyRange.Style.BOLD, 0, title.length)

      val recipientId = SignalStore.releaseChannelValues().releaseChannelRecipientId!!
      val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(recipientId))

      val insertResult: MessageDatabase.InsertResult? = ReleaseChannel.insertAnnouncement(
        recipientId = recipientId,
        body = body,
        threadId = threadId,
        messageRanges = bodyRangeList.build(),
        image = "https://via.placeholder.com/720x480",
        imageWidth = 720,
        imageHeight = 480
      )

      SignalDatabase.sms.insertBoostRequestMessage(recipientId, threadId)

      if (insertResult != null) {
        SignalDatabase.attachments.getAttachmentsForMessage(insertResult.messageId)
          .forEach { ApplicationDependencies.getJobManager().add(AttachmentDownloadJob(insertResult.messageId, it.attachmentId, false)) }

        ApplicationDependencies.getMessageNotifier().updateNotification(context, insertResult.threadId)
      }
    }
  }
}
