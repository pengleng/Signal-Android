package asia.coolapp.chat.mediasend.v2.text.send

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.signal.core.util.DimensionUnit
import asia.coolapp.chat.R
import asia.coolapp.chat.contacts.paged.ContactSearchConfiguration
import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.contacts.paged.ContactSearchMediator
import asia.coolapp.chat.conversation.ui.error.SafetyNumberChangeDialog
import asia.coolapp.chat.linkpreview.LinkPreviewViewModel
import asia.coolapp.chat.mediasend.v2.stories.ChooseGroupStoryBottomSheet
import asia.coolapp.chat.mediasend.v2.stories.ChooseStoryTypeBottomSheet
import asia.coolapp.chat.mediasend.v2.text.TextStoryPostCreationViewModel
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.sharing.ShareSelectionAdapter
import asia.coolapp.chat.sharing.ShareSelectionMappingModel
import asia.coolapp.chat.stories.Stories
import asia.coolapp.chat.stories.dialogs.StoryDialogs
import asia.coolapp.chat.stories.settings.create.CreateStoryFlowDialogFragment
import asia.coolapp.chat.stories.settings.create.CreateStoryWithViewersFragment
import asia.coolapp.chat.stories.settings.hide.HideStoryFromDialogFragment
import asia.coolapp.chat.util.BottomSheetUtil
import asia.coolapp.chat.util.FeatureFlags
import asia.coolapp.chat.util.LifecycleDisposable
import asia.coolapp.chat.util.livedata.LiveDataUtil

class TextStoryPostSendFragment : Fragment(R.layout.stories_send_text_post_fragment), ChooseStoryTypeBottomSheet.Callback {

  private lateinit var shareListWrapper: View
  private lateinit var shareSelectionRecyclerView: RecyclerView
  private lateinit var shareConfirmButton: View

  private val shareSelectionAdapter = ShareSelectionAdapter()
  private val disposables = LifecycleDisposable()

  private lateinit var contactSearchMediator: ContactSearchMediator

  private val viewModel: TextStoryPostSendViewModel by viewModels(
    factoryProducer = {
      TextStoryPostSendViewModel.Factory(TextStoryPostSendRepository())
    }
  )

  private val creationViewModel: TextStoryPostCreationViewModel by viewModels(
    ownerProducer = {
      requireActivity()
    }
  )

  private val linkPreviewViewModel: LinkPreviewViewModel by viewModels(
    ownerProducer = {
      requireActivity()
    }
  )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    val viewPort: ImageView = view.findViewById(R.id.preview_viewport)
    val searchField: EditText = view.findViewById(R.id.search_field)

    toolbar.setNavigationOnClickListener {
      findNavController().popBackStack()
    }

    shareListWrapper = view.findViewById(R.id.list_wrapper)
    shareConfirmButton = view.findViewById(R.id.share_confirm)
    shareSelectionRecyclerView = view.findViewById(R.id.selected_list)
    shareSelectionRecyclerView.adapter = shareSelectionAdapter

    disposables.bindTo(viewLifecycleOwner)

    creationViewModel.thumbnail.observe(viewLifecycleOwner) { bitmap ->
      viewPort.setImageBitmap(bitmap)
    }

    shareConfirmButton.setOnClickListener {
      viewModel.onSending()
      StoryDialogs.guardWithAddToYourStoryDialog(
        contacts = contactSearchMediator.getSelectedContacts(),
        context = requireContext(),
        onAddToStory = { send() },
        onEditViewers = {
          viewModel.onSendCancelled()
          HideStoryFromDialogFragment().show(childFragmentManager, null)
        },
        onCancel = {
          viewModel.onSendCancelled()
        }
      )
    }

    disposables += viewModel.untrustedIdentities.subscribe {
      SafetyNumberChangeDialog.show(childFragmentManager, it)
    }

    searchField.doAfterTextChanged {
      contactSearchMediator.onFilterChanged(it?.toString())
    }

    setFragmentResultListener(CreateStoryWithViewersFragment.REQUEST_KEY) { _, bundle ->
      val recipientId: RecipientId = bundle.getParcelable(CreateStoryWithViewersFragment.STORY_RECIPIENT)!!
      contactSearchMediator.setKeysSelected(setOf(ContactSearchKey.Story(recipientId)))
      contactSearchMediator.onFilterChanged("")
    }

    setFragmentResultListener(ChooseGroupStoryBottomSheet.GROUP_STORY) { _, bundle ->
      val groups: Set<RecipientId> = bundle.getParcelableArrayList<RecipientId>(ChooseGroupStoryBottomSheet.RESULT_SET)?.toSet() ?: emptySet()
      val keys: Set<ContactSearchKey.Story> = groups.map { ContactSearchKey.Story(it) }.toSet()
      contactSearchMediator.addToVisibleGroupStories(keys)
      contactSearchMediator.onFilterChanged("")
      contactSearchMediator.setKeysSelected(keys)
    }

    val contactsRecyclerView: RecyclerView = view.findViewById(R.id.contacts_container)
    contactSearchMediator = ContactSearchMediator(this, contactsRecyclerView, FeatureFlags.shareSelectionLimit()) { contactSearchState ->
      ContactSearchConfiguration.build {
        query = contactSearchState.query

        addSection(
          ContactSearchConfiguration.Section.Stories(
            groupStories = contactSearchState.groupStories,
            includeHeader = true,
            headerAction = Stories.getHeaderAction(childFragmentManager)
          )
        )
      }
    }

    contactSearchMediator.getSelectionState().observe(viewLifecycleOwner) { selection ->
      shareSelectionAdapter.submitList(selection.mapIndexed { index, contact -> ShareSelectionMappingModel(contact.requireShareContact(), index == 0) })
      if (selection.isNotEmpty()) {
        animateInSelection()
      } else {
        animateOutSelection()
      }
    }

    val saveStateAndSelection = LiveDataUtil.combineLatest(viewModel.state, contactSearchMediator.getSelectionState(), ::Pair)
    saveStateAndSelection.observe(viewLifecycleOwner) { (state, selection) ->
      when (state) {
        TextStoryPostSendState.INIT -> shareConfirmButton.isEnabled = selection.isNotEmpty()
        TextStoryPostSendState.SENDING -> shareConfirmButton.isEnabled = false
        TextStoryPostSendState.SENT -> requireActivity().finish()
      }
    }
  }

  private fun send() {
    shareConfirmButton.isEnabled = false

    val textStoryPostCreationState = creationViewModel.state.value

    viewModel.onSend(contactSearchMediator.getSelectedContacts(), textStoryPostCreationState!!, linkPreviewViewModel.onSend().firstOrNull())
  }

  private fun animateInSelection() {
    shareListWrapper.animate()
      .alpha(1f)
      .translationY(0f)
    shareConfirmButton.animate()
      .alpha(1f)
  }

  private fun animateOutSelection() {
    shareListWrapper.animate()
      .alpha(0f)
      .translationY(DimensionUnit.DP.toPixels(48f))
    shareConfirmButton.animate()
      .alpha(0f)
  }

  override fun onNewStoryClicked() {
    CreateStoryFlowDialogFragment().show(parentFragmentManager, CreateStoryWithViewersFragment.REQUEST_KEY)
  }

  override fun onGroupStoryClicked() {
    ChooseGroupStoryBottomSheet().show(parentFragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
  }
}
