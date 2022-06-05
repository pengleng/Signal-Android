package asia.coolapp.chat

import android.content.ContentValues
import android.os.Build
import leakcanary.LeakCanary
import org.signal.spinner.Spinner
import org.signal.spinner.Spinner.DatabaseConfig
import asia.coolapp.chat.database.DatabaseMonitor
import asia.coolapp.chat.database.GV2Transformer
import asia.coolapp.chat.database.GV2UpdateTransformer
import asia.coolapp.chat.database.IsStoryTransformer
import asia.coolapp.chat.database.JobDatabase
import asia.coolapp.chat.database.KeyValueDatabase
import asia.coolapp.chat.database.LocalMetricsDatabase
import asia.coolapp.chat.database.LogDatabase
import asia.coolapp.chat.database.MegaphoneDatabase
import asia.coolapp.chat.database.MessageBitmaskColumnTransformer
import asia.coolapp.chat.database.QueryMonitor
import asia.coolapp.chat.database.SignalDatabase
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.AppSignatureUtil
import shark.AndroidReferenceMatchers

class SpinnerApplicationContext : ApplicationContext() {
  override fun onCreate() {
    super.onCreate()

    Spinner.init(
      this,
      mapOf(
        "Device" to "${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
        "Package" to "$packageName (${AppSignatureUtil.getAppSignature(this).orElse("Unknown")})",
        "App Version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.CANONICAL_VERSION_CODE}, ${BuildConfig.GIT_HASH})",
        "Profile Name" to (if (SignalStore.account().isRegistered) Recipient.self().profileName.toString() else "none"),
        "E164" to (SignalStore.account().e164 ?: "none"),
        "ACI" to (SignalStore.account().aci?.toString() ?: "none"),
        "PNI" to (SignalStore.account().pni?.toString() ?: "none")
      ),
      linkedMapOf(
        "signal" to DatabaseConfig(
          db = SignalDatabase.rawDatabase,
          columnTransformers = listOf(MessageBitmaskColumnTransformer, GV2Transformer, GV2UpdateTransformer, IsStoryTransformer)
        ),
        "jobmanager" to DatabaseConfig(db = JobDatabase.getInstance(this).sqlCipherDatabase),
        "keyvalue" to DatabaseConfig(db = KeyValueDatabase.getInstance(this).sqlCipherDatabase),
        "megaphones" to DatabaseConfig(db = MegaphoneDatabase.getInstance(this).sqlCipherDatabase),
        "localmetrics" to DatabaseConfig(db = LocalMetricsDatabase.getInstance(this).sqlCipherDatabase),
        "logs" to DatabaseConfig(db = LogDatabase.getInstance(this).sqlCipherDatabase),
      )
    )

    DatabaseMonitor.initialize(object : QueryMonitor {
      override fun onSql(sql: String, args: Array<Any>?) {
        Spinner.onSql("signal", sql, args)
      }

      override fun onQuery(distinct: Boolean, table: String, projection: Array<String>?, selection: String?, args: Array<Any>?, groupBy: String?, having: String?, orderBy: String?, limit: String?) {
        Spinner.onQuery("signal", distinct, table, projection, selection, args, groupBy, having, orderBy, limit)
      }

      override fun onDelete(table: String, selection: String?, args: Array<Any>?) {
        Spinner.onDelete("signal", table, selection, args)
      }

      override fun onUpdate(table: String, values: ContentValues, selection: String?, args: Array<Any>?) {
        Spinner.onUpdate("signal", table, values, selection, args)
      }
    })

    LeakCanary.config = LeakCanary.config.copy(
      referenceMatchers = AndroidReferenceMatchers.appDefaults +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "android.service.media.MediaBrowserService\$ServiceBinder",
          fieldName = "this\$0"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "androidx.media.MediaBrowserServiceCompat\$MediaBrowserServiceImplApi26\$MediaBrowserServiceApi26",
          fieldName = "mBase"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "android.support.v4.media.MediaBrowserCompat",
          fieldName = "mImpl"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "android.support.v4.media.session.MediaControllerCompat",
          fieldName = "mToken"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "android.support.v4.media.session.MediaControllerCompat",
          fieldName = "mImpl"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "asia.coolapp.chat.components.voice.VoiceNotePlaybackService",
          fieldName = "mApplication"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "asia.coolapp.chat.service.GenericForegroundService\$LocalBinder",
          fieldName = "this\$0"
        ) +
        AndroidReferenceMatchers.ignoredInstanceField(
          className = "asia.coolapp.chat.contacts.ContactsSyncAdapter",
          fieldName = "mContext"
        )
    )
  }
}
