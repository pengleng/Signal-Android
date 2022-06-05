package asia.coolapp.chat.components.settings.app.subscription.receipts.list

import android.view.View
import android.widget.TextView
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.BadgeImageView
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.database.model.DonationReceiptRecord
import asia.coolapp.chat.payments.FiatMoneyUtil
import asia.coolapp.chat.util.DateUtils
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingModel
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder
import java.util.Locale

object DonationReceiptListItem {

  fun register(adapter: MappingAdapter, onClick: (Model) -> Unit) {
    adapter.registerFactory(Model::class.java, LayoutFactory({ ViewHolder(it, onClick) }, R.layout.donation_receipt_list_item))
  }

  class Model(
    val record: DonationReceiptRecord,
    val badge: Badge?
  ) : MappingModel<Model> {
    override fun areContentsTheSame(newItem: Model): Boolean = record == newItem.record && badge == newItem.badge

    override fun areItemsTheSame(newItem: Model): Boolean = record.id == newItem.record.id
  }

  private class ViewHolder(itemView: View, private val onClick: (Model) -> Unit) : MappingViewHolder<Model>(itemView) {

    private val badgeView: BadgeImageView = itemView.findViewById(R.id.badge)
    private val dateView: TextView = itemView.findViewById(R.id.date)
    private val typeView: TextView = itemView.findViewById(R.id.type)
    private val moneyView: TextView = itemView.findViewById(R.id.money)

    override fun bind(model: Model) {
      itemView.setOnClickListener { onClick(model) }
      badgeView.setBadge(model.badge)
      dateView.text = DateUtils.formatDate(Locale.getDefault(), model.record.timestamp)
      typeView.setText(
        when (model.record.type) {
          DonationReceiptRecord.Type.RECURRING -> R.string.DonationReceiptListFragment__recurring
          DonationReceiptRecord.Type.BOOST -> R.string.DonationReceiptListFragment__one_time
        }
      )
      moneyView.text = FiatMoneyUtil.format(context.resources, model.record.amount)
    }
  }
}
