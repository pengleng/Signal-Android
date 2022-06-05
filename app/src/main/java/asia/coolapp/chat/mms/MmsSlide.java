package asia.coolapp.chat.mms;


import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.attachments.Attachment;

public class MmsSlide extends ImageSlide {

  public MmsSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  @NonNull
  @Override
  public String getContentDescription() {
    return "MMS";
  }

}
