package asia.coolapp.chat.avatar.photo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import androidx.navigation.Navigation
import org.signal.core.util.ThreadUtil
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.R
import asia.coolapp.chat.avatar.AvatarBundler
import asia.coolapp.chat.avatar.AvatarPickerStorage
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.providers.BlobProvider
import asia.coolapp.chat.scribbles.ImageEditorFragment

class PhotoEditorFragment : Fragment(R.layout.avatar_photo_editor_fragment), ImageEditorFragment.Controller {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val args = PhotoEditorFragmentArgs.fromBundle(requireArguments())
    val photo = AvatarBundler.extractPhoto(args.photoAvatar)
    val imageEditorFragment = ImageEditorFragment.newInstanceForAvatarEdit(photo.uri)

    childFragmentManager.commit {
      add(R.id.fragment_container, imageEditorFragment, IMAGE_EDITOR)
    }
  }

  override fun onTouchEventsNeeded(needed: Boolean) {
  }

  override fun onRequestFullScreen(fullScreen: Boolean, hideKeyboard: Boolean) {
  }

  override fun onDoneEditing() {
    val args = PhotoEditorFragmentArgs.fromBundle(requireArguments())
    val applicationContext = requireContext().applicationContext
    val imageEditorFragment: ImageEditorFragment = childFragmentManager.findFragmentByTag(IMAGE_EDITOR) as ImageEditorFragment

    SignalExecutors.BOUNDED.execute {
      val editedImageUri = imageEditorFragment.renderToSingleUseBlob()
      val size = BlobProvider.getFileSize(editedImageUri) ?: 0
      val inputStream = BlobProvider.getInstance().getStream(applicationContext, editedImageUri)
      val onDiskUri = AvatarPickerStorage.save(applicationContext, inputStream)
      val photo = AvatarBundler.extractPhoto(args.photoAvatar)
      val database = SignalDatabase.avatarPicker
      val newPhoto = photo.copy(uri = onDiskUri, size = size)

      database.update(newPhoto)
      BlobProvider.getInstance().delete(requireContext(), photo.uri)

      ThreadUtil.runOnMain {
        setFragmentResult(REQUEST_KEY_EDIT, AvatarBundler.bundlePhoto(newPhoto))
        Navigation.findNavController(requireView()).popBackStack()
      }
    }
  }

  override fun onCancelEditing() {
    Navigation.findNavController(requireView()).popBackStack()
  }

  override fun onMainImageLoaded() {
  }

  override fun onMainImageFailedToLoad() {
  }

  companion object {
    const val REQUEST_KEY_EDIT = "asia.coolapp.chat.avatar.photo.EDIT"

    private const val IMAGE_EDITOR = "image_editor"
  }
}
