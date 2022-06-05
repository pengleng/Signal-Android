package asia.coolapp.chat.stories

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.scale
import androidx.core.view.drawToBitmap
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import asia.coolapp.chat.conversation.colors.ChatColors
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.database.model.MessageRecord
import asia.coolapp.chat.database.model.MmsMessageRecord
import asia.coolapp.chat.database.model.databaseprotos.StoryTextPost
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.util.Base64
import java.security.MessageDigest

/**
 * Glide model to render a StoryTextPost as a bitmap
 */
data class StoryTextPostModel(
  private val storyTextPost: StoryTextPost,
  private val storySentAtMillis: Long,
  private val storyAuthor: RecipientId
) : Key {

  override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    messageDigest.update(storyTextPost.toByteArray())
    messageDigest.update(storySentAtMillis.toString().toByteArray())
    messageDigest.update(storyAuthor.serialize().toByteArray())
  }

  val text: String = storyTextPost.body

  fun getPlaceholder(): Drawable {
    return if (storyTextPost.hasBackground()) {
      ChatColors.forChatColor(ChatColors.Id.NotSet, storyTextPost.background).chatBubbleMask
    } else {
      ColorDrawable(Color.TRANSPARENT)
    }
  }

  companion object {

    fun parseFrom(messageRecord: MessageRecord): StoryTextPostModel {
      return parseFrom(
        messageRecord.body,
        messageRecord.timestamp,
        if (messageRecord.isOutgoing) Recipient.self().id else messageRecord.individualRecipient.id
      )
    }

    @JvmStatic
    fun parseFrom(body: String, storySentAtMillis: Long, storyAuthor: RecipientId): StoryTextPostModel {
      return StoryTextPostModel(
        storyTextPost = StoryTextPost.parseFrom(Base64.decode(body)),
        storySentAtMillis = storySentAtMillis,
        storyAuthor = storyAuthor
      )
    }
  }

  class Decoder : ResourceDecoder<StoryTextPostModel, Bitmap> {

    companion object {
      private const val RENDER_WIDTH = 1080
      private const val RENDER_HEIGHT = 1920
    }

    override fun handles(source: StoryTextPostModel, options: Options): Boolean = true

    override fun decode(source: StoryTextPostModel, width: Int, height: Int, options: Options): Resource<Bitmap> {
      val message = SignalDatabase.mmsSms.getMessageFor(source.storySentAtMillis, source.storyAuthor)
      val view = StoryTextPostView(ApplicationDependencies.getApplication())

      view.bindFromStoryTextPost(source.storyTextPost)
      view.bindLinkPreview((message as? MmsMessageRecord)?.linkPreviews?.firstOrNull())

      view.invalidate()
      view.measure(View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(RENDER_HEIGHT, View.MeasureSpec.EXACTLY))
      view.layout(0, 0, view.measuredWidth, view.measuredHeight)

      val bitmap = view.drawToBitmap().scale(width, height)

      return SimpleResource(bitmap)
    }
  }
}
