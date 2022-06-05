package asia.coolapp.chat.components.webrtc.participantslist;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import asia.coolapp.chat.R;
import asia.coolapp.chat.util.adapter.mapping.MappingViewHolder;

public class CallParticipantsListHeaderViewHolder extends MappingViewHolder<CallParticipantsListHeader> {

  private final TextView headerText;

  public CallParticipantsListHeaderViewHolder(@NonNull View itemView) {
    super(itemView);
    headerText = findViewById(R.id.call_participants_list_header);
  }

  @Override
  public void bind(@NonNull CallParticipantsListHeader model) {
    headerText.setText(model.getHeader(getContext()));
  }
}
