package asia.coolapp.chat.components.webrtc.participantslist;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.R;
import asia.coolapp.chat.util.adapter.mapping.MappingModel;

public class CallParticipantsListHeader implements MappingModel<CallParticipantsListHeader> {

  private int participantCount;

  public CallParticipantsListHeader(int participantCount) {
    this.participantCount = participantCount;
  }

  @NonNull String getHeader(@NonNull Context context) {
    return context.getResources().getQuantityString(R.plurals.CallParticipantsListDialog_in_this_call_d_people, participantCount, participantCount);
  }

  @Override
  public boolean areItemsTheSame(@NonNull CallParticipantsListHeader newItem) {
    return true;
  }

  @Override
  public boolean areContentsTheSame(@NonNull CallParticipantsListHeader newItem) {
    return participantCount == newItem.participantCount;
  }
}
