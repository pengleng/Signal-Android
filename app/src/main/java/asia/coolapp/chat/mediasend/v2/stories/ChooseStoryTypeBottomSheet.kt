package asia.coolapp.chat.mediasend.v2.stories

import org.signal.core.util.DimensionUnit
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsBottomSheetFragment
import asia.coolapp.chat.components.settings.DSLSettingsIcon
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.components.settings.conversation.preferences.LargeIconClickPreference
import asia.coolapp.chat.util.fragments.requireListener

class ChooseStoryTypeBottomSheet : DSLSettingsBottomSheetFragment(
  layoutId = R.layout.dsl_settings_bottom_sheet_no_handle
) {
  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    LargeIconClickPreference.register(adapter)
    adapter.submitList(getConfiguration().toMappingModelList())
  }

  private fun getConfiguration(): DSLConfiguration {
    return configure {
      textPref(
        title = DSLSettingsText.from(
          stringId = R.string.ChooseStoryTypeBottomSheet__choose_your_story_type,
          DSLSettingsText.CenterModifier, DSLSettingsText.Body1BoldModifier, DSLSettingsText.BoldModifier
        )
      )

      customPref(
        LargeIconClickPreference.Model(
          title = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__new_private_story
          ),
          summary = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__visible_only_to
          ),
          icon = DSLSettingsIcon.from(
            R.drawable.ic_plus_24,
            R.color.core_grey_15,
            R.drawable.circle_tintable,
            R.color.core_grey_80,
            DimensionUnit.DP.toPixels(8f).toInt()
          ),
          onClick = {
            dismissAllowingStateLoss()
            requireListener<Callback>().onNewStoryClicked()
          }
        )
      )

      customPref(
        LargeIconClickPreference.Model(
          title = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__group_story
          ),
          summary = DSLSettingsText.from(
            stringId = R.string.ChooseStoryTypeBottomSheet__share_to_an_existing_group
          ),
          icon = DSLSettingsIcon.from(
            R.drawable.ic_group_outline_24,
            R.color.core_grey_15,
            R.drawable.circle_tintable,
            R.color.core_grey_80,
            DimensionUnit.DP.toPixels(8f).toInt()
          ),
          onClick = {
            dismissAllowingStateLoss()
            requireListener<Callback>().onGroupStoryClicked()
          }
        )
      )

      space(DimensionUnit.DP.toPixels(32f).toInt())
    }
  }

  interface Callback {
    fun onNewStoryClicked()
    fun onGroupStoryClicked()
  }
}
