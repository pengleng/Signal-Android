package asia.coolapp.chat.stories.viewer.views

import android.view.View
import android.widget.TextView
import asia.coolapp.chat.R
import asia.coolapp.chat.components.AvatarImageView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.util.DateUtils
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder
import java.util.Locale

/**
 * UI consisting of a recipient's avatar, name, and when they viewed a story
 */
object StoryViewItem {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.stories_story_view_item))
  }

  class Model(
    val storyViewItemData: StoryViewItemData
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return storyViewItemData.recipient == newItem.storyViewItemData.recipient
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return storyViewItemData == newItem.storyViewItemData &&
        storyViewItemData.recipient.hasSameContent(newItem.storyViewItemData.recipient) &&
        super.areContentsTheSame(newItem)
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val avatarView: AvatarImageView = itemView.findViewById(R.id.avatar)
    private val nameView: TextView = itemView.findViewById(R.id.name)
    private val viewedAtView: TextView = itemView.findViewById(R.id.viewed_at)

    override fun bind(model: Model) {
      avatarView.setAvatar(model.storyViewItemData.recipient)
      nameView.text = model.storyViewItemData.recipient.getDisplayName(context)
      viewedAtView.text = formatDate(model.storyViewItemData.timeViewedInMillis)
    }

    private fun formatDate(dateInMilliseconds: Long): String {
      return DateUtils.formatDateWithDayOfWeek(Locale.getDefault(), dateInMilliseconds)
    }
  }
}
