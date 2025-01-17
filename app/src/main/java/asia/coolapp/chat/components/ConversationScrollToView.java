package asia.coolapp.chat.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import asia.coolapp.chat.R;

public final class ConversationScrollToView extends FrameLayout {

  private final TextView  unreadCount;
  private final ImageView scrollButton;

  public ConversationScrollToView(@NonNull Context context) {
    this(context, null);
  }

  public ConversationScrollToView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ConversationScrollToView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    inflate(context, R.layout.conversation_scroll_to, this);

    unreadCount  = findViewById(R.id.conversation_scroll_to_count);
    scrollButton = findViewById(R.id.conversation_scroll_to_button);

    if (attrs != null) {
      TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ConversationScrollToView);
      int        srcId = array.getResourceId(R.styleable.ConversationScrollToView_cstv_scroll_button_src, 0);

      scrollButton.setImageResource(srcId);

      array.recycle();
    }
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener l) {
    scrollButton.setOnClickListener(l);
  }

  public void setUnreadCount(int unreadCount) {
    this.unreadCount.setText(formatUnreadCount(unreadCount));
    this.unreadCount.setVisibility(unreadCount > 0 ? VISIBLE : GONE);
  }

  private @NonNull CharSequence formatUnreadCount(int unreadCount) {
    return unreadCount > 999 ? "999+" : String.valueOf(unreadCount);
  }
}
