package asia.coolapp.chat.avatar.picker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import org.signal.core.util.ThreadUtil
import asia.coolapp.chat.R
import asia.coolapp.chat.avatar.Avatar
import asia.coolapp.chat.avatar.AvatarBundler
import asia.coolapp.chat.avatar.photo.PhotoEditorFragment
import asia.coolapp.chat.avatar.text.TextAvatarCreationFragment
import asia.coolapp.chat.avatar.vector.VectorAvatarCreationFragment
import asia.coolapp.chat.components.ButtonStripItemView
import asia.coolapp.chat.components.recyclerview.GridDividerDecoration
import asia.coolapp.chat.groups.ParcelableGroupId
import asia.coolapp.chat.mediasend.AvatarSelectionActivity
import asia.coolapp.chat.mediasend.Media
import asia.coolapp.chat.permissions.Permissions
import asia.coolapp.chat.util.ViewUtil
import asia.coolapp.chat.util.adapter.mapping.MappingAdapter
import asia.coolapp.chat.util.navigation.safeNavigate
import asia.coolapp.chat.util.visible

/**
 * Primary Avatar picker fragment, displays current user avatar and a list of recently used avatars and defaults.
 */
class AvatarPickerFragment : Fragment(R.layout.avatar_picker_fragment) {

  companion object {
    const val REQUEST_KEY_SELECT_AVATAR = "asia.coolapp.chat.avatar.picker.SELECT_AVATAR"
    const val SELECT_AVATAR_MEDIA = "asia.coolapp.chat.avatar.picker.SELECT_AVATAR_MEDIA"
    const val SELECT_AVATAR_CLEAR = "asia.coolapp.chat.avatar.picker.SELECT_AVATAR_CLEAR"

    private const val REQUEST_CODE_SELECT_IMAGE = 1
  }

  private val viewModel: AvatarPickerViewModel by viewModels(factoryProducer = this::createFactory)

  private lateinit var recycler: RecyclerView

  private fun createFactory(): AvatarPickerViewModel.Factory {
    val args = AvatarPickerFragmentArgs.fromBundle(requireArguments())
    val groupId = ParcelableGroupId.get(args.groupId)

    return AvatarPickerViewModel.Factory(AvatarPickerRepository(requireContext()), groupId, args.isNewGroup, args.groupAvatarMedia)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val toolbar: Toolbar = view.findViewById(R.id.avatar_picker_toolbar)
    val cameraButton: ButtonStripItemView = view.findViewById(R.id.avatar_picker_camera)
    val photoButton: ButtonStripItemView = view.findViewById(R.id.avatar_picker_photo)
    val textButton: ButtonStripItemView = view.findViewById(R.id.avatar_picker_text)
    val saveButton: View = view.findViewById(R.id.avatar_picker_save)
    val clearButton: View = view.findViewById(R.id.avatar_picker_clear)

    recycler = view.findViewById(R.id.avatar_picker_recycler)
    recycler.addItemDecoration(GridDividerDecoration(4, ViewUtil.dpToPx(16)))

    val adapter = MappingAdapter()
    AvatarPickerItem.register(adapter, this::onAvatarClick, this::onAvatarLongClick)

    recycler.adapter = adapter

    val avatarViewHolder = AvatarPickerItem.ViewHolder(view)

    viewModel.state.observe(viewLifecycleOwner) { state ->
      if (state.currentAvatar != null) {
        avatarViewHolder.bind(AvatarPickerItem.Model(state.currentAvatar, false))
      }

      clearButton.visible = state.canClear

      val wasEnabled = saveButton.isEnabled
      saveButton.isEnabled = state.canSave
      if (wasEnabled != state.canSave) {
        val alpha = if (state.canSave) 1f else 0.5f
        saveButton.animate().cancel()
        saveButton.animate().alpha(alpha)
      }

      val items = state.selectableAvatars.map { AvatarPickerItem.Model(it, it == state.currentAvatar) }
      val selectedPosition = items.indexOfFirst { it.isSelected }

      adapter.submitList(items) {
        if (selectedPosition > -1)
          recycler.smoothScrollToPosition(selectedPosition)
      }
    }

    toolbar.setNavigationOnClickListener { Navigation.findNavController(it).popBackStack() }
    cameraButton.setOnIconClickedListener { openCameraCapture() }
    photoButton.setOnIconClickedListener { openGallery() }
    textButton.setOnIconClickedListener { openTextEditor(null) }
    saveButton.setOnClickListener { v ->
      viewModel.save(
        {
          setFragmentResult(
            REQUEST_KEY_SELECT_AVATAR,
            Bundle().apply {
              putParcelable(SELECT_AVATAR_MEDIA, it)
            }
          )
          ThreadUtil.runOnMain { Navigation.findNavController(v).popBackStack() }
        },
        {
          setFragmentResult(
            REQUEST_KEY_SELECT_AVATAR,
            Bundle().apply {
              putBoolean(SELECT_AVATAR_CLEAR, true)
            }
          )
          ThreadUtil.runOnMain { Navigation.findNavController(v).popBackStack() }
        }
      )
    }
    clearButton.setOnClickListener { viewModel.clear() }

    setFragmentResultListener(TextAvatarCreationFragment.REQUEST_KEY_TEXT) { _, bundle ->
      val text = AvatarBundler.extractText(bundle)
      viewModel.onAvatarEditCompleted(text)
    }

    setFragmentResultListener(VectorAvatarCreationFragment.REQUEST_KEY_VECTOR) { _, bundle ->
      val vector = AvatarBundler.extractVector(bundle)
      viewModel.onAvatarEditCompleted(vector)
    }

    setFragmentResultListener(PhotoEditorFragment.REQUEST_KEY_EDIT) { _, bundle ->
      val photo = AvatarBundler.extractPhoto(bundle)
      viewModel.onAvatarEditCompleted(photo)
    }
  }

