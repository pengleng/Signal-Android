package asia.coolapp.chat.mediasend.v2.gallery

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import asia.coolapp.chat.R
import asia.coolapp.chat.mediasend.Media
import asia.coolapp.chat.mms.DecryptableStreamUriLoader
import asia.coolapp.chat.util.MediaUtil
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingModel
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder
import asia.coolapp.chat.util.visible

typealias OnSelectedMediaClicked = (Media) -> Unit

object MediaGallerySelectedItem {

  fun register(mappingAdapter: MappingAdapter, onSelectedMediaClicked: OnSelectedMediaClicked) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it, onSelectedMediaClicked) }, R.layout.v2_media_selection_item))
  }

  class Model(val media: Media) : MappingModel<Model> {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return media.uri == newItem.media.uri
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return media.uri == newItem.media.uri
    }
  }

  class ViewHolder(itemView: View, private val onSelectedMediaClicked: OnSelectedMediaClicked) : MappingViewHolder<Model>(itemView) {

    private val imageView: ImageView = itemView.findViewById(R.id.media_selection_image)
    private val videoOverlay: ImageView = itemView.findViewById(R.id.media_selection_play_overlay)

    override fun bind(model: Model) {
      Glide.with(imageView)
        .load(DecryptableStreamUriLoader.DecryptableUri(model.media.uri))
        .centerCrop()
        .into(imageView)

      videoOverlay.visible = MediaUtil.isVideo(model.media.mimeType) && !model.media.isVideoGif
      itemView.setOnClickListener { onSelectedMediaClicked(model.media) }
    }
  }
}
