package asia.coolapp.chat.avatar

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.mediasend.Media
import asia.coolapp.chat.mms.PartAuthority
import asia.coolapp.chat.util.MediaUtil
import asia.coolapp.chat.util.storage.FileStorage
import java.io.InputStream

object AvatarPickerStorage {

  private const val DIRECTORY = "avatar_picker"
  private const val FILENAME_BASE = "avatar"

  @JvmStatic
  fun read(context: Context, fileName: String) = FileStorage.read(context, DIRECTORY, fileName)

  fun save(context: Context, media: Media): Uri {
    val fileName = FileStorage.save(context, PartAuthority.getAttachmentStream(context, media.uri), DIRECTORY, FILENAME_BASE, MediaUtil.getExtension(context, media.uri) ?: "")

    return PartAuthority.getAvatarPickerUri(fileName)
  }

  fun save(context: Context, inputStream: InputStream): Uri {
    val fileName = FileStorage.save(context, inputStream, DIRECTORY, FILENAME_BASE, MimeTypeMap.getSingleton().getExtensionFromMimeType(MediaUtil.IMAGE_JPEG) ?: "")

    return PartAuthority.getAvatarPickerUri(fileName)
  }

  @JvmStatic
  fun cleanOrphans(context: Context) {
    val avatarFiles = FileStorage.getAllFiles(context, DIRECTORY, FILENAME_BASE)
    val database = SignalDatabase.avatarPicker
    val photoAvatars = database
      .getAllAvatars()
      .filterIsInstance<Avatar.Photo>()

    val inDatabaseFileNames = photoAvatars.map { PartAuthority.getAvatarPickerFilename(it.uri) }
    val onDiskFileNames = avatarFiles.map { it.name }

    val inDatabaseButNotOnDisk = inDatabaseFileNames - onDiskFileNames
    val onDiskButNotInDatabase = onDiskFileNames - inDatabaseFileNames

    avatarFiles
      .filter { onDiskButNotInDatabase.contains(it.name) }
      .forEach { it.delete() }

    photoAvatars
      .filter { inDatabaseButNotOnDisk.contains(PartAuthority.getAvatarPickerFilename(it.uri)) }
      .forEach { database.deleteAvatar(it) }
  }
}
