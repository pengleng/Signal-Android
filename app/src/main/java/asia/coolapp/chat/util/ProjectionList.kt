package asia.coolapp.chat.util

import java.io.Closeable

class ProjectionList(size: Int = 0) : ArrayList<Projection>(size), Closeable {
  override fun close() {
    forEach { it.release() }
  }
}
