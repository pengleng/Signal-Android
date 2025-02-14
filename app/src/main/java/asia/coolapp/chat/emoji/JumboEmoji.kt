package asia.coolapp.chat.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.annotation.MainThread
import org.signal.core.util.ThreadUtil
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import asia.coolapp.chat.components.emoji.parsing.EmojiDrawInfo
import asia.coolapp.chat.emoji.protos.JumbomojiPack
import asia.coolapp.chat.jobmanager.impl.AutoDownloadEmojiConstraint
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.util.ListenableFutureTask
import asia.coolapp.chat.util.SoftHashMap
import asia.coolapp.chat.util.concurrent.SimpleTask
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

private val TAG = Log.tag(JumboEmoji::class.java)

/**
 * For Jumbo Emojis, will download, add to in-memory cache, and load from disk.
 */
object JumboEmoji {

  private val executor = ThreadUtil.trace(SignalExecutors.newCachedSingleThreadExecutor("jumbo-emoji"))

  const val MAX_JUMBOJI_COUNT = 5

  private const val JUMBOMOJI_SUPPORTED_VERSION = 5

  private val cache: MutableMap<String, Bitmap> = SoftHashMap(16)
  private val versionToFormat: MutableMap<UUID, String?> = hashMapOf()
  private val downloadedJumbos: MutableSet<String> = mutableSetOf()

  private val networkCheckThrottle: Long = TimeUnit.MINUTES.toMillis(1)
  private var lastNetworkCheck: Long = 0
  private var canDownload: Boolean = false

  @Volatile
  private var currentVersion: Int = -1

  @JvmStatic
  @MainThread
  fun updateCurrentVersion(context: Context) {
    SignalExecutors.BOUNDED.execute {
      val version: EmojiFiles.Version = EmojiFiles.Version.readVersion(context, true) ?: return@execute

      if (EmojiFiles.getLatestEmojiData(context, version)?.format != null) {
        currentVersion = version.version
        ThreadUtil.runOnMain { downloadedJumbos.addAll(SignalStore.emojiValues().getJumboEmojiSheets(version.version)) }
      }
    }
  }

  @JvmStatic
  @MainThread
  fun canDownloadJumbo(context: Context): Boolean {
    val now = SystemClock.elapsedRealtime()
    if (now - networkCheckThrottle > lastNetworkCheck) {
      canDownload = AutoDownloadEmojiConstraint.canAutoDownloadJumboEmoji(context)
      lastNetworkCheck = now
    }
    return canDownload && currentVersion >= JUMBOMOJI_SUPPORTED_VERSION
  }

  @JvmStatic
  @MainThread
  fun hasJumboEmoji(drawInfo: EmojiDrawInfo): Boolean {
    return downloadedJumbos.contains(drawInfo.jumboSheet)
  }

  @Suppress("FoldInitializerAndIfToElvis")
  @JvmStatic
  @MainThread
  fun loadJumboEmoji(context: Context, drawInfo: EmojiDrawInfo): LoadResult {
    val applicationContext: Context = context.applicationContext

    val archiveName = "jumbo/${drawInfo.jumboSheet}.proto"
    val emojiName: String = drawInfo.rawEmoji!!
    val bitmap: Bitmap? = cache[emojiName]

    if (bitmap != null) {
      return LoadResult.Immediate(bitmap)
    }

    val newTask = ListenableFutureTask<Bitmap> {
      val version: EmojiFiles.Version? = EmojiFiles.Version.readVersion(applicationContext, true)
      if (version == null) {
        throw NoVersionData()
      }

      val format: String? = versionToFormat.getOrPut(version.uuid) {
        EmojiFiles.getLatestEmojiData(context, version)?.format
      }

      if (format == null) {
        throw NoVersionData()
      }

      currentVersion = version.version

      var jumbos: EmojiFiles.JumboCollection = EmojiFiles.JumboCollection.read(applicationContext, version)

      val uuid = jumbos.getUUIDForName(emojiName)

      if (uuid == null) {
        if (!AutoDownloadEmojiConstraint.canAutoDownloadJumboEmoji(applicationContext)) {
          throw CannotAutoDownload()
        }

        Log.i(TAG, "No file for emoji, downloading jumbo")
        EmojiDownloader.streamFileFromRemote(version, version.density, archiveName) { stream ->
          stream.use { remote ->
            val jumbomojiPack = JumbomojiPack.parseFrom(remote)

            jumbomojiPack.itemsList.forEach { jumbo ->
              val emojiNameEntry = EmojiFiles.Name(jumbo.name, UUID.randomUUID())
              val outputStream = EmojiFiles.openForWriting(applicationContext, version, emojiNameEntry.uuid)

              outputStream.use { jumbo.image.writeTo(it) }

              jumbos = EmojiFiles.JumboCollection.append(applicationContext, jumbos, emojiNameEntry)
            }
          }
        }

        SignalStore.emojiValues().addJumboEmojiSheet(version.version, drawInfo.jumboSheet)
      }

      EmojiFiles.openForReadingJumbo(applicationContext, version, jumbos, emojiName).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options()) }
    }

    SimpleTask.run(executor, newTask::run) {
      try {
        val newBitmap: Bitmap? = newTask.get()
        if (newBitmap == null) {
          Log.w(TAG, "Failed to load jumbo emoji")
        } else {
          cache[emojiName] = newBitmap
          downloadedJumbos.add(drawInfo.jumboSheet!!)
        }
      } catch (e: ExecutionException) {
        // do nothing, emoji provider will log the exception
      }
    }

    return LoadResult.Async(newTask)
  }

  class NoVersionData : Throwable()
  class CannotAutoDownload : IOException()

  sealed class LoadResult {
    data class Immediate(val bitmap: Bitmap) : LoadResult()
    data class Async(val task: ListenableFutureTask<Bitmap>) : LoadResult()
  }
}
