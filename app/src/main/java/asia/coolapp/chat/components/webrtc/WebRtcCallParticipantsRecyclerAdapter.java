package asia.coolapp.chat.components.webrtc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import asia.coolapp.chat.R;
import asia.coolapp.chat.events.CallParticipant;
import org.webrtc.RendererCommon;

class WebRtcCallParticipantsRecyclerAdapter extends ListAdapter<CallParticipant, WebRtcCallParticipantsRecyclerAdapter.ViewHolder> {

  private static final int PARTICIPANT = 0;
  private static final int EMPTY       = 1;

  protected WebRtcCallParticipantsRecyclerAdapter() {
    super(new DiffCallback());
  }

  @Override
  public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == PARTICIPANT) {
      return new ParticipantViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.webrtc_call_participant_recycler_item, parent, false));
    } else {
      return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.webrtc_call_participant_recycler_empty_item, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position) == CallParticipant.EMPTY ? EMPTY : PARTICIPANT;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    ViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    void bind(@NonNull CallParticipant callParticipant) {}
  }

  private static class ParticipantViewHolder extends ViewHolder {

    private final CallParticipantView callParticipantView;

    ParticipantViewHolder(@NonNull View itemView) {
      super(itemView);
      callParticipantView = itemView.findViewById(R.id.call_participant);
    }

    @Override
    void bind(@NonNull CallParticipant callParticipant) {
      callParticipantView.setCallParticipant(callParticipant);
      callParticipantView.setRenderInPip(true);
      callParticipantView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }
  }

  private static class DiffCallback extends DiffUtil.ItemCallback<CallParticipant> {

    @Override
    public boolean areItemsTheSame(@NonNull CallParticipant oldItem, @NonNull CallParticipant newItem) {
      return oldItem.getRecipient().equals(newItem.getRecipient());
    }

    @Override
    public boolean areContentsTheSame(@NonNull CallParticipant oldItem, @NonNull CallParticipant newItem) {
      return oldItem.equals(newItem);
    }
  }

}
