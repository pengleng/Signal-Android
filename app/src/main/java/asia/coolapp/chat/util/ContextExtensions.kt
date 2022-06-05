package asia.coolapp.chat.util

import android.content.BroadcastReceiver
import android.content.Context

fun Context.safeUnregisterReceiver(receiver: BroadcastReceiver?) {
  if (receiver == null) {
    return
  }

  try {
    unregisterReceiver(receiver)
  } catch (e: IllegalArgumentException) {
  }
}
