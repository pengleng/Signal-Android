package asia.coolapp.chat.conversation.colors.ui.custom

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import asia.coolapp.chat.R

class CustomChatColorCreatorFragment : Fragment(R.layout.custom_chat_color_creator_fragment) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)
    val pager: ViewPager2 = view.findViewById(R.id.pager)
    val adapter = CustomChatColorPagerAdapter(this, requireArguments())
    val tabLayoutMediator = TabLayoutMediator(tabLayout, pager) { tab, position ->
      tab.setText(
        if (position == 0) {
          R.string.CustomChatColorCreatorFragment__solid
        } else {
          R.string.CustomChatColorCreatorFragment__gradient
        }
      )
    }

    toolbar.setNavigationOnClickListener {
      Navigation.findNavController(it).popBackStack()
    }

    pager.isUserInputEnabled = false
    pager.adapter = adapter

    if (Build.VERSION.SDK_INT < 21) {
      tabLayout.visibility = View.GONE
    } else {
      tabLayoutMediator.attach()
    }

    val startPage = CustomChatColorCreatorFragmentArgs.fromBundle(requireArguments()).startPage
    pager.setCurrentItem(startPage, false)
  }
}
