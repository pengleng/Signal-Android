package asia.coolapp.chat.components.settings.app.changenumber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.signal.core.util.logging.Log
import asia.coolapp.chat.MainActivity
import asia.coolapp.chat.PassphraseRequiredActivity
import asia.coolapp.chat.R
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.logsubmit.SubmitDebugLogActivity
import asia.coolapp.chat.phonenumbers.PhoneNumberFormatter
import asia.coolapp.chat.util.DynamicNoActionBarTheme
import asia.coolapp.chat.util.DynamicTheme
import asia.coolapp.chat.util.LifecycleDisposable
import org.whispersystems.signalservice.api.push.PNI
import java.util.Objects

private val TAG: String = Log.tag(ChangeNumberLockActivity::class.java)

/**
 * A captive activity that can determine if an interrupted/erred change number request
 * caused a disparity between the server and our locally stored number.
 */
class ChangeNumberLockActivity : PassphraseRequiredActivity() {

  private val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()
  private val disposables: LifecycleDisposable = LifecycleDisposable()
  private lateinit var changeNumberRepository: ChangeNumberRepository

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    dynamicTheme.onCreate(this)
    disposables.bindTo(lifecycle)

    setContentView(R.layout.activity_change_number_lock)

    changeNumberRepository = ChangeNumberRepository(applicationContext)
    checkWhoAmI()
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  override fun onBackPressed() = Unit

  private fun checkWhoAmI() {
    disposables.add(
      changeNumberRepository.whoAmI()
        .flatMap { whoAmI ->
          if (Objects.equals(whoAmI.number, SignalStore.account().e164)) {
            Log.i(TAG, "Local and remote numbers match, nothing needs to be done.")
            Single.just(false)
          } else {
            Log.i(TAG, "Local (${SignalStore.account().e164}) and remote (${whoAmI.number}) numbers do not match, updating local.")
            changeNumberRepository.changeLocalNumber(whoAmI.number, PNI.parseOrThrow(whoAmI.pni))
              .map { true }
          }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(onSuccess = { onChangeStatusConfirmed() }, onError = this::onFailedToGetChangeNumberStatus)
    )
  }

  private fun onChangeStatusConfirmed() {
    SignalStore.misc().unlockChangeNumber()

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.ChangeNumberLockActivity__change_status_confirmed)
      .setMessage(getString(R.string.ChangeNumberLockActivity__your_number_has_been_confirmed_as_s, PhoneNumberFormatter.prettyPrint(SignalStore.account().e164!!)))
      .setPositiveButton(android.R.string.ok) { _, _ ->
        startActivity(MainActivity.clearTop(this))
        finish()
      }
      .setCancelable(false)
      .show()
  }

  private fun onFailedToGetChangeNumberStatus(error: Throwable) {
    Log.w(TAG, "Unable to determine status of change number", error)

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.ChangeNumberLockActivity__change_status_unconfirmed)
      .setMessage(getString(R.string.ChangeNumberLockActivity__we_could_not_determine_the_status_of_your_change_number_request, error.javaClass.simpleName))
      .setPositiveButton(R.string.ChangeNumberLockActivity__retry) { _, _ -> checkWhoAmI() }
      .setNegativeButton(R.string.ChangeNumberLockActivity__leave) { _, _ -> finish() }
      .setNeutralButton(R.string.ChangeNumberLockActivity__submit_debug_log) { _, _ ->
        startActivity(Intent(this, SubmitDebugLogActivity::class.java))
        finish()
      }
      .setCancelable(false)
      .show()
  }

  companion object {
    @JvmStatic
    fun createIntent(context: Context): Intent {
      return Intent(context, ChangeNumberLockActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    }
  }
}
