package asia.coolapp.chat.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;

import asia.coolapp.chat.R;
import asia.coolapp.chat.mms.GlideRequests;
import asia.coolapp.chat.mms.Slide;
import asia.coolapp.chat.mms.SlideClickListener;
import asia.coolapp.chat.mms.SlidesClickedListener;

public class BorderlessImageView extends FrameLayout {

  private ThumbnailView image;
  private View          missingShade;

  public BorderlessImageView(@NonNull Context context) {
    super(context);
    init();
  }

  public BorderlessImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.sticker_view, this);

    this.image        = findViewById(R.id.sticker_thumbnail);
    this.missingShade = findViewById(R.id.sticker_missing_shade);
  }

  @Override
  public void setFocusable(boolean focusable) {
    image.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    image.setClickable(clickable);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener l) {
    image.setOnLongClickListener(l);
  }

  public void setSlide(@NonNull GlideRequests glideRequests, @NonNull Slide slide) {
    boolean showControls = slide.asAttachment().getUri() == null;

    if (slide.hasSticker()) {
      image.setFit(new CenterInside());
      image.setImageResource(glideRequests, slide, showControls, false);
    } else {
      image.setFit(new CenterCrop());
      image.setImageResource(glideRequests, slide, showControls, false, slide.asAttachment().getWidth(), slide.asAttachment().getHeight());
    }

    missingShade.setVisibility(showControls ? View.VISIBLE : View.GONE);
  }

  public void setThumbnailClickListener(@NonNull SlideClickListener listener) {
    image.setThumbnailClickListener(listener);
  }

  public void setDownloadClickListener(@NonNull SlidesClickedListener listener) {
    image.setDownloadClickListener(listener);
  }
}
