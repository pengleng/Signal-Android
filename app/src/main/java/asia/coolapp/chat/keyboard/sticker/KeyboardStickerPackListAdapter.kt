package asia.coolapp.chat.keyboard.sticker

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import asia.coolapp.chat.R
import asia.coolapp.chat.glide.cache.ApngOptions
import asia.coolapp.chat.mms.DecryptableStreamUriLoader.DecryptableUri
import asia.coolapp.chat.mms.GlideRequests
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingModel
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

class KeyboardStickerPackListAdapter(private val glideRequests: GlideRequests, private val allowApngAnimation: Boolean, private val onTabSelected: (StickerPack) -> Unit) : MappingAdapter() {

  init {
    registerFactory(StickerPack::class.java, LayoutFactory(::StickerPackViewHolder, R.layout.keyboard_pager_category_icon))
  }

  data class StickerPack(val packRecord: StickerKeyboardRepository.KeyboardStickerPack, val selected: Boolean = false) : MappingModel<StickerPack> {
    val loadImage: Boolean = packRecord.coverResource == null
    val uri: DecryptableUri? = packRecord.coverUri?.let { DecryptableUri(packRecord.coverUri) }
    val iconResource: Int = packRecord.coverResource ?: 0

    override fun areItemsTheSame(newItem: StickerPack): Boolean {
      return packRecord.packId == newItem.packRecord.packId
    }

    override fun areContentsTheSame(newItem: StickerPack): Boolean {
      return areItemsTheSame(newItem) && selected == newItem.selected
    }
  }

  private inner class StickerPackViewHolder(itemView: View) : MappingViewHolder<StickerPack>(itemView) {

    private val selected: View = findViewById(R.id.category_icon_selected)
    private val icon: ImageView = findViewById(R.id.category_icon)
    private val defaultTint: ColorStateList? = ImageViewCompat.getImageTintList(icon)

    override fun bind(model: StickerPack) {
      itemView.setOnClickListener { onTabSelected(model) }

      selected.isSelected = model.selected

      if (model.loadImage) {
        ImageViewCompat.setImageTintList(icon, null)
        icon.alpha = if (model.selected) 1f else 0.5f
        glideRequests.load(model.uri)
          .set(ApngOptions.ANIMATE, allowApngAnimation)
          .into(icon)
      } else {
        ImageViewCompat.setImageTintList(icon, defaultTint)
        icon.setImageResource(model.iconResource)
        icon.alpha = 1f
        icon.isSelected = model.selected
      }
    }
  }
}
