package asia.coolapp.chat.mediasend.v2.capture

import asia.coolapp.chat.mediasend.Media

sealed class MediaCaptureEvent {
  data class MediaCaptureRendered(val media: Media) : MediaCaptureEvent()
  object MediaCaptureRenderFailed : MediaCaptureEvent()
}
