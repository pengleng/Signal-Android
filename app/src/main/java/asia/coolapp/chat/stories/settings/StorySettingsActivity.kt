package asia.coolapp.chat.stories.settings

import android.content.Context
import android.content.Intent
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLSettingsActivity

class StorySettingsActivity : DSLSettingsActivity() {
  companion object {
    fun getIntent(context: Context): Intent {
      return Intent(context, StorySettingsActivity::class.java)
        .putExtra(ARG_NAV_GRAPH, R.navigation.story_settings)
    }
  }
}
