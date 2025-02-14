package asia.coolapp.chat.badges.self.featured

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import asia.coolapp.chat.R
import asia.coolapp.chat.badges.BadgeRepository
import asia.coolapp.chat.badges.Badges
import asia.coolapp.chat.badges.Badges.displayBadges
import asia.coolapp.chat.badges.models.Badge
import asia.coolapp.chat.badges.models.BadgePreview
import asia.coolapp.chat.components.recyclerview.OnScrollAnimationHelper
import asia.coolapp.chat.components.recyclerview.ToolbarShadowAnimationHelper
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsFragment
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.util.LifecycleDisposable

/**
 * Fragment which allows user to select one of their badges to be their "Featured" badge.
 */
class SelectFeaturedBadgeFragment : DSLSettingsFragment(
  titleId = R.string.BadgesOverviewFragment__featured_badge,
  layoutId = R.layout.select_featured_badge_fragment,
  layoutManagerProducer = Badges::createLayoutManagerForGridWithBadges
) {

  private val viewModel: SelectFeaturedBadgeViewModel by viewModels(factoryProducer = { SelectFeaturedBadgeViewModel.Factory(BadgeRepository(requireContext())) })

  private val lifecycleDisposable = LifecycleDisposable()

  private lateinit var scrollShadow: View
  private lateinit var save: View

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    scrollShadow = view.findViewById(R.id.scroll_shadow)

    super.onViewCreated(view, savedInstanceState)

    save = view.findViewById(R.id.save)
    save.setOnClickListener {
      viewModel.save()
    }
  }

  override fun getOnScrollAnimationHelper(toolbarShadow: View): OnScrollAnimationHelper {
    return ToolbarShadowAnimationHelper(scrollShadow)
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    Badge.register(adapter) { badge, isSelected, _ ->
      if (!isSelected) {
        viewModel.setSelectedBadge(badge)
      }
    }

    val previewView: View = requireView().findViewById(R.id.preview)
    val previewViewHolder = BadgePreview.ViewHolder<BadgePreview.Model>(previewView)

    lifecycleDisposable.bindTo(viewLifecycleOwner.lifecycle)
    lifecycleDisposable += viewModel.events.subscribe { event: SelectFeaturedBadgeEvent ->
      when (event) {
        SelectFeaturedBadgeEvent.NO_BADGE_SELECTED -> Toast.makeText(requireContext(), R.string.SelectFeaturedBadgeFragment__you_must_select_a_badge, Toast.LENGTH_LONG).show()
        SelectFeaturedBadgeEvent.FAILED_TO_UPDATE_PROFILE -> Toast.makeText(requireContext(), R.string.SelectFeaturedBadgeFragment__failed_to_update_profile, Toast.LENGTH_LONG).show()
        SelectFeaturedBadgeEvent.SAVE_SUCCESSFUL -> findNavController().popBackStack()
      }
    }

    var hasBoundPreview = false
    viewModel.state.observe(viewLifecycleOwner) { state ->
      save.isEnabled = state.stage == SelectFeaturedBadgeState.Stage.READY

      if (hasBoundPreview) {
        previewViewHolder.setPayload(listOf(Unit))
      } else {
        hasBoundPreview = true
      }

      previewViewHolder.bind(BadgePreview.Model(state.selectedBadge))
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }
  }

  private fun getConfiguration(state: SelectFeaturedBadgeState): DSLConfiguration {
    return configure {
      sectionHeaderPref(R.string.SelectFeaturedBadgeFragment__select_a_badge)
      displayBadges(requireContext(), state.allUnlockedBadges, state.selectedBadge)
    }
  }
}
