package asia.coolapp.chat.conversation.drafts

import android.content.Context
import android.net.Uri
import org.signal.core.util.concurrent.SignalExecutors
import asia.coolapp.chat.database.DraftDatabase
import asia.coolapp.chat.providers.BlobProvider

class DraftRepository(private val context: Context) {
  fun deleteVoiceNoteDraft(draft: DraftDatabase.Draft) {
    deleteBlob(Uri.parse(draft.value).buildUpon().clearQuery().build())
  }

  fun deleteBlob(uri: Uri) {
    SignalExecutors.BOUNDED.execute {
      BlobProvider.getInstance().delete(context, uri)
    }
  }
}
