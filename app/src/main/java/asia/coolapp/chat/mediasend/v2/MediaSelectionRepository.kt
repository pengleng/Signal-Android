package asia.coolapp.chat.mediasend.v2

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.ThreadUtil
import org.signal.core.util.logging.Log
import org.signal.imageeditor.core.model.EditorModel
import asia.coolapp.chat.TransportOption
import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.contacts.paged.RecipientSearchKey
import asia.coolapp.chat.database.AttachmentDatabase.TransformProperties
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.ThreadDatabase
import asia.coolapp.chat.database.model.Mention
import asia.coolapp.chat.database.model.StoryType
import asia.coolapp.chat.mediasend.CompositeMediaTransform
import asia.coolapp.chat.mediasend.ImageEditorModelRenderMediaTransform
import asia.coolapp.chat.mediasend.Media
import asia.coolapp.chat.mediasend.MediaRepository
import asia.coolapp.chat.mediasend.MediaSendActivityResult
import asia.coolapp.chat.mediasend.MediaTransform
import asia.coolapp.chat.mediasend.MediaUploadRepository
import asia.coolapp.chat.mediasend.SentMediaQualityTransform
import asia.coolapp.chat.mediasend.VideoEditorFragment
import asia.coolapp.chat.mediasend.VideoTrimTransform
import asia.coolapp.chat.mms.MediaConstraints
import asia.coolapp.chat.mms.OutgoingMediaMessage
import asia.coolapp.chat.mms.OutgoingSecureMediaMessage
import asia.coolapp.chat.mms.SentMediaQuality
import asia.coolapp.chat.mms.Slide
import asia.coolapp.chat.providers.BlobProvider
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.scribbles.ImageEditorFragment
import asia.coolapp.chat.sms.MessageSender
import asia.coolapp.chat.sms.MessageSender.PreUploadResult
import asia.coolapp.chat.sms.OutgoingStoryMessage
import asia.coolapp.chat.util.MessageUtil
import java.util.Collections
import java.util.concurrent.TimeUnit

private val TAG = Log.tag(MediaSelectionRepository::class.java)

class MediaSelectionRepository(context: Context) {

  private val context: Context = context.applicationContext

  private val mediaRepository = MediaRepository()

  val uploadRepository = MediaUploadRepository(this.context)
  val isMetered: Observable<Boolean> = MeteredConnectivity.isMetered(this.context)

