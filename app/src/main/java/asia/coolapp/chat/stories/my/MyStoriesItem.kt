package asia.coolapp.chat.stories.my

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import asia.coolapp.chat.R
import asia.coolapp.chat.components.ThumbnailView
import asia.coolapp.chat.components.menu.ActionItem
import asia.coolapp.chat.components.menu.SignalContextMenu
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.conversation.ConversationMessage
import asia.coolapp.chat.database.model.MmsMessageRecord
import asia.coolapp.chat.mms.GlideApp
import asia.coolapp.chat.mms.Slide
import asia.coolapp.chat.stories.StoryTextPostModel
import asia.coolapp.chat.util.DateUtils
import asia.coolapp.chat.util.SpanUtil
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder
import asia.coolapp.chat.util.visible
import java.util.Locale

object MyStoriesItem {

  private const val STATUS_CHANGE = 0

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.stories_my_stories_item))
  }

  class Model(
    val distributionStory: ConversationMessage,
    val onClick: (Model, View) -> Unit,
    val onSaveClick: (Model) -> Unit,
    val onDeleteClick: (Model) -> Unit,
    val onForwardClick: (Model) -> Unit,
    val onShareClick: (Model) -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return distributionStory.messageRecord.id == newItem.distributionStory.messageRecord.id
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return distributionStory == newItem.distributionStory &&
        !hasStatusChange(newItem) &&
        super.areContentsTheSame(newItem)
    }

    override fun getChangePayload(newItem: Model): Any? {
      return if (isSameRecord(newItem) && hasStatusChange(newItem)) {
        STATUS_CHANGE
      } else {
        null
      }
    }

    private fun isSameRecord(newItem: Model): Boolean {
      return distributionStory.messageRecord.id == newItem.distributionStory.messageRecord.id
    }

    private fun hasStatusChange(newItem: Model): Boolean {
      val oldRecord = distributionStory.messageRecord
      val newRecord = newItem.distributionStory.messageRecord

      return oldRecord.isOutgoing &&
        newRecord.isOutgoing &&
        (oldRecord.isPending != newRecord.isPending || oldRecord.isSent != newRecord.isSent || oldRecord.isFailed != newRecord.isFailed)
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val downloadTarget: View = itemView.findViewById(R.id.download_touch)
    private val moreTarget: View = itemView.findViewById(R.id.more_touch)
    private val storyPreview: ThumbnailView = itemView.findViewById(R.id.story)
    private val viewCount: TextView = itemView.findViewById(R.id.view_count)
    private val date: TextView = itemView.findViewById(R.id.date)
    private val errorIndicator: View = itemView.findViewById(R.id.error_indicator)

    override fun bind(model: Model) {
      storyPreview.isClickable = false
      itemView.setOnClickListener { model.onClick(model, storyPreview) }
      downloadTarget.setOnClickListener { model.onSaveClick(model) }
      moreTarget.setOnClickListener { showContextMenu(model) }
      presentDateOrStatus(model)

      viewCount.text = context.resources.getQuantityString(
        R.plurals.MyStories__d_views,
        model.distributionStory.messageRecord.viewedReceiptCount,
        model.distributionStory.messageRecord.viewedReceiptCount
      )

      if (STATUS_CHANGE in payload) {
        return
      }

      val record: MmsMessageRecord = model.distributionStory.messageRecord as MmsMessageRecord
      val thumbnail: Slide? = record.slideDeck.thumbnailSlide

      @Suppress("CascadeIf")
      if (record.storyType.isTextStory) {
        storyPreview.setImageResource(GlideApp.with(storyPreview), StoryTextPostModel.parseFrom(record), 0, 0)
      } else if (thumbnail != null) {
        storyPreview.setImageResource(GlideApp.with(storyPreview), thumbnail, false, true)
      } else {
        storyPreview.clear(GlideApp.with(storyPreview))
      }
    }

    private fun presentDateOrStatus(model: Model) {
      if (model.distributionStory.messageRecord.isPending || model.distributionStory.messageRecord.isMediaPending) {
        errorIndicator.visible = false
        date.setText(R.string.StoriesLandingItem__sending)
      } else if (model.distributionStory.messageRecord.isFailed) {
        errorIndicator.visible = true
        date.text = SpanUtil.color(ContextCompat.getColor(context, R.color.signal_alert_primary), context.getString(R.string.StoriesLandingItem__couldnt_send))
      } else {
        errorIndicator.visible = false
        date.text = DateUtils.getBriefRelativeTimeSpanString(context, Locale.getDefault(), model.distributionStory.messageRecord.dateSent)
      }
    }

    private fun showContextMenu(model: Model) {
      SignalContextMenu.Builder(itemView, itemView.rootView as ViewGroup)
        .preferredHorizontalPosition(SignalContextMenu.HorizontalPosition.END)
        .show(
          listOf(
            ActionItem(R.drawable.ic_delete_24_tinted, context.getString(R.string.delete)) { model.onDeleteClick(model) },
            ActionItem(R.drawable.ic_download_24_tinted, context.getString(R.string.save)) { model.onSaveClick(model) },
            ActionItem(R.drawable.ic_forward_24_tinted, context.getString(R.string.MyStories_forward)) { model.onForwardClick(model) },
            ActionItem(R.drawable.ic_share_24_tinted, context.getString(R.string.StoriesLandingItem__share)) { model.onShareClick(model) }
          )
        )
    }
  }
}
