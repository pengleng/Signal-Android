package asia.coolapp.chat.mediasend;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import asia.coolapp.chat.database.AttachmentDatabase;
import asia.coolapp.chat.mms.SentMediaQuality;

import java.util.Optional;


public final class VideoTrimTransform implements MediaTransform {

  private final VideoEditorFragment.Data data;

  public VideoTrimTransform(@NonNull VideoEditorFragment.Data data) {
    this.data = data;
  }

  @WorkerThread
  @Override
  public @NonNull Media transform(@NonNull Context context, @NonNull Media media) {
    return new Media(media.getUri(),
                     media.getMimeType(),
                     media.getDate(),
                     media.getWidth(),
                     media.getHeight(),
                     media.getSize(),
                     media.getDuration(),
                     media.isBorderless(),
                     media.isVideoGif(),
                     media.getBucketId(),
                     media.getCaption(),
                     Optional.of(new AttachmentDatabase.TransformProperties(false, data.durationEdited, data.startTimeUs, data.endTimeUs, SentMediaQuality.STANDARD.getCode())));
  }
}
