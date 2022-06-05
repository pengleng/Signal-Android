package asia.coolapp.chat.util;

import androidx.annotation.StyleRes;

import asia.coolapp.chat.R;

public class DynamicDarkToolbarTheme extends DynamicTheme {

  protected @StyleRes int getTheme() {
    return R.style.Signal_DayNight_DarkNoActionBar;
  }
}
