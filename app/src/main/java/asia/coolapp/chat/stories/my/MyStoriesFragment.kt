package asia.coolapp.chat.stories.my

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import asia.coolapp.chat.R
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.DSLSettingsAdapter
import asia.coolapp.chat.components.settings.DSLSettingsFragment
import asia.coolapp.chat.components.settings.DSLSettingsText
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.conversation.mutiselect.forward.MultiselectForwardFragment
import asia.coolapp.chat.conversation.mutiselect.forward.MultiselectForwardFragmentArgs
import asia.coolapp.chat.database.model.MediaMmsMessageRecord
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.stories.dialogs.StoryContextMenu
import asia.coolapp.chat.stories.viewer.StoryViewerActivity
import asia.coolapp.chat.util.LifecycleDisposable

class MyStoriesFragment : DSLSettingsFragment(
  titleId = R.string.StoriesLandingFragment__my_stories
) {

  private val lifecycleDisposable = LifecycleDisposable()

  private val viewModel: MyStoriesViewModel by viewModels(
    factoryProducer = {
      MyStoriesViewModel.Factory(MyStoriesRepository(requireContext()))
    }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    MyStoriesItem.register(adapter)

    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          requireActivity().finish()
        }
      }
    )

    lifecycleDisposable.bindTo(viewLifecycleOwner)
    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
      if (it.distributionSets.isEmpty()) {
        requireActivity().finish()
      }
    }
  }

  private fun getConfiguration(state: MyStoriesState): DSLConfiguration {
    return configure {
      val nonEmptySets = state.distributionSets.filter { it.stories.isNotEmpty() }
      nonEmptySets
        .forEachIndexed { index, distributionSet ->
          sectionHeaderPref(
            if (distributionSet.label == null) {
              DSLSettingsText.from(getString(R.string.MyStories__ss_story, Recipient.self().getShortDisplayName(requireContext())))
            } else {
              DSLSettingsText.from(distributionSet.label)
            }
          )
          distributionSet.stories.forEach { conversationMessage ->
            customPref(
              MyStoriesItem.Model(
                distributionStory = conversationMessage,
                onClick = { it, preview ->
                  if (it.distributionStory.messageRecord.isOutgoing && it.distributionStory.messageRecord.isFailed) {
                    lifecycleDisposable += viewModel.resend(it.distributionStory.messageRecord).subscribe()
                    Toast.makeText(requireContext(), R.string.message_recipients_list_item__resend, Toast.LENGTH_SHORT).show()
                  } else {
                    val recipientId = if (it.distributionStory.messageRecord.recipient.isGroup) {
                      it.distributionStory.messageRecord.recipient.id
                    } else {
                      Recipient.self().id
                    }

                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), preview, ViewCompat.getTransitionName(preview) ?: "")
                    startActivity(StoryViewerActivity.createIntent(requireContext(), recipientId, conversationMessage.messageRecord.id), options.toBundle())
                  }
                },
                onSaveClick = {
                  StoryContextMenu.save(requireContext(), it.distributionStory.messageRecord)
                },
                onDeleteClick = this@MyStoriesFragment::handleDeleteClick,
                onForwardClick = { item ->
                  MultiselectForwardFragmentArgs.create(
                    requireContext(),
                    item.distributionStory.multiselectCollection.toSet()
                  ) {
                    MultiselectForwardFragment.showBottomSheet(childFragmentManager, it)
                  }
                },
                onShareClick = {
                  StoryContextMenu.share(this@MyStoriesFragment, it.distributionStory.messageRecord as MediaMmsMessageRecord)
                }
              )
            )
          }

          if (index != nonEmptySets.lastIndex) {
            dividerPref()
          }
        }
    }
  }

  private fun handleDeleteClick(model: MyStoriesItem.Model) {
    lifecycleDisposable += StoryContextMenu.delete(requireContext(), setOf(model.distributionStory.messageRecord)).subscribe()
  }
}
