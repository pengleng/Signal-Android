package asia.coolapp.chat.stories.dialogs

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import asia.coolapp.chat.R
import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient

object StoryDialogs {

  /**
   * Guards onAddToStory with a dialog
   */
  fun guardWithAddToYourStoryDialog(
    context: Context,
    contacts: Collection<ContactSearchKey>,
    onAddToStory: () -> Unit,
    onEditViewers: () -> Unit,
    onCancel: () -> Unit = {}
  ) {
    if (!isFirstSendToMyStory(contacts)) {
      onAddToStory()
    } else {
      SignalStore.storyValues().userHasBeenNotifiedAboutStories = true
      MaterialAlertDialogBuilder(context, R.style.Signal_ThemeOverlay_Dialog_Rounded)
        .setTitle(R.string.StoryDialogs__add_to_story_q)
        .setMessage(R.string.StoryDialogs__adding_content)
        .setPositiveButton(R.string.StoryDialogs__add_to_story) { _, _ ->
          onAddToStory.invoke()
        }
        .setNeutralButton(R.string.StoryDialogs__edit_viewers) { _, _ -> onEditViewers.invoke() }
        .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel.invoke() }
        .setCancelable(false)
        .show()
    }
  }

  private fun isFirstSendToMyStory(shareContacts: Collection<ContactSearchKey>): Boolean {
    if (SignalStore.storyValues().userHasBeenNotifiedAboutStories) {
      return false
    }

    return shareContacts.any { it is ContactSearchKey.Story && Recipient.resolved(it.recipientId).isMyStory }
  }
}
