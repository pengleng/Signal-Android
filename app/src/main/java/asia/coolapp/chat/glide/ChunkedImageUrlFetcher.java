package asia.coolapp.chat.glide;


import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.giph.model.ChunkedImageUrl;
import asia.coolapp.chat.net.ChunkedDataFetcher;
import asia.coolapp.chat.net.RequestController;

import java.io.InputStream;

import okhttp3.OkHttpClient;

class ChunkedImageUrlFetcher implements DataFetcher<InputStream> {

  private static final String TAG = Log.tag(ChunkedImageUrlFetcher.class);

  private final OkHttpClient    client;
  private final ChunkedImageUrl url;

  private RequestController requestController;

  ChunkedImageUrlFetcher(@NonNull OkHttpClient client, @NonNull ChunkedImageUrl url) {
    this.client  = client;
    this.url     = url;
  }

  @Override
  public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    ChunkedDataFetcher fetcher = new ChunkedDataFetcher(client);
    requestController = fetcher.fetch(url.getUrl(), url.getSize(), new ChunkedDataFetcher.Callback() {
      @Override
      public void onSuccess(InputStream stream) {
        callback.onDataReady(stream);
      }

      @Override
      public void onFailure(Exception e) {
        callback.onLoadFailed(e);
      }
    });
  }

  @Override
  public void cleanup() {
    if (requestController != null) {
      requestController.cancel();
    }
  }

  @Override
  public void cancel() {
    Log.d(TAG, "Canceled.");
    if (requestController != null) {
      requestController.cancel();
    }
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }
}
