package asia.coolapp.chat.conversation

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import asia.coolapp.chat.PassphraseRequiredActivity
import asia.coolapp.chat.R
import asia.coolapp.chat.components.HidingLinearLayout
import asia.coolapp.chat.components.reminder.ReminderView
import asia.coolapp.chat.components.settings.app.subscription.DonationPaymentComponent
import asia.coolapp.chat.components.settings.app.subscription.DonationPaymentRepository
import asia.coolapp.chat.recipients.Recipient
import asia.coolapp.chat.util.DynamicNoActionBarTheme
import asia.coolapp.chat.util.DynamicTheme
import asia.coolapp.chat.util.concurrent.ListenableFuture
import asia.coolapp.chat.util.views.Stub

open class ConversationActivity : PassphraseRequiredActivity(), ConversationParentFragment.Callback, DonationPaymentComponent {

  private lateinit var fragment: ConversationParentFragment

  private val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()
  override fun onPreCreate() {
    dynamicTheme.onCreate(this)
  }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    setContentView(R.layout.conversation_parent_fragment_container)

    fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as ConversationParentFragment
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    fragment.onNewIntent(intent)
  }

  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    return fragment.dispatchTouchEvent(ev) || super.dispatchTouchEvent(ev)
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  override fun onInitializeToolbar(toolbar: Toolbar) {
    toolbar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_left_24)
    toolbar.setNavigationOnClickListener { finish() }
  }

  fun saveDraft(): ListenableFuture<Long> {
    return fragment.saveDraft()
  }

  fun getRecipient(): Recipient {
    return fragment.recipient
  }

  fun getTitleView(): View {
    return fragment.titleView
  }

  fun getComposeText(): View {
    return fragment.composeText
  }

  fun getQuickAttachmentToggle(): HidingLinearLayout {
    return fragment.quickAttachmentToggle
  }

  fun getReminderView(): Stub<ReminderView> {
    return fragment.reminderView
  }

  override val donationPaymentRepository: DonationPaymentRepository by lazy { DonationPaymentRepository(this) }
  override val googlePayResultPublisher: Subject<DonationPaymentComponent.GooglePayResult> = PublishSubject.create()
}
