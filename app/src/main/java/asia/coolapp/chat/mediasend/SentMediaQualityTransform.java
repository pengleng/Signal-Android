package asia.coolapp.chat.mediasend;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import asia.coolapp.chat.database.AttachmentDatabase;
import asia.coolapp.chat.mms.SentMediaQuality;

import java.util.Optional;


/**
 * Add a {@link SentMediaQuality} value for {@link AttachmentDatabase.TransformProperties#getSentMediaQuality()} on the
 * transformed media. Safe to use in a pipeline with other transforms.
 */
public final class SentMediaQualityTransform implements MediaTransform {

  private final SentMediaQuality sentMediaQuality;

  public SentMediaQualityTransform(@NonNull SentMediaQuality sentMediaQuality) {
    this.sentMediaQuality = sentMediaQuality;
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
                     Optional.of(AttachmentDatabase.TransformProperties.forSentMediaQuality(media.getTransformProperties(), sentMediaQuality)));
  }
}
