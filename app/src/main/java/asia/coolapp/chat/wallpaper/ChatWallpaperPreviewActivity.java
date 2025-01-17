package asia.coolapp.chat.wallpaper;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.annimon.stream.Stream;

import asia.coolapp.chat.PassphraseRequiredActivity;
import asia.coolapp.chat.R;
import asia.coolapp.chat.conversation.colors.ChatColors;
import asia.coolapp.chat.conversation.colors.ColorizerView;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.DynamicNoActionBarTheme;
import asia.coolapp.chat.util.DynamicTheme;
import asia.coolapp.chat.util.FullscreenHelper;
import asia.coolapp.chat.util.Projection;
import asia.coolapp.chat.util.ViewUtil;
import asia.coolapp.chat.util.WindowUtil;
import asia.coolapp.chat.util.adapter.mapping.MappingModel;

import java.util.Collections;
import java.util.Objects;

public class ChatWallpaperPreviewActivity extends PassphraseRequiredActivity {

  public  static final String EXTRA_CHAT_WALLPAPER   = "extra.chat.wallpaper";
  private static final String EXTRA_DIM_IN_DARK_MODE = "extra.dim.in.dark.mode";
  private static final String EXTRA_RECIPIENT_ID     = "extra.recipient.id";

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  private ChatWallpaperPreviewAdapter adapter;
  private ColorizerView               colorizerView;
  private View                        bubble2;
  private OnPageChanged               onPageChanged;
  private ViewPager2                  viewPager;

  public static @NonNull Intent create(@NonNull Context context, @NonNull ChatWallpaper selection, @NonNull RecipientId recipientId, boolean dimInDarkMode) {
    Intent intent = new Intent(context, ChatWallpaperPreviewActivity.class);

    intent.putExtra(EXTRA_CHAT_WALLPAPER, selection);
    intent.putExtra(EXTRA_DIM_IN_DARK_MODE, dimInDarkMode);
    intent.putExtra(EXTRA_RECIPIENT_ID, recipientId);

    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    dynamicTheme.onCreate(this);

    setContentView(R.layout.chat_wallpaper_preview_activity);

    adapter       = new ChatWallpaperPreviewAdapter();
    colorizerView = findViewById(R.id.colorizer);
    bubble2       = findViewById(R.id.preview_bubble_2);
    viewPager     = findViewById(R.id.preview_pager);

    View                             submit        = findViewById(R.id.preview_set_wallpaper);
    ChatWallpaperRepository          repository    = new ChatWallpaperRepository();
    ChatWallpaper                    selected      = getIntent().getParcelableExtra(EXTRA_CHAT_WALLPAPER);
    boolean                          dim           = getIntent().getBooleanExtra(EXTRA_DIM_IN_DARK_MODE, false);
    Toolbar                          toolbar       = findViewById(R.id.toolbar);
    TextView                         bubble2Text   = findViewById(R.id.preview_bubble_2_text);

    toolbar.setNavigationOnClickListener(unused -> {
      finish();
    });

    viewPager.setAdapter(adapter);

    adapter.submitList(Collections.singletonList(new ChatWallpaperSelectionMappingModel(selected)));
    repository.getAllWallpaper(wallpapers -> adapter.submitList(Stream.of(wallpapers)
                                                                      .map(wallpaper -> ChatWallpaperFactory.updateWithDimming(wallpaper, dim ? ChatWallpaper.FIXED_DIM_LEVEL_FOR_DARK_THEME : 0f))
                                                                      .<MappingModel<?>>map(ChatWallpaperSelectionMappingModel::new)
                                                                      .toList()));

    submit.setOnClickListener(unused -> {
      ChatWallpaperSelectionMappingModel model = (ChatWallpaperSelectionMappingModel) adapter.getCurrentList().get(viewPager.getCurrentItem());

      setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHAT_WALLPAPER, model.getWallpaper()));
      finish();
    });

    RecipientId recipientId = getIntent().getParcelableExtra(EXTRA_RECIPIENT_ID);

    final ChatColors chatColors;
    if (recipientId != null && Recipient.live(recipientId).get().hasOwnChatColors()) {
      Recipient recipient = Recipient.live(recipientId).get();
      bubble2Text.setText(getString(R.string.ChatWallpaperPreviewActivity__set_wallpaper_for_s, recipient.getDisplayName(this)));
      chatColors = recipient.getChatColors();
      bubble2.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
        updateChatColors(chatColors);
      });
    } else if (SignalStore.chatColorsValues().hasChatColors()) {
      chatColors = Objects.requireNonNull(SignalStore.chatColorsValues().getChatColors());
      bubble2.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
        updateChatColors(chatColors);
      });
    } else {
      onPageChanged = new OnPageChanged();
      viewPager.registerOnPageChangeCallback(onPageChanged);
      bubble2.addOnLayoutChangeListener(new UpdateChatColorsOnNextLayoutChange(selected.getAutoChatColors()));
    }

    new FullscreenHelper(this).showSystemUI();
    WindowUtil.setLightStatusBarFromTheme(this);
    WindowUtil.setLightNavigationBarFromTheme(this);
  }

  @Override protected void onDestroy() {
    if (onPageChanged != null) {
      viewPager.unregisterOnPageChangeCallback(onPageChanged);
    }

    super.onDestroy();
  }

  private class OnPageChanged extends ViewPager2.OnPageChangeCallback {
    @Override
    public void onPageSelected(int position) {
      ChatWallpaperSelectionMappingModel model = (ChatWallpaperSelectionMappingModel) adapter.getCurrentList().get(position);

      updateChatColors(model.getWallpaper().getAutoChatColors());
    }
  }

  private void updateChatColors(@NonNull ChatColors chatColors) {
    Drawable mask = chatColors.getChatBubbleMask();

    colorizerView.setBackground(mask);

    colorizerView.setProjections(
        Collections.singletonList(Projection.relativeToViewWithCommonRoot(bubble2, colorizerView, new Projection.Corners(ViewUtil.dpToPx(18))))
    );

    bubble2.getBackground().setColorFilter(chatColors.getChatBubbleColorFilter());
  }

  private class UpdateChatColorsOnNextLayoutChange implements View.OnLayoutChangeListener {

    private final ChatColors chatColors;

    private UpdateChatColorsOnNextLayoutChange(@NonNull ChatColors chatColors) {
      this.chatColors = chatColors;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
      v.removeOnLayoutChangeListener(this);
      updateChatColors(chatColors);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }
}
