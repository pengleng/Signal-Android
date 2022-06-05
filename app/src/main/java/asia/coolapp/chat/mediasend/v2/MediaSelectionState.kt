package asia.coolapp.chat.mediasend.v2

import android.net.Uri
import asia.coolapp.chat.TransportOption
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.mediasend.Media
import asia.coolapp.chat.mediasend.MediaSendConstants
import asia.coolapp.chat.mms.SentMediaQuality
import asia.coolapp.chat.recipients.Recipient

data class MediaSelectionState(
  val transportOption: TransportOption,
  val selectedMedia: List<Media> = listOf(),
  val focusedMedia: Media? = null,
  val recipient: Recipient? = null,
  val quality: SentMediaQuality = SignalStore.settings().sentMediaQuality,
  val message: CharSequence? = null,
  val viewOnceToggleState: ViewOnceToggleState = ViewOnceToggleState.INFINITE,
  val isTouchEnabled: Boolean = true,
  val isSent: Boolean = false,
  val isPreUploadEnabled: Boolean = false,
  val isMeteredConnection: Boolean = false,
  val editorStateMap: Map<Uri, Any> = mapOf(),
  val cameraFirstCapture: Media? = null
) {

  val maxSelection = if (transportOption.isSms) {
    MediaSendConstants.MAX_SMS
  } else {
    MediaSendConstants.MAX_PUSH
  }

  val canSend = !isSent && selectedMedia.isNotEmpty()

  enum class ViewOnceToggleState(val code: Int) {
    INFINITE(0),
    ONCE(1);

    fun next(): ViewOnceToggleState {
      return when (this) {
        INFINITE -> ONCE
        ONCE -> INFINITE
      }
    }

    companion object {
      fun fromCode(code: Int): ViewOnceToggleState {
        return when (code) {
          1 -> ONCE
          else -> INFINITE
        }
      }
    }
  }
}
