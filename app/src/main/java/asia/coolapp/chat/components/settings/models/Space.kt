package asia.coolapp.chat.components.settings.models

import android.view.View
import androidx.annotation.Px
import androidx.core.view.updateLayoutParams
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

/**
 * Adds extra space between elements in a DSL fragment
 */
data class Space(
  @Px val pixels: Int
) {

  companion object {
    fun register(mappingAdapter: MappingAdapter) {
      mappingAdapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it) }, R.layout.dsl_space_preference))
    }
  }

  class Model(val space: Space) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return true
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) && newItem.space == space
    }
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {
    override fun bind(model: Model) {
      itemView.updateLayoutParams {
        height = model.space.pixels
      }
    }
  }
}
