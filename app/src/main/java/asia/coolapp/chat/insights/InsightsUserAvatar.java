package asia.coolapp.chat.insights;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import asia.coolapp.chat.contacts.avatars.FallbackContactPhoto;
import asia.coolapp.chat.contacts.avatars.ProfileContactPhoto;
import asia.coolapp.chat.conversation.colors.AvatarColor;
import asia.coolapp.chat.mms.GlideApp;

class InsightsUserAvatar {
  private final ProfileContactPhoto  profileContactPhoto;
  private final AvatarColor          fallbackColor;
  private final FallbackContactPhoto fallbackContactPhoto;

  InsightsUserAvatar(@NonNull ProfileContactPhoto profileContactPhoto, @NonNull AvatarColor fallbackColor, @NonNull FallbackContactPhoto fallbackContactPhoto) {
    this.profileContactPhoto  = profileContactPhoto;
    this.fallbackColor        = fallbackColor;
    this.fallbackContactPhoto = fallbackContactPhoto;
  }

  private Drawable fallbackDrawable(@NonNull Context context) {
    return fallbackContactPhoto.asDrawable(context, fallbackColor);
  }

  void load(ImageView into) {
    GlideApp.with(into)
            .load(profileContactPhoto)
            .error(fallbackDrawable(into.getContext()))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(into);
  }
}
