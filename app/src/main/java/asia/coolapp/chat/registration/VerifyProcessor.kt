package asia.coolapp.chat.registration

interface VerifyProcessor {
  fun hasResult(): Boolean
  fun isServerSentError(): Boolean
}
