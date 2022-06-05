package asia.coolapp.chat.conversation.colors

import android.view.ViewGroup
import asia.coolapp.chat.util.ProjectionList

/**
 * Denotes that a class can be colorized. The class is responsible for
 * generating its own projection.
 */
interface Colorizable {
  fun getColorizerProjections(coordinateRoot: ViewGroup): ProjectionList
}
