package asia.coolapp.chat.stories.viewer.reply.tabs

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.stories.viewer.reply.group.StoryGroupReplyFragment
import asia.coolapp.chat.stories.viewer.views.StoryViewsFragment

class StoryViewsAndRepliesPagerAdapter(
  fragment: Fragment,
  private val storyId: Long,
  private val groupRecipientId: RecipientId
) : FragmentStateAdapter(fragment) {
  override fun getItemCount(): Int = 2

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    recyclerView.isNestedScrollingEnabled = false
  }

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      0 -> StoryViewsFragment.create(storyId)
      1 -> StoryGroupReplyFragment.create(storyId, groupRecipientId)
      else -> throw IndexOutOfBoundsException()
    }
  }
}
