package asia.coolapp.chat.components.settings.conversation.preferences

import android.view.View
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.EmojiTextView
import asia.coolapp.chat.components.settings.PreferenceModel
import asia.coolapp.chat.groups.GroupId
import asia.coolapp.chat.groups.v2.GroupDescriptionUtil
import asia.coolapp.chat.util.LongClickMovementMethod
import asia.coolapp.chat.util.adapter.mapping.LayoutFactory
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder

object GroupDescriptionPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_group_description_preference))
  }

  class Model(
    private val groupId: GroupId,
    val groupDescription: String?,
    val descriptionShouldLinkify: Boolean,
    val canEditGroupAttributes: Boolean,
    val onEditGroupDescription: () -> Unit,
    val onViewGroupDescription: () -> Unit
  ) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return groupId == newItem.groupId
    }

    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) &&
        groupDescription == newItem.groupDescription &&
        descriptionShouldLinkify == newItem.descriptionShouldLinkify &&
        canEditGroupAttributes == newItem.canEditGroupAttributes
    }
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val groupDescriptionTextView: EmojiTextView = findViewById(R.id.manage_group_description)

    override fun bind(model: Model) {
      groupDescriptionTextView.movementMethod = LongClickMovementMethod.getInstance(context)

      if (model.groupDescription.isNullOrEmpty()) {
        if (model.canEditGroupAttributes) {
          groupDescriptionTextView.setOverflowText(null)
          groupDescriptionTextView.setText(R.string.ManageGroupActivity_add_group_description)
          groupDescriptionTextView.setOnClickListener { model.onEditGroupDescription() }
        }
      } else {
        groupDescriptionTextView.setOnClickListener(null)
        GroupDescriptionUtil.setText(
          context,
          groupDescriptionTextView,
          model.groupDescription,
          model.descriptionShouldLinkify
        ) {
          model.onViewGroupDescription()
        }
      }
    }
  }
}
