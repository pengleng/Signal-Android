package asia.coolapp.chat.components;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.DrawableImageViewTarget;

import asia.coolapp.chat.util.concurrent.SettableFuture;

public class GlideDrawableListeningTarget extends DrawableImageViewTarget {

  private final SettableFuture<Boolean> loaded;

  public GlideDrawableListeningTarget(@NonNull ImageView view, @NonNull SettableFuture<Boolean> loaded) {
    super(view);
    this.loaded = loaded;
  }

  @Override
  protected void setResource(@Nullable Drawable resource) {
    super.setResource(resource);
    loaded.set(true);
  }

  @Override
  public void onLoadFailed(@Nullable Drawable errorDrawable) {
    super.onLoadFailed(errorDrawable);
    loaded.set(true);
  }
}
