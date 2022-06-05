package asia.coolapp.chat.jobs

import org.signal.core.util.logging.Log
import asia.coolapp.chat.crypto.UnidentifiedAccessUtil
import asia.coolapp.chat.dependencies.ApplicationDependencies
import asia.coolapp.chat.jobmanager.Data
import asia.coolapp.chat.jobmanager.Job
import asia.coolapp.chat.jobmanager.impl.NetworkConstraint
import asia.coolapp.chat.net.NotPushRegisteredException
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.TextSecurePreferences
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException

/**
 * Sends a sync message to linked devices to notify them to refresh subscription status.
 */
class MultiDeviceSubscriptionSyncRequestJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    const val KEY = "MultiDeviceSubscriptionSyncRequestJob"

    private val TAG = Log.tag(MultiDeviceSubscriptionSyncRequestJob::class.java)

    @JvmStatic
    fun enqueue() {
      val job = MultiDeviceSubscriptionSyncRequestJob(
        Parameters.Builder()
          .setQueue("MultiDeviceSubscriptionSyncRequestJob")
          .setMaxInstancesForFactory(2)
          .addConstraint(NetworkConstraint.KEY)
          .setMaxAttempts(10)
          .build()
      )

      ApplicationDependencies.getJobManager().add(job)
    }
  }

  override fun serialize(): Data = Data.EMPTY

  override fun getFactoryKey(): String = KEY

  override fun onFailure() {
    Log.w(TAG, "Did not succeed!")
  }

  override fun onRun() {
    if (!Recipient.self().isRegistered) {
      throw NotPushRegisteredException()
    }

    if (!TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Not multi device, aborting...")
      return
    }

    val messageSender = ApplicationDependencies.getSignalServiceMessageSender()

    messageSender.sendSyncMessage(
      SignalServiceSyncMessage.forFetchLatest(SignalServiceSyncMessage.FetchType.SUBSCRIPTION_STATUS),
      UnidentifiedAccessUtil.getAccessForSync(context)
    )
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is PushNetworkException && e !is ServerRejectedException
  }

  class Factory : Job.Factory<MultiDeviceSubscriptionSyncRequestJob> {
    override fun create(parameters: Parameters, data: Data): MultiDeviceSubscriptionSyncRequestJob {
      return MultiDeviceSubscriptionSyncRequestJob(parameters)
    }
  }
}
