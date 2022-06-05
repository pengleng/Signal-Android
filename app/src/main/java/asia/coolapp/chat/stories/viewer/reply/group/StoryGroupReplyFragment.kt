package asia.coolapp.chat.stories.viewer.reply.group

import android.content.ClipData
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asia.coolapp.chat.R
import asia.coolapp.chat.components.emoji.MediaKeyboard
import asia.coolapp.chat.components.mention.MentionAnnotation
import asia.coolapp.chat.components.settings.DSLConfiguration
import asia.coolapp.chat.components.settings.configure
import asia.coolapp.chat.conversation.colors.Colorizer
import asia.coolapp.chat.conversation.ui.mentions.MentionsPickerFragment
import asia.coolapp.chat.conversation.ui.mentions.MentionsPickerViewModel
import asia.coolapp.chat.keyboard.KeyboardPage
import asia.coolapp.chat.keyboard.KeyboardPagerViewModel
import asia.coolapp.chat.keyboard.emoji.EmojiKeyboardCallback
import asia.coolapp.chat.reactions.any.ReactWithAnyEmojiBottomSheetDialogFragment
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.recipients.RecipientId
import asia.coolapp.chat.recipients.ui.bottomsheet.RecipientBottomSheetDialogFragment
import asia.coolapp.chat.stories.viewer.reply.BottomSheetBehaviorDelegate
import asia.coolapp.chat.stories.viewer.reply.StoryViewsAndRepliesPagerChild
import asia.coolapp.chat.stories.viewer.reply.StoryViewsAndRepliesPagerParent
import asia.coolapp.chat.stories.viewer.reply.composer.StoryReactionBar
import asia.coolapp.chat.stories.viewer.reply.composer.StoryReplyComposer
import asia.coolapp.chat.util.DeleteDialog
import asia.coolapp.chat.util.FragmentDialogs.displayInDialogAboveAnchor
import asia.coolapp.chat.util.LifecycleDisposable
import asia.coolapp.chat.util.Projection
import asia.coolapp.chat.util.ServiceUtil
import asia.coolapp.chat.util.ViewUtil
import asia.coolapp.chat.util.adapter.mapping.PagingMappingAdapter
import asia.coolapp.chat.util.fragments.findListener
import asia.coolapp.chat.util.fragments.requireListener
import asia.coolapp.chat.util.visible

/**
 * Fragment which contains UI to reply to a group story
 */
