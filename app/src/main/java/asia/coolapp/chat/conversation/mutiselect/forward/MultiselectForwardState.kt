package asia.coolapp.chat.conversation.mutiselect.forward

import asia.coolapp.chat.contacts.paged.ContactSearchKey
import asia.coolapp.chat.database.model.IdentityRecord

data class MultiselectForwardState(
  val stage: Stage = Stage.Selection
) {
  sealed class Stage {
    object Selection : Stage()
    object FirstConfirmation : Stage()
    object LoadingIdentities : Stage()
    data class SafetyConfirmation(val identities: List<IdentityRecord>) : Stage()
    object SendPending : Stage()
    object SomeFailed : Stage()
    object AllFailed : Stage()
    object Success : Stage()
    data class SelectionConfirmed(val selectedContacts: Set<ContactSearchKey>) : Stage()
  }
}
