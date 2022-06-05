package asia.coolapp.chat.components.settings.models

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

/**
 * Renders a single image, horizontally centered.
 */
object SplashImage {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.splash_image))
  }

  class Model(@DrawableRes val splashImageResId: Int) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return newItem.splashImageResId == splashImageResId
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val splashImageView: ImageView = itemView as ImageView

    override fun bind(model: Model) {
      splashImageView.setImageResource(model.splashImageResId)
    }
  }
}