  fun populateAndFilterMedia(media: List<Media>, mediaConstraints: MediaConstraints, maxSelection: Int): Single<MediaValidator.FilterResult> {
    return Single.fromCallable {
      val populatedMedia = mediaRepository.getPopulatedMedia(context, media)

      MediaValidator.filterMedia(context, populatedMedia, mediaConstraints, maxSelection)
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Tries to send the selected media, performing proper transformations for edited images and videos.
   */
  fun send(
    selectedMedia: List<Media>,
    stateMap: Map<Uri, Any>,
    quality: SentMediaQuality,
    message: CharSequence?,
    isSms: Boolean,
    isViewOnce: Boolean,
    singleContact: RecipientSearchKey?,
    contacts: List<RecipientSearchKey>,
    mentions: List<Mention>,
    transport: TransportOption
  ): Maybe<MediaSendActivityResult> {
    if (isSms && contacts.isNotEmpty()) {
      throw IllegalStateException("Provided recipients to send to, but this is SMS!")
    }

    if (selectedMedia.isEmpty()) {
      throw IllegalStateException("No selected media!")
    }

    return Maybe.create<MediaSendActivityResult> { emitter ->
      val trimmedBody: String = if (isViewOnce) "" else message?.toString()?.trim() ?: ""
      val trimmedMentions: List<Mention> = if (isViewOnce) emptyList() else mentions
      val modelsToTransform: Map<Media, MediaTransform> = buildModelsToTransform(selectedMedia, stateMap, quality)
      val oldToNewMediaMap: Map<Media, Media> = MediaRepository.transformMediaSync(context, selectedMedia, modelsToTransform)
      val updatedMedia = oldToNewMediaMap.values.toList()

      for (media in updatedMedia) {
        Log.w(TAG, media.uri.toString() + " : " + media.transformProperties.map { t: TransformProperties -> "" + t.isVideoTrim }.orElse("null"))
        media.setCaption(trimmedBody)
      }

      val singleRecipient: Recipient? = singleContact?.let { Recipient.resolved(it.recipientId) }
      val storyType: StoryType = if (singleRecipient?.isDistributionList == true) {
        SignalDatabase.distributionLists.getStoryType(singleRecipient.requireDistributionListId())
      } else {
        StoryType.NONE
      }

      if (isSms || MessageSender.isLocalSelfSend(context, singleRecipient, isSms)) {
        Log.i(TAG, "SMS or local self-send. Skipping pre-upload.")
        emitter.onSuccess(MediaSendActivityResult.forTraditionalSend(singleRecipient!!.id, updatedMedia, trimmedBody, transport, isViewOnce, trimmedMentions, StoryType.NONE))
      } else {
        val splitMessage = MessageUtil.getSplitMessage(context, trimmedBody, transport.calculateCharacters(trimmedBody).maxPrimaryMessageSize)
        val splitBody = splitMessage.body

        if (splitMessage.textSlide.isPresent) {
          val slide: Slide = splitMessage.textSlide.get()
          uploadRepository.startUpload(
            MediaBuilder.buildMedia(
              uri = requireNotNull(slide.uri),
              mimeType = slide.contentType,
              date = System.currentTimeMillis(),
              size = slide.fileSize,
              borderless = slide.isBorderless,
              videoGif = slide.isVideoGif
            ),
            singleRecipient
          )
        }

        uploadRepository.applyMediaUpdates(oldToNewMediaMap, singleRecipient)
        uploadRepository.updateCaptions(updatedMedia)
        uploadRepository.updateDisplayOrder(updatedMedia)
        uploadRepository.getPreUploadResults { uploadResults ->
          if (contacts.isNotEmpty()) {
            sendMessages(contacts, splitBody, uploadResults, trimmedMentions, isViewOnce)
            uploadRepository.deleteAbandonedAttachments()
            emitter.onComplete()
          } else if (uploadResults.isNotEmpty()) {
            emitter.onSuccess(MediaSendActivityResult.forPreUpload(singleRecipient!!.id, uploadResults, splitBody, transport, isViewOnce, trimmedMentions, storyType))
          } else {
            Log.w(TAG, "Got empty upload results! isSms: $isSms, updatedMedia.size(): ${updatedMedia.size}, isViewOnce: $isViewOnce, target: $singleContact")
            emitter.onSuccess(MediaSendActivityResult.forTraditionalSend(singleRecipient!!.id, updatedMedia, trimmedBody, transport, isViewOnce, trimmedMentions, storyType))
          }
        }
      }
    }.subscribeOn(Schedulers.io()).cast(MediaSendActivityResult::class.java)
  }

  fun deleteBlobs(media: List<Media>) {
    media
      .map(Media::getUri)
      .filter(BlobProvider::isAuthority)
      .forEach { BlobProvider.getInstance().delete(context, it) }
  }

  fun cleanUp(selectedMedia: List<Media>) {
    deleteBlobs(selectedMedia)
    uploadRepository.cancelAllUploads()
    uploadRepository.deleteAbandonedAttachments()
  }

  fun isLocalSelfSend(recipient: Recipient?, isSms: Boolean): Boolean {
    return MessageSender.isLocalSelfSend(context, recipient, isSms)
  }

  @WorkerThread
  private fun buildModelsToTransform(
    selectedMedia: List<Media>,
    stateMap: Map<Uri, Any>,
    quality: SentMediaQuality
  ): Map<Media, MediaTransform> {
    val modelsToRender: MutableMap<Media, MediaTransform> = mutableMapOf()

    selectedMedia.forEach {
      val state = stateMap[it.uri]
      if (state is ImageEditorFragment.Data) {
        val model: EditorModel? = state.readModel()
        if (model != null && model.isChanged) {
          modelsToRender[it] = ImageEditorModelRenderMediaTransform(model)
        }
      }

      if (state is VideoEditorFragment.Data && state.isDurationEdited) {
        modelsToRender[it] = VideoTrimTransform(state)
      }

      if (quality == SentMediaQuality.HIGH) {
        val existingTransform: MediaTransform? = modelsToRender[it]

        modelsToRender[it] = if (existingTransform == null) {
          SentMediaQualityTransform(quality)
        } else {
          CompositeMediaTransform(existingTransform, SentMediaQualityTransform(quality))
        }
      }
    }

    return modelsToRender
  }

  @WorkerThread
  private fun sendMessages(contacts: List<RecipientSearchKey>, body: String, preUploadResults: Collection<PreUploadResult>, mentions: List<Mention>, isViewOnce: Boolean) {
    val broadcastMessages: MutableList<OutgoingSecureMediaMessage> = ArrayList(contacts.size)
    val storyMessages: MutableMap<PreUploadResult, MutableList<OutgoingSecureMediaMessage>> = mutableMapOf()
    val distributionListSentTimestamps: MutableMap<PreUploadResult, Long> = mutableMapOf()

    for (contact in contacts) {
      val recipient = Recipient.resolved(contact.recipientId)
      val isStory = contact is ContactSearchKey.Story || recipient.isDistributionList

      if (isStory && recipient.isActiveGroup) {
        SignalDatabase.groups.markDisplayAsStory(recipient.requireGroupId())
      }

      val storyType: StoryType = when {
        recipient.isDistributionList -> SignalDatabase.distributionLists.getStoryType(recipient.requireDistributionListId())
        isStory -> StoryType.STORY_WITH_REPLIES
        else -> StoryType.NONE
      }

      val message = OutgoingMediaMessage(
        recipient,
        body,
        emptyList(),
        if (recipient.isDistributionList) distributionListSentTimestamps.getOrPut(preUploadResults.first()) { System.currentTimeMillis() } else System.currentTimeMillis(),
        -1,
        TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong()),
        isViewOnce,
        ThreadDatabase.DistributionTypes.DEFAULT,
        storyType,
        null,
        false,
        null,
        emptyList(),
        emptyList(),
        mentions,
        mutableSetOf(),
        mutableSetOf()
      )

      if (isStory && preUploadResults.size > 1) {
        preUploadResults.forEach {
          val list = storyMessages[it] ?: mutableListOf()
          list.add(OutgoingSecureMediaMessage(message).withSentTimestamp(if (recipient.isDistributionList) distributionListSentTimestamps.getOrPut(it) { System.currentTimeMillis() } else System.currentTimeMillis()))
          storyMessages[it] = list

          // XXX We must do this to avoid sending out messages to the same recipient with the same
          //     sentTimestamp. If we do this, they'll be considered dupes by the receiver.
          ThreadUtil.sleep(5)
        }
      } else {
        broadcastMessages.add(OutgoingSecureMediaMessage(message))

        // XXX We must do this to avoid sending out messages to the same recipient with the same
        //     sentTimestamp. If we do this, they'll be considered dupes by the receiver.
        ThreadUtil.sleep(5)
      }
    }

    if (broadcastMessages.isNotEmpty()) {
      MessageSender.sendMediaBroadcast(
        context,
        broadcastMessages,
        preUploadResults,
        storyMessages.flatMap { (preUploadResult, messages) ->
          messages.map { OutgoingStoryMessage(it, preUploadResult) }
        }
      )
    } else {
      storyMessages.forEach { (preUploadResult, messages) ->
        MessageSender.sendMediaBroadcast(context, messages, Collections.singleton(preUploadResult), Collections.emptyList())
      }
    }
  }
}
