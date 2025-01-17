package asia.coolapp.chat.components.settings.app.subscription.errors

/**
 * Error states that can occur if we detect that a user's subscription has been cancelled and the manual
 * cancellation flag is not set.
 */
enum class UnexpectedSubscriptionCancellation(val status: String) {
  PAST_DUE("past_due"),
  CANCELED("canceled"),
  UNPAID("unpaid"),
  INACTIVE("user-was-inactive");

  companion object {
    @JvmStatic
    fun fromStatus(status: String?): UnexpectedSubscriptionCancellation? {
      return values().firstOrNull { it.status == status }
    }
  }
}
