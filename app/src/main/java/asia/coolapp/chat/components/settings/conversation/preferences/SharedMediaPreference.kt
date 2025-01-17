package asia.coolapp.chat.components.settings.conversation.preferences

import android.database.Cursor
import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.components.ThreadPhotoRailView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.database.MediaDatabase
import asia.coolapp.chat.mms.GlideApp
import asia.coolapp.chat.util.ViewUtil
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

/**
 * Renders the shared media photo rail.
 */
object SharedMediaPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_shared_media))
  }

  class Model(
    val mediaCursor: Cursor,
    val mediaIds: List<Long>,
    val onMediaRecordClick: (MediaDatabase.MediaRecord, Boolean) -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return true
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) &&
        mediaIds == newItem.mediaIds
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val rail: ThreadPhotoRailView = itemView.findViewById(R.id.rail_view)

    override fun bind(model: Model) {
      rail.setCursor(GlideApp.with(rail), model.mediaCursor)
      rail.setListener {
        model.onMediaRecordClick(it, ViewUtil.isLtr(rail))
      }
    }
  }
}
