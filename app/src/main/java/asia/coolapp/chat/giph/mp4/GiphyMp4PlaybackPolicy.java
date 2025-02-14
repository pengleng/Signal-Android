package asia.coolapp.chat.giph.mp4;

import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.util.DeviceProperties;

import java.util.concurrent.TimeUnit;

/**
 * Central policy object for determining what kind of gifs to display, routing, etc.
 */
public final class GiphyMp4PlaybackPolicy {

  private GiphyMp4PlaybackPolicy() { }

  public static boolean autoplay() {
    return !DeviceProperties.isLowMemoryDevice(ApplicationDependencies.getApplication());
  }

  public static int maxRepeatsOfSinglePlayback() {
    return 4;
  }

  public static long maxDurationOfSinglePlayback() {
    return TimeUnit.SECONDS.toMillis(8);
  }

  public static int maxSimultaneousPlaybackInSearchResults() {
    return 12;
  }

  public static int maxSimultaneousPlaybackInConversation() {
    return 4;
  }
}
