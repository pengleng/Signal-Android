package asia.coolapp.chat.stories.viewer

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.stories.viewer.page.StoryViewerPageFragment

class StoryViewerPagerAdapter(fragment: Fragment, private val initialStoryId: Long) : FragmentStateAdapter(fragment) {

  private var pages: List<RecipientId> = emptyList()

  fun setPages(newPages: List<RecipientId>) {
    val oldPages = pages
    pages = newPages

    val callback = Callback(oldPages, pages)
    DiffUtil.calculateDiff(callback).dispatchUpdatesTo(this)
  }

  override fun getItemCount(): Int = pages.size

  override fun createFragment(position: Int): Fragment {
    return StoryViewerPageFragment.create(pages[position], initialStoryId)
  }

  private class Callback(
    private val oldList: List<RecipientId>,
    private val newList: List<RecipientId>
  ) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      return oldList[oldItemPosition] == newList[newItemPosition]
    }
  }
}
