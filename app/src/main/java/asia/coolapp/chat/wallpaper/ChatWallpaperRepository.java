package asia.coolapp.chat.wallpaper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.signal.core.util.concurrent.SignalExecutors;
import asia.coolapp.chat.conversation.colors.ChatColors;
import asia.coolapp.chat.conversation.colors.ChatColorsPalette;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.keyvalue.SignalStore;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.concurrent.SerialExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

class ChatWallpaperRepository {

  private static final Executor EXECUTOR = new SerialExecutor(SignalExecutors.BOUNDED);

  @MainThread
  @Nullable ChatWallpaper getCurrentWallpaper(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getWallpaper();
    } else {
      return SignalStore.wallpaper().getWallpaper();
    }
  }

  @MainThread
  @NonNull ChatColors getCurrentChatColors(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getChatColors();
    } else if (SignalStore.chatColorsValues().hasChatColors()) {
      return Objects.requireNonNull(SignalStore.chatColorsValues().getChatColors());
    } else if (SignalStore.wallpaper().hasWallpaperSet()) {
      return Objects.requireNonNull(SignalStore.wallpaper().getWallpaper()).getAutoChatColors();
    } else {
      return ChatColorsPalette.Bubbles.getDefault().withId(ChatColors.Id.Auto.INSTANCE);
    }
  }

  void getAllWallpaper(@NonNull Consumer<List<ChatWallpaper>> consumer) {
    EXECUTOR.execute(() -> {
      List<ChatWallpaper> wallpapers = new ArrayList<>(ChatWallpaper.BuiltIns.INSTANCE.getAllBuiltIns());

      wallpapers.addAll(WallpaperStorage.getAll(ApplicationDependencies.getApplication()));
      consumer.accept(wallpapers);
    });
  }

  void saveWallpaper(@Nullable RecipientId recipientId, @Nullable ChatWallpaper chatWallpaper, @NonNull Runnable onWallpaperSaved) {
    if (recipientId != null) {
      //noinspection CodeBlock2Expr
      EXECUTOR.execute(() -> {
        SignalDatabase.recipients().setWallpaper(recipientId, chatWallpaper);
        onWallpaperSaved.run();
      });
    } else {
      SignalStore.wallpaper().setWallpaper(ApplicationDependencies.getApplication(), chatWallpaper);
      onWallpaperSaved.run();
    }
  }

  void resetAllWallpaper(@NonNull Runnable onWallpaperReset) {
    SignalStore.wallpaper().setWallpaper(ApplicationDependencies.getApplication(), null);
    EXECUTOR.execute(() -> {
      SignalDatabase.recipients().resetAllWallpaper();
      onWallpaperReset.run();
    });
  }

  void resetAllChatColors(@NonNull Runnable onColorsReset) {
    SignalStore.chatColorsValues().setChatColors(null);
    EXECUTOR.execute(() -> {
      SignalDatabase.recipients().clearAllColors();
      onColorsReset.run();
    });
  }

  void setDimInDarkTheme(@Nullable RecipientId recipientId, boolean dimInDarkTheme) {
    if (recipientId != null) {
      EXECUTOR.execute(() -> {
        Recipient recipient = Recipient.resolved(recipientId);
        if (recipient.hasOwnWallpaper()) {
          SignalDatabase.recipients().setDimWallpaperInDarkTheme(recipientId, dimInDarkTheme);
        } else if (recipient.hasWallpaper()) {
          SignalDatabase.recipients()
                       .setWallpaper(recipientId,
                                     ChatWallpaperFactory.updateWithDimming(recipient.getWallpaper(),
                                                                            dimInDarkTheme ? ChatWallpaper.FIXED_DIM_LEVEL_FOR_DARK_THEME
                                                                                           : 0f));
        } else {
          throw new IllegalStateException("Unexpected call to setDimInDarkTheme, no wallpaper has been set on the given recipient or globally.");
        }
      });
    } else {
      SignalStore.wallpaper().setDimInDarkTheme(dimInDarkTheme);
    }
  }

  public void clearChatColor(@Nullable RecipientId recipientId, @NonNull Runnable onChatColorCleared) {
    if (recipientId == null) {
      SignalStore.chatColorsValues().setChatColors(null);
      onChatColorCleared.run();
    } else {
      EXECUTOR.execute(() -> {
        SignalDatabase.recipients().clearColor(recipientId);
        onChatColorCleared.run();
      });
    }
  }
}