class StoryGroupReplyFragment :
  Fragment(R.layout.stories_group_replies_fragment),
  StoryViewsAndRepliesPagerChild,
  BottomSheetBehaviorDelegate,
  StoryReplyComposer.Callback,
  EmojiKeyboardCallback,
  ReactWithAnyEmojiBottomSheetDialogFragment.Callback {

  private val viewModel: StoryGroupReplyViewModel by viewModels(
    factoryProducer = {
      StoryGroupReplyViewModel.Factory(storyId, StoryGroupReplyRepository())
    }
  )

  private val mentionsViewModel: MentionsPickerViewModel by viewModels(
    factoryProducer = { MentionsPickerViewModel.Factory() },
    ownerProducer = { requireActivity() }
  )

  private val keyboardPagerViewModel: KeyboardPagerViewModel by viewModels(
    ownerProducer = { requireActivity() }
  )

  private val colorizer = Colorizer()
  private val lifecycleDisposable = LifecycleDisposable()

  private val storyId: Long
    get() = requireArguments().getLong(ARG_STORY_ID)

  private val groupRecipientId: RecipientId
    get() = requireArguments().getParcelable(ARG_GROUP_RECIPIENT_ID)!!

  private lateinit var recyclerView: RecyclerView
  private lateinit var composer: StoryReplyComposer

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    recyclerView = view.findViewById(R.id.recycler)
    composer = view.findViewById(R.id.composer)

    lifecycleDisposable.bindTo(viewLifecycleOwner)

    val emptyNotice: View = requireView().findViewById(R.id.empty_notice)

    val adapter = PagingMappingAdapter<StoryGroupReplyItemData.Key>()
    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
    recyclerView.layoutManager = layoutManager
    recyclerView.adapter = adapter
    recyclerView.itemAnimator = null
    StoryGroupReplyItem.register(adapter)

    composer.callback = this

    onPageSelected(findListener<StoryViewsAndRepliesPagerParent>()?.selectedChild ?: StoryViewsAndRepliesPagerParent.Child.REPLIES)

    viewModel.state.observe(viewLifecycleOwner) { state ->
      emptyNotice.visible = state.noReplies && state.loadState == StoryGroupReplyState.LoadState.READY
      colorizer.onNameColorsChanged(state.nameColors)
    }

    viewModel.pagingController.observe(viewLifecycleOwner) { controller ->
      adapter.setPagingController(controller)
    }

    viewModel.pageData.observe(viewLifecycleOwner) { pageData ->
      val isScrolledToBottom = recyclerView.canScrollVertically(-1)
      adapter.submitList(getConfiguration(pageData).toMappingModelList()) {
        if (isScrolledToBottom) {
          recyclerView.doOnNextLayout {
            recyclerView.smoothScrollToPosition(0)
          }
        }
      }
    }

    initializeMentions()
  }

  override fun onDestroyView() {
    super.onDestroyView()

    composer.input.setMentionQueryChangedListener(null)
    composer.input.setMentionValidator(null)
  }

  private fun getConfiguration(pageData: List<StoryGroupReplyItemData>): DSLConfiguration {
    return configure {
      pageData.filterNotNull().forEach {
        when (it.replyBody) {
          is StoryGroupReplyItemData.ReplyBody.Text -> {
            customPref(
              StoryGroupReplyItem.TextModel(
                storyGroupReplyItemData = it,
                text = it.replyBody,
                nameColor = colorizer.getIncomingGroupSenderColor(
                  requireContext(),
                  it.sender
                ),
                onPrivateReplyClick = { model ->
                  requireListener<Callback>().onStartDirectReply(model.storyGroupReplyItemData.sender.id)
                },
                onCopyClick = { model ->
                  val clipData = ClipData.newPlainText(requireContext().getString(R.string.app_name), model.text.message.getDisplayBody(requireContext()))
                  ServiceUtil.getClipboardManager(requireContext()).setPrimaryClip(clipData)
                  Toast.makeText(requireContext(), R.string.StoryGroupReplyFragment__copied_to_clipboard, Toast.LENGTH_SHORT).show()
                },
                onDeleteClick = { model ->
                  lifecycleDisposable += DeleteDialog.show(requireActivity(), setOf(model.text.message.messageRecord)).subscribe { result ->
                    if (result) {
                      throw AssertionError("We should never end up deleting a Group Thread like this.")
                    }
                  }
                },
                onMentionClick = { recipientId ->
                  RecipientBottomSheetDialogFragment
                    .create(recipientId, null)
                    .show(childFragmentManager, null)
                }
              )
            )
          }
          is StoryGroupReplyItemData.ReplyBody.Reaction -> {
            customPref(
              StoryGroupReplyItem.ReactionModel(
                storyGroupReplyItemData = it,
                reaction = it.replyBody,
                nameColor = colorizer.getIncomingGroupSenderColor(
                  requireContext(),
                  it.sender
                )
              )
            )
          }
        }
      }
    }
  }

  override fun onSlide(bottomSheet: View) {
    val inputProjection = Projection.relativeToViewRoot(composer, null)
    val parentProjection = Projection.relativeToViewRoot(bottomSheet.parent as ViewGroup, null)
    composer.translationY = (parentProjection.height + parentProjection.y - (inputProjection.y + inputProjection.height))
    inputProjection.release()
    parentProjection.release()
  }

  override fun onPageSelected(child: StoryViewsAndRepliesPagerParent.Child) {
    recyclerView.isNestedScrollingEnabled = child == StoryViewsAndRepliesPagerParent.Child.REPLIES
  }

  override fun onSendActionClicked() {
    val (body, mentions) = composer.consumeInput()
    lifecycleDisposable += StoryGroupReplySender.sendReply(requireContext(), storyId, body, mentions).subscribe()
  }

  override fun onPickReactionClicked() {
    displayInDialogAboveAnchor(composer.reactionButton, R.layout.stories_reaction_bar_layout) { dialog, view ->
      view.findViewById<StoryReactionBar>(R.id.reaction_bar).apply {
        callback = object : StoryReactionBar.Callback {
          override fun onTouchOutsideOfReactionBar() {
            dialog.dismiss()
          }

          override fun onReactionSelected(emoji: String) {
            dialog.dismiss()
            sendReaction(emoji)
          }

          override fun onOpenReactionPicker() {
            dialog.dismiss()
            ReactWithAnyEmojiBottomSheetDialogFragment.createForStory().show(childFragmentManager, null)
          }
        }
        animateIn()
      }
    }
  }

  override fun onEmojiSelected(emoji: String?) {
    composer.onEmojiSelected(emoji)
  }

  private fun sendReaction(emoji: String) {
    lifecycleDisposable += StoryGroupReplySender.sendReaction(requireContext(), storyId, emoji).subscribe()
  }

  override fun onKeyEvent(keyEvent: KeyEvent?) = Unit

  override fun onInitializeEmojiDrawer(mediaKeyboard: MediaKeyboard) {
    keyboardPagerViewModel.setOnlyPage(KeyboardPage.EMOJI)
    mediaKeyboard.setFragmentManager(childFragmentManager)
  }

  override fun openEmojiSearch() {
    composer.openEmojiSearch()
  }

  override fun closeEmojiSearch() {
    composer.closeEmojiSearch()
  }

  override fun onReactWithAnyEmojiDialogDismissed() {
  }

  override fun onReactWithAnyEmojiSelected(emoji: String) {
    sendReaction(emoji)
  }

  override fun onHeightChanged(height: Int) {
    ViewUtil.setPaddingBottom(recyclerView, height)
  }

  private fun initializeMentions() {
    Recipient.live(groupRecipientId).observe(viewLifecycleOwner) { recipient ->
      mentionsViewModel.onRecipientChange(recipient)

      composer.input.setMentionQueryChangedListener { query ->
        if (recipient.isPushV2Group) {
          ensureMentionsContainerFilled()
          mentionsViewModel.onQueryChange(query)
        }
      }

      composer.input.setMentionValidator { annotations ->
        if (!recipient.isPushV2Group) {
          annotations
        } else {

          val validRecipientIds: Set<String> = recipient.participants
            .map { r -> MentionAnnotation.idToMentionAnnotationValue(r.id) }
            .toSet()

          annotations
            .filter { !validRecipientIds.contains(it.value) }
            .toList()
        }
      }
    }

    mentionsViewModel.selectedRecipient.observe(viewLifecycleOwner) { recipient ->
      composer.input.replaceTextWithMention(recipient.getDisplayName(requireContext()), recipient.id)
    }
  }

  private fun ensureMentionsContainerFilled() {
    val mentionsFragment = childFragmentManager.findFragmentById(R.id.mentions_picker_container)
    if (mentionsFragment == null) {
      childFragmentManager
        .beginTransaction()
        .replace(R.id.mentions_picker_container, MentionsPickerFragment())
        .commitNowAllowingStateLoss()
    }
  }

  companion object {
    private const val ARG_STORY_ID = "arg.story.id"
    private const val ARG_GROUP_RECIPIENT_ID = "arg.group.recipient.id"

    fun create(storyId: Long, groupRecipientId: RecipientId): Fragment {
      return StoryGroupReplyFragment().apply {
        arguments = Bundle().apply {
          putLong(ARG_STORY_ID, storyId)
          putParcelable(ARG_GROUP_RECIPIENT_ID, groupRecipientId)
        }
      }
    }
  }

  interface Callback {
    fun onStartDirectReply(recipientId: RecipientId)
  }
}
