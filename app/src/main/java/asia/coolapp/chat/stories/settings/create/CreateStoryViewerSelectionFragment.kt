package asia.coolapp.chat.stories.settings.create

import androidx.navigation.fragment.findNavController
import asia.coolapp.chat.R
import asia.coolapp.chat.database.model.DistributionListId
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.stories.settings.select.BaseStoryRecipientSelectionFragment
import asia.coolapp.chat.util.navigation.safeNavigate

/**
 * Allows user to select who will see the story they are creating
 */
class CreateStoryViewerSelectionFragment : BaseStoryRecipientSelectionFragment() {
  override val actionButtonLabel: Int = R.string.CreateStoryViewerSelectionFragment__next
  override val distributionListId: DistributionListId? = null

  override fun goToNextScreen(recipients: Set<RecipientId>) {
    findNavController().safeNavigate(CreateStoryViewerSelectionFragmentDirections.actionCreateStoryViewerSelectionToCreateStoryWithViewers(recipients.toTypedArray()))
  }
}
