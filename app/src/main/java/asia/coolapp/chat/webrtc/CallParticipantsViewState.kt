package asia.coolapp.chat.webrtc

import asia.coolapp.chat.components.webrtc.CallParticipantsState

data class CallParticipantsViewState(
  val callParticipantsState: CallParticipantsState,
  val isPortrait: Boolean,
  val isLandscapeEnabled: Boolean
)
