package asia.coolapp.chat.components

import android.os.Bundle
import androidx.fragment.app.Fragment
import asia.coolapp.chat.PassphraseRequiredActivity
import asia.coolapp.chat.R
import asia.coolapp.chat.util.DynamicNoActionBarTheme
import asia.coolapp.chat.util.DynamicTheme

/**
 * Activity that wraps a given fragment
 */
abstract class FragmentWrapperActivity : PassphraseRequiredActivity() {

  protected open val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)
    setContentView(R.layout.fragment_container)
    dynamicTheme.onCreate(this)

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, getFragment())
        .commit()
    }
  }

  abstract fun getFragment(): Fragment

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }
}
