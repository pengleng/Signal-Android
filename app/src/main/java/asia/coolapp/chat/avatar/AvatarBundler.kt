package asia.coolapp.chat.avatar

import android.os.Bundle

/**
 * Utility class which encapsulates reading and writing Avatar objects to and from Bundles.
 */
object AvatarBundler {

  private const val TEXT = "asia.coolapp.chat.avatar.TEXT"
  private const val COLOR = "asia.coolapp.chat.avatar.COLOR"
  private const val URI = "asia.coolapp.chat.avatar.URI"
  private const val KEY = "asia.coolapp.chat.avatar.KEY"
  private const val DATABASE_ID = "asia.coolapp.chat.avatar.DATABASE_ID"
  private const val SIZE = "asia.coolapp.chat.avatar.SIZE"

  fun bundleText(text: Avatar.Text): Bundle = Bundle().apply {
    putString(TEXT, text.text)
    putString(COLOR, text.color.code)
    putDatabaseId(DATABASE_ID, text.databaseId)
  }

  fun extractText(bundle: Bundle): Avatar.Text = Avatar.Text(
    text = requireNotNull(bundle.getString(TEXT)),
    color = Avatars.colorMap[bundle.getString(COLOR)] ?: throw IllegalStateException(),
    databaseId = bundle.getDatabaseId()
  )

  fun bundlePhoto(photo: Avatar.Photo): Bundle = Bundle().apply {
    putParcelable(URI, photo.uri)
    putLong(SIZE, photo.size)
    putDatabaseId(DATABASE_ID, photo.databaseId)
  }

  fun extractPhoto(bundle: Bundle): Avatar.Photo = Avatar.Photo(
    uri = requireNotNull(bundle.getParcelable(URI)),
    size = bundle.getLong(SIZE),
    databaseId = bundle.getDatabaseId()
  )

  fun bundleVector(vector: Avatar.Vector): Bundle = Bundle().apply {
    putString(KEY, vector.key)
    putString(COLOR, vector.color.code)
    putDatabaseId(DATABASE_ID, vector.databaseId)
  }

  fun extractVector(bundle: Bundle): Avatar.Vector = Avatar.Vector(
    key = requireNotNull(bundle.getString(KEY)),
    color = Avatars.colorMap[bundle.getString(COLOR)] ?: throw IllegalStateException(),
    databaseId = bundle.getDatabaseId()
  )

  private fun Bundle.getDatabaseId(): Avatar.DatabaseId {
    val id = getLong(DATABASE_ID, -1L)

    return if (id == -1L) {
      Avatar.DatabaseId.NotSet
    } else {
      Avatar.DatabaseId.Saved(id)
    }
  }

  private fun Bundle.putDatabaseId(key: String, databaseId: Avatar.DatabaseId) {
    if (databaseId is Avatar.DatabaseId.Saved) {
      putLong(key, databaseId.id)
    }
  }
}
