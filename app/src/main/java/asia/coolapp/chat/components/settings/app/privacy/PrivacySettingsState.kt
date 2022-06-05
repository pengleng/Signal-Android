package asia.coolapp.chat.components.settings.app.privacy

import asia.coolapp.chat.database.model.DistributionListPartialRecord
import asia.coolapp.chat.keyvalue.PhoneNumberPrivacyValues

data class PrivacySettingsState(
  val blockedCount: Int,
  val seeMyPhoneNumber: PhoneNumberPrivacyValues.PhoneNumberSharingMode,
  val findMeByPhoneNumber: PhoneNumberPrivacyValues.PhoneNumberListingMode,
  val readReceipts: Boolean,
  val typingIndicators: Boolean,
  val screenLock: Boolean,
  val screenLockActivityTimeout: Long,
  val screenSecurity: Boolean,
  val incognitoKeyboard: Boolean,
  val isObsoletePasswordEnabled: Boolean,
  val isObsoletePasswordTimeoutEnabled: Boolean,
  val obsoletePasswordTimeout: Int,
  val universalExpireTimer: Int,
  val privateStories: List<DistributionListPartialRecord>,
  val isStoriesEnabled: Boolean
)
