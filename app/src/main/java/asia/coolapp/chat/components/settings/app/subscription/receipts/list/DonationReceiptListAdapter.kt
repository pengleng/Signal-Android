package asia.coolapp.chat.components.settings.app.subscription.receipts.list

import android.view.LayoutInflater
import android.view.ViewGroup
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.SectionHeaderPreference
import asia.coolapp.chat.components.settings.SectionHeaderPreferenceViewHolder
import asia.coolapp.chat.components.settings.TextPreference
import asia.coolapp.chat.components.settings.TextPreferenceViewHolder
import asia.coolapp.chat.util.StickyHeaderDecoration
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.toLocalDateTime

class DonationReceiptListAdapter(onModelClick: (DonationReceiptListItem.Model) -> Unit) : MappingAdapter(), StickyHeaderDecoration.StickyHeaderAdapter<SectionHeaderPreferenceViewHolder> {

  init {
    registerFactory(TextPreference::class.java, LayoutFactory({ TextPreferenceViewHolder(it) }, R.layout.dsl_preference_item))
    DonationReceiptListItem.register(this, onModelClick)
  }

  override fun getHeaderId(position: Int): Long {
    return when (val item = getItem(position)) {
      is DonationReceiptListItem.Model -> item.record.timestamp.toLocalDateTime().year.toLong()
      else -> StickyHeaderDecoration.StickyHeaderAdapter.NO_HEADER_ID
    }
  }

  override fun onCreateHeaderViewHolder(parent: ViewGroup?, position: Int, type: Int): SectionHeaderPreferenceViewHolder {
    return SectionHeaderPreferenceViewHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.dsl_section_header, parent, false))
  }

  override fun onBindHeaderViewHolder(viewHolder: SectionHeaderPreferenceViewHolder?, position: Int, type: Int) {
    viewHolder?.bind(SectionHeaderPreference(DSLSettingsText.from(getHeaderId(position).toString())))
  }
}
