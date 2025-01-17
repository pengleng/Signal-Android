package asia.coolapp.chat.mediasend.v2.text

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import asia.coolapp.chat.conversation.colors.ChatColors
import asia.coolapp.chat.fonts.TextFont
import asia.coolapp.chat.scribbles.HSVColorSlider
import asia.coolapp.chat.util.FeatureFlags

@Parcelize
data class TextStoryPostCreationState(
  val body: CharSequence = "",
  val textColor: Int = HSVColorSlider.getLastColor(),
  val textColorStyle: TextColorStyle = TextColorStyle.NO_BACKGROUND,
  val textAlignment: TextAlignment = if (FeatureFlags.storiesTextFunctions()) TextAlignment.START else TextAlignment.CENTER,
  val textFont: TextFont = TextFont.REGULAR,
  @IntRange(from = 0, to = 100) val textScale: Int = 50,
  val backgroundColor: ChatColors = TextStoryBackgroundColors.getInitialBackgroundColor(),
  val linkPreviewUri: String? = null,
) : Parcelable {

  @ColorInt
  @IgnoredOnParcel
  val textForegroundColor: Int = when (textColorStyle) {
    TextColorStyle.NO_BACKGROUND -> textColor
    TextColorStyle.NORMAL -> textColor
    TextColorStyle.INVERT -> Color.WHITE
  }

  @ColorInt
  @IgnoredOnParcel
  val textBackgroundColor: Int = when (textColorStyle) {
    TextColorStyle.NO_BACKGROUND -> Color.TRANSPARENT
    TextColorStyle.NORMAL -> Color.WHITE
    TextColorStyle.INVERT -> textColor
  }
}
