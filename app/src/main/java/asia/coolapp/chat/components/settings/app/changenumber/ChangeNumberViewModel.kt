package asia.coolapp.chat.components.settings.app.changenumber

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.reactivex.rxjava3.core.Single
import org.signal.core.util.logging.Log
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.keyvalue.SignalStore
import asia.coolapp.chat.pin.KbsRepository
import asia.coolapp.chat.pin.TokenData
import asia.coolapp.chat.registration.VerifyAccountRepository
import asia.coolapp.chat.registration.VerifyAccountResponseProcessor
import asia.coolapp.chat.registration.VerifyAccountResponseWithoutKbs
import asia.coolapp.chat.registration.VerifyCodeWithRegistrationLockResponseProcessor
import asia.coolapp.chat.registration.VerifyProcessor
import asia.coolapp.chat.registration.viewmodel.BaseRegistrationViewModel
import asia.coolapp.chat.registration.viewmodel.NumberViewState
import asia.coolapp.chat.util.DefaultValueLiveData
import org.whispersystems.signalservice.api.push.PNI
import org.whispersystems.signalservice.internal.ServiceResponse
import org.whispersystems.signalservice.internal.push.VerifyAccountResponse
import java.util.Objects

private val TAG: String = Log.tag(ChangeNumberViewModel::class.java)

class ChangeNumberViewModel(
  private val localNumber: String,
  private val changeNumberRepository: ChangeNumberRepository,
  savedState: SavedStateHandle,
  password: String,
  verifyAccountRepository: VerifyAccountRepository,
  kbsRepository: KbsRepository,
) : BaseRegistrationViewModel(savedState, verifyAccountRepository, kbsRepository, password) {

  var oldNumberState: NumberViewState = NumberViewState.Builder().build()
    private set

  private val liveOldNumberState = DefaultValueLiveData(oldNumberState)
  private val liveNewNumberState = DefaultValueLiveData(number)

  init {
    try {
      val countryCode: Int = PhoneNumberUtil.getInstance()
        .parse(localNumber, null)
        .countryCode

      setOldCountry(countryCode)
      setNewCountry(countryCode)
    } catch (e: NumberParseException) {
      Log.i(TAG, "Unable to parse number for default country code")
    }
  }

  fun getLiveOldNumber(): LiveData<NumberViewState> {
    return liveOldNumberState
  }

  fun getLiveNewNumber(): LiveData<NumberViewState> {
    return liveNewNumberState
  }

  fun setOldNationalNumber(number: String) {
    oldNumberState = oldNumberState.toBuilder()
      .nationalNumber(number)
      .build()

    liveOldNumberState.value = oldNumberState
  }

  fun setOldCountry(countryCode: Int, country: String? = null) {
    oldNumberState = oldNumberState.toBuilder()
      .selectedCountryDisplayName(country)
      .countryCode(countryCode)
      .build()

    liveOldNumberState.value = oldNumberState
  }

  fun setNewNationalNumber(number: String) {
    setNationalNumber(number)

    liveNewNumberState.value = this.number
  }

  fun setNewCountry(countryCode: Int, country: String? = null) {
    onCountrySelected(country, countryCode)

    liveNewNumberState.value = this.number
  }

  fun canContinue(): ContinueStatus {
    return if (oldNumberState.e164Number == localNumber) {
      if (number.isValid) {
        ContinueStatus.CAN_CONTINUE
      } else {
        ContinueStatus.INVALID_NUMBER
      }
    } else {
      ContinueStatus.OLD_NUMBER_DOESNT_MATCH
    }
  }

  override fun verifyCodeWithoutRegistrationLock(code: String): Single<VerifyAccountResponseProcessor> {
    return super.verifyCodeWithoutRegistrationLock(code)
      .doOnSubscribe { SignalStore.misc().lockChangeNumber() }
      .flatMap(this::attemptToUnlockChangeNumber)
  }

  override fun verifyCodeAndRegisterAccountWithRegistrationLock(pin: String): Single<VerifyCodeWithRegistrationLockResponseProcessor> {
    return super.verifyCodeAndRegisterAccountWithRegistrationLock(pin)
      .doOnSubscribe { SignalStore.misc().lockChangeNumber() }
      .flatMap(this::attemptToUnlockChangeNumber)
  }

  private fun <T : VerifyProcessor> attemptToUnlockChangeNumber(processor: T): Single<T> {
    return if (processor.hasResult() || processor.isServerSentError()) {
      SignalStore.misc().unlockChangeNumber()
      Single.just(processor)
    } else {
      changeNumberRepository.whoAmI()
        .map { whoAmI ->
          if (Objects.equals(whoAmI.number, localNumber)) {
            Log.i(TAG, "Local and remote numbers match, we can unlock.")
            SignalStore.misc().unlockChangeNumber()
          }
          processor
        }
        .onErrorReturn { processor }
    }
  }

  override fun verifyAccountWithoutRegistrationLock(): Single<ServiceResponse<VerifyAccountResponse>> {
    return changeNumberRepository.changeNumber(textCodeEntered, number.e164Number)
  }

  override fun verifyAccountWithRegistrationLock(pin: String, kbsTokenData: TokenData): Single<ServiceResponse<VerifyAccountRepository.VerifyAccountWithRegistrationLockResponse>> {
    return changeNumberRepository.changeNumber(textCodeEntered, number.e164Number, pin, kbsTokenData)
  }

  @WorkerThread
  override fun onVerifySuccess(processor: VerifyAccountResponseProcessor): Single<VerifyAccountResponseProcessor> {
    return changeNumberRepository.changeLocalNumber(number.e164Number, PNI.parseOrThrow(processor.result.pni))
      .map { processor }
      .onErrorReturn { t ->
        Log.w(TAG, "Error attempting to change local number", t)
        VerifyAccountResponseWithoutKbs(ServiceResponse.forUnknownError(t))
      }
  }

  override fun onVerifySuccessWithRegistrationLock(processor: VerifyCodeWithRegistrationLockResponseProcessor, pin: String): Single<VerifyCodeWithRegistrationLockResponseProcessor> {
    return changeNumberRepository.changeLocalNumber(number.e164Number, PNI.parseOrThrow(processor.result.verifyAccountResponse.pni))
      .map { processor }
      .onErrorReturn { t ->
        Log.w(TAG, "Error attempting to change local number", t)
        VerifyCodeWithRegistrationLockResponseProcessor(ServiceResponse.forUnknownError(t), processor.token)
      }
  }

  class Factory(owner: SavedStateRegistryOwner) : AbstractSavedStateViewModelFactory(owner, null) {

    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
      val context: Application = ApplicationDependencies.getApplication()
      val localNumber: String = SignalStore.account().e164!!
      val password: String = SignalStore.account().servicePassword!!

      val viewModel = ChangeNumberViewModel(
        localNumber = localNumber,
        changeNumberRepository = ChangeNumberRepository(context),
        savedState = handle,
        password = password,
        verifyAccountRepository = VerifyAccountRepository(context),
        kbsRepository = KbsRepository()
      )

      return requireNotNull(modelClass.cast(viewModel))
    }
  }

  enum class ContinueStatus {
    CAN_CONTINUE,
    INVALID_NUMBER,
    OLD_NUMBER_DOESNT_MATCH
  }
}
