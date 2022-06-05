package asia.coolapp.chat.stories.my

import androidx.fragment.app.Fragment
import asia.coolapp.chat.components.FragmentWrapperActivity

class MyStoriesActivity : FragmentWrapperActivity() {
  override fun getFragment(): Fragment {
    return MyStoriesFragment()
  }
}
