package asia.coolapp.chat.components.settings.app.subscription.receipts.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import asia.coolapp.chat.R
import asia.coolapp.chat.components.BoldSelectionTabItem
import asia.coolapp.chat.components.ControllableTabLayout

class DonationReceiptListFragment : Fragment(R.layout.donation_receipt_list_fragment) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val pager: ViewPager2 = view.findViewById(R.id.pager)
    val tabs: ControllableTabLayout = view.findViewById(R.id.tabs)
    val toolbar: Toolbar = view.findViewById(R.id.toolbar)

    toolbar.setNavigationOnClickListener {
      findNavController().popBackStack()
    }

    pager.adapter = DonationReceiptListPageAdapter(this)

    BoldSelectionTabItem.registerListeners(tabs)

    TabLayoutMediator(tabs, pager) { tab, position ->
      tab.setText(
        when (position) {
          0 -> R.string.DonationReceiptListFragment__all
          1 -> R.string.DonationReceiptListFragment__recurring
          2 -> R.string.DonationReceiptListFragment__one_time
          else -> error("Unsupported index $position")
        }
      )
    }.attach()
  }
}