  override fun onResume() {
    super.onResume()
    ViewUtil.hideKeyboard(requireContext(), requireView())
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
      val media: Media = requireNotNull(data.getParcelableExtra(AvatarSelectionActivity.EXTRA_MEDIA))
      viewModel.onAvatarPhotoSelectionCompleted(media)
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun onAvatarClick(avatar: Avatar, isSelected: Boolean) {
    if (isSelected) {
      openEditor(avatar)
    } else {
      viewModel.onAvatarSelectedFromGrid(avatar)
    }
  }

  private fun onAvatarLongClick(anchorView: View, avatar: Avatar): Boolean {
    val menuRes = when (avatar) {
      is Avatar.Photo -> R.menu.avatar_picker_context
      is Avatar.Text -> R.menu.avatar_picker_context
      is Avatar.Vector -> return true
      is Avatar.Resource -> return true
    }

    val popup = PopupMenu(context, anchorView, Gravity.TOP)
    popup.menuInflater.inflate(menuRes, popup.menu)
    popup.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.action_delete -> viewModel.delete(avatar)
      }

      true
    }
    popup.show()

    return true
  }

  fun openEditor(avatar: Avatar) {
    when (avatar) {
      is Avatar.Photo -> openPhotoEditor(avatar)
      is Avatar.Resource -> throw UnsupportedOperationException()
      is Avatar.Text -> openTextEditor(avatar)
      is Avatar.Vector -> openVectorEditor(avatar)
    }
  }

  private fun openPhotoEditor(photo: Avatar.Photo) {
    Navigation.findNavController(requireView())
      .safeNavigate(AvatarPickerFragmentDirections.actionAvatarPickerFragmentToAvatarPhotoEditorFragment(AvatarBundler.bundlePhoto(photo)))
  }

  private fun openVectorEditor(vector: Avatar.Vector) {
    Navigation.findNavController(requireView())
      .safeNavigate(AvatarPickerFragmentDirections.actionAvatarPickerFragmentToVectorAvatarCreationFragment(AvatarBundler.bundleVector(vector)))
  }

  private fun openTextEditor(text: Avatar.Text?) {
    val bundle = if (text != null) AvatarBundler.bundleText(text) else null
    Navigation.findNavController(requireView())
      .safeNavigate(AvatarPickerFragmentDirections.actionAvatarPickerFragmentToTextAvatarCreationFragment(bundle))
  }

  @Suppress("DEPRECATION")
  private fun openCameraCapture() {
    Permissions.with(this)
      .request(Manifest.permission.CAMERA)
      .ifNecessary()
      .onAllGranted {
        val intent = AvatarSelectionActivity.getIntentForCameraCapture(requireContext())
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
      }
      .onAnyDenied {
        Toast.makeText(requireContext(), R.string.AvatarSelectionBottomSheetDialogFragment__taking_a_photo_requires_the_camera_permission, Toast.LENGTH_SHORT)
          .show()
      }
      .execute()
  }

  @Suppress("DEPRECATION")
  private fun openGallery() {
    Permissions.with(this)
      .request(Manifest.permission.READ_EXTERNAL_STORAGE)
      .ifNecessary()
      .onAllGranted {
        val intent = AvatarSelectionActivity.getIntentForGallery(requireContext())
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
      }
      .onAnyDenied {
        Toast.makeText(requireContext(), R.string.AvatarSelectionBottomSheetDialogFragment__viewing_your_gallery_requires_the_storage_permission, Toast.LENGTH_SHORT)
          .show()
      }
      .execute()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }
}
