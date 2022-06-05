package asia.coolapp.chat.mediasend.v2.text.send

import io.reactivex.rxjava3.core.Single
import org.signal.core.util.ThreadUtil
import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.contacts.paged.RecipientSearchKey
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.ThreadDatabase
import asia.coolapp.chat.database.model.StoryType
import asia.coolapp.chat.database.model.databaseprotos.StoryTextPost
import asia.coolapp.chat.fonts.TextFont
import asia.coolapp.chat.linkpreview.LinkPreview
import asia.coolapp.chat.mediasend.v2.UntrustedRecords
import asia.coolapp.chat.mediasend.v2.text.TextStoryPostCreationState
import asia.coolapp.chat.mms.OutgoingMediaMessage
import asia.coolapp.chat.mms.OutgoingSecureMediaMessage
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.stories.Stories
import asia.coolapp.chat.util.Base64

class TextStoryPostSendRepository {

  fun send(contactSearchKey: Set<ContactSearchKey>, textStoryPostCreationState: TextStoryPostCreationState, linkPreview: LinkPreview?): Single<TextStoryPostSendResult> {
    return UntrustedRecords
      .checkForBadIdentityRecords(contactSearchKey.filterIsInstance(RecipientSearchKey::class.java).toSet())
      .toSingleDefault<TextStoryPostSendResult>(TextStoryPostSendResult.Success)
      .onErrorReturn {
        if (it is UntrustedRecords.UntrustedRecordsException) {
          TextStoryPostSendResult.UntrustedRecordsError(it.untrustedRecords)
        } else {
          TextStoryPostSendResult.Failure
        }
      }
      .flatMap { result ->
        if (result is TextStoryPostSendResult.Success) {
          performSend(contactSearchKey, textStoryPostCreationState, linkPreview)
        } else {
          Single.just(result)
        }
      }
  }

  private fun performSend(contactSearchKey: Set<ContactSearchKey>, textStoryPostCreationState: TextStoryPostCreationState, linkPreview: LinkPreview?): Single<TextStoryPostSendResult> {
    return Single.fromCallable {
      val messages: MutableList<OutgoingSecureMediaMessage> = mutableListOf()
      val distributionListSentTimestamp = System.currentTimeMillis()

      for (contact in contactSearchKey) {
        val recipient = Recipient.resolved(contact.requireShareContact().recipientId.get())
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
          serializeTextStoryState(textStoryPostCreationState),
          emptyList(),
          if (recipient.isDistributionList) distributionListSentTimestamp else System.currentTimeMillis(),
          -1,
          0,
          false,
          ThreadDatabase.DistributionTypes.DEFAULT,
          storyType.toTextStoryType(),
          null,
          false,
          null,
          emptyList(),
          listOfNotNull(linkPreview),
          emptyList(),
          mutableSetOf(),
          mutableSetOf()
        )

        messages.add(OutgoingSecureMediaMessage(message))
        ThreadUtil.sleep(5)
      }

      Stories.sendTextStories(messages)
    }.flatMap { messages ->
      messages.toSingleDefault<TextStoryPostSendResult>(TextStoryPostSendResult.Success)
    }
  }

  private fun serializeTextStoryState(textStoryPostCreationState: TextStoryPostCreationState): String {
    val builder = StoryTextPost.newBuilder()

    builder.body = textStoryPostCreationState.body.toString()
    builder.background = textStoryPostCreationState.backgroundColor.serialize()
    builder.style = when (textStoryPostCreationState.textFont) {
      TextFont.REGULAR -> StoryTextPost.Style.REGULAR
      TextFont.BOLD -> StoryTextPost.Style.BOLD
      TextFont.SERIF -> StoryTextPost.Style.SERIF
      TextFont.SCRIPT -> StoryTextPost.Style.SCRIPT
      TextFont.CONDENSED -> StoryTextPost.Style.CONDENSED
    }
    builder.textBackgroundColor = textStoryPostCreationState.textBackgroundColor
    builder.textForegroundColor = textStoryPostCreationState.textForegroundColor

    return Base64.encodeBytes(builder.build().toByteArray())
  }
}
