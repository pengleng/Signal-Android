package asia.coolapp.chat.megaphone;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;

import asia.coolapp.chat.R;

public class BasicMegaphoneView extends FrameLayout {

  private LottieAnimationView image;
  private TextView            titleText;
  private TextView            bodyText;
  private Button              actionButton;
  private Button              secondaryButton;

  private Megaphone                 megaphone;
  private MegaphoneActionController megaphoneListener;

  public BasicMegaphoneView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public BasicMegaphoneView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(@NonNull Context context) {
    inflate(context, R.layout.basic_megaphone_view, this);

    this.image           = findViewById(R.id.basic_megaphone_image);
    this.titleText       = findViewById(R.id.basic_megaphone_title);
    this.bodyText        = findViewById(R.id.basic_megaphone_body);
    this.actionButton    = findViewById(R.id.basic_megaphone_action);
    this.secondaryButton = findViewById(R.id.basic_megaphone_secondary);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (megaphone != null && megaphoneListener != null && megaphone.getOnVisibleListener() != null) {
      megaphone.getOnVisibleListener().onEvent(megaphone, megaphoneListener);
    }
  }

  public void present(@NonNull Megaphone megaphone, @NonNull MegaphoneActionController megaphoneListener) {
    this.megaphone         = megaphone;
    this.megaphoneListener = megaphoneListener;

    if (megaphone.getImageRes() != 0) {
      image.setVisibility(VISIBLE);
      image.setImageResource(megaphone.getImageRes());
    } else if (megaphone.getImageRequest() != null) {
      image.setVisibility(VISIBLE);
      megaphone.getImageRequest().into(image);
    } else if (megaphone.getLottieRes() != 0) {
      image.setVisibility(VISIBLE);
      image.setAnimation(megaphone.getLottieRes());
      image.playAnimation();
    } else {
      image.setVisibility(GONE);
    }

    if (megaphone.getTitle() != 0) {
      titleText.setVisibility(VISIBLE);
      titleText.setText(megaphone.getTitle());
    } else {
      titleText.setVisibility(GONE);
    }

    if (megaphone.getBody() != 0) {
      bodyText.setVisibility(VISIBLE);
      bodyText.setText(megaphone.getBody());
    } else {
      bodyText.setVisibility(GONE);
    }

    if (megaphone.hasButton()) {
      actionButton.setVisibility(VISIBLE);
      actionButton.setText(megaphone.getButtonText());
      actionButton.setOnClickListener(v -> {
        if (megaphone.getButtonClickListener() != null) {
          megaphone.getButtonClickListener().onEvent(megaphone, megaphoneListener);
        }
      });
    } else {
      actionButton.setVisibility(GONE);
    }

    if (megaphone.canSnooze() || megaphone.hasSecondaryButton()) {
      secondaryButton.setVisibility(VISIBLE);

      if (megaphone.canSnooze()) {
        secondaryButton.setOnClickListener(v -> {
          megaphoneListener.onMegaphoneSnooze(megaphone.getEvent());

          if (megaphone.getSnoozeListener() != null) {
            megaphone.getSnoozeListener().onEvent(megaphone, megaphoneListener);
          }
        });
      } else {
        secondaryButton.setText(megaphone.getSecondaryButtonText());
        secondaryButton.setOnClickListener(v -> {
          if (megaphone.getSecondaryButtonClickListener() != null) {
            megaphone.getSecondaryButtonClickListener().onEvent(megaphone, megaphoneListener);
          }
        });
      }
    } else {
      secondaryButton.setVisibility(GONE);
    }
  }
}
