package asia.coolapp.chat.mediasend.v2.review

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import asia.coolapp.chat.util.ViewUtil
import asia.coolapp.chat.util.visible

object MediaReviewAnimatorController {

  fun getSlideInAnimator(view: View): Animator {
    return ObjectAnimator.ofFloat(view, "translationY", view.translationY, 0f)
  }

  fun getSlideOutAnimator(view: View): Animator {
    return ObjectAnimator.ofFloat(view, "translationY", view.translationX, ViewUtil.dpToPx(48).toFloat())
  }

  fun getFadeInAnimator(view: View, isEnabled: Boolean = true): Animator {
    view.visible = true
    view.isEnabled = isEnabled

    return ObjectAnimator.ofFloat(view, "alpha", view.alpha, 1f)
  }

  fun getFadeOutAnimator(view: View, isEnabled: Boolean = false): Animator {
    view.isEnabled = isEnabled

    val animator = ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f)

    animator.doOnEnd { view.visible = false }

    return animator
  }
}
