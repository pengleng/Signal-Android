package asia.coolapp.chat.conversationlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.signal.paging.PagingController;
import asia.coolapp.chat.BindableConversationListItem;
import asia.coolapp.chat.R;
import asia.coolapp.chat.conversationlist.model.Conversation;
import asia.coolapp.chat.conversationlist.model.ConversationSet;
import asia.coolapp.chat.mms.GlideRequests;
import asia.coolapp.chat.util.CachedInflater;
import asia.coolapp.chat.util.ViewUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

class ConversationListAdapter extends ListAdapter<Conversation, RecyclerView.ViewHolder> {

  private static final int TYPE_THREAD      = 1;
  private static final int TYPE_ACTION      = 2;
  private static final int TYPE_PLACEHOLDER = 3;
  private static final int TYPE_HEADER      = 4;

  private enum Payload {
    TYPING_INDICATOR,
    SELECTION
  }

  private final GlideRequests               glideRequests;
  private final OnConversationClickListener onConversationClickListener;
  private       ConversationSet             selectedConversations = new ConversationSet();
  private final Set<Long>                   typingSet             = new HashSet<>();

  private PagingController pagingController;

  protected ConversationListAdapter(@NonNull GlideRequests glideRequests,
                                    @NonNull OnConversationClickListener onConversationClickListener)
  {
    super(new ConversationDiffCallback());

    this.glideRequests               = glideRequests;
    this.onConversationClickListener = onConversationClickListener;
  }

  @Override
  public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_ACTION) {
      ConversationViewHolder holder = new ConversationViewHolder(LayoutInflater.from(parent.getContext())
                                                                               .inflate(R.layout.conversation_list_item_action, parent, false));

      holder.itemView.setOnClickListener(v -> {
        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
          onConversationClickListener.onShowArchiveClick();
        }
      });

      return holder;
    } else if (viewType == TYPE_THREAD) {
      ConversationViewHolder holder = new ConversationViewHolder(CachedInflater.from(parent.getContext())
                                                                               .inflate(R.layout.conversation_list_item_view, parent, false));

      holder.itemView.setOnClickListener(v -> {
        int position = holder.getAdapterPosition();

        if (position != RecyclerView.NO_POSITION) {
          onConversationClickListener.onConversationClick(getItem(position));
        }
      });

      holder.itemView.setOnLongClickListener(v -> {
        int position = holder.getAdapterPosition();

        if (position != RecyclerView.NO_POSITION) {
          return onConversationClickListener.onConversationLongClick(getItem(position), v);
        }

        return false;
      });
      return holder;
    } else if (viewType == TYPE_PLACEHOLDER) {
      View v = new FrameLayout(parent.getContext());
      v.setLayoutParams(new FrameLayout.LayoutParams(1, ViewUtil.dpToPx(100)));
      return new PlaceholderViewHolder(v);
    } else if (viewType == TYPE_HEADER) {
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dsl_section_header, parent, false);
      return new HeaderViewHolder(v);
    } else {
      throw new IllegalStateException("Unknown type! " + viewType);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
    if (payloads.isEmpty()) {
      onBindViewHolder(holder, position);
    } else if (holder instanceof ConversationViewHolder) {
      for (Object payloadObject : payloads) {
        if (payloadObject instanceof Payload) {
          Payload payload = (Payload) payloadObject;

          if (payload == Payload.SELECTION) {
            ((ConversationViewHolder) holder).getConversationListItem().setSelectedConversations(selectedConversations);
          } else {
            ((ConversationViewHolder) holder).getConversationListItem().updateTypingIndicator(typingSet);
          }
        }
      }
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder.getItemViewType() == TYPE_ACTION || holder.getItemViewType() == TYPE_THREAD) {
      ConversationViewHolder casted       = (ConversationViewHolder) holder;
      Conversation           conversation = Objects.requireNonNull(getItem(position));

      casted.getConversationListItem().bind(conversation.getThreadRecord(),
                                            glideRequests,
                                            Locale.getDefault(),
                                            typingSet,
                                            selectedConversations);
    } else if (holder.getItemViewType() == TYPE_HEADER) {
      HeaderViewHolder casted       = (HeaderViewHolder) holder;
      Conversation     conversation = Objects.requireNonNull(getItem(position));
      switch (conversation.getType()) {
        case PINNED_HEADER:
          casted.headerText.setText(R.string.conversation_list__pinned);
          break;
        case UNPINNED_HEADER:
          casted.headerText.setText(R.string.conversation_list__chats);
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
    if (holder instanceof ConversationViewHolder) {
      ((ConversationViewHolder) holder).getConversationListItem().unbind();
    }
  }

  @Override
  protected Conversation getItem(int position) {
    if (pagingController != null) {
      pagingController.onDataNeededAroundIndex(position);
    }

    return super.getItem(position);
  }

  public void setPagingController(@Nullable PagingController pagingController) {
    this.pagingController = pagingController;
  }

  void setTypingThreads(@NonNull Set<Long> typingThreadSet) {
    this.typingSet.clear();
    this.typingSet.addAll(typingThreadSet);

    notifyItemRangeChanged(0, getItemCount(), Payload.TYPING_INDICATOR);
  }

  void setSelectedConversations(@NonNull ConversationSet conversations) {
    selectedConversations = conversations;
    notifyItemRangeChanged(0, getItemCount(), Payload.SELECTION);
  }

  @Override
  public int getItemViewType(int position) {
    Conversation conversation = getItem(position);
    if (conversation == null) {
      return TYPE_PLACEHOLDER;
    }
    switch (conversation.getType()) {
      case PINNED_HEADER:
      case UNPINNED_HEADER:
        return TYPE_HEADER;
      case ARCHIVED_FOOTER:
        return TYPE_ACTION;
      case THREAD:
        return TYPE_THREAD;
      default:
        throw new IllegalArgumentException();
    }
  }

  static final class ConversationViewHolder extends RecyclerView.ViewHolder {

    private final BindableConversationListItem conversationListItem;

    ConversationViewHolder(@NonNull View itemView) {
      super(itemView);

      conversationListItem = (BindableConversationListItem) itemView;
    }

    public BindableConversationListItem getConversationListItem() {
      return conversationListItem;
    }
  }

  private static final class ConversationDiffCallback extends DiffUtil.ItemCallback<Conversation> {

    @Override
    public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
      return oldItem.getThreadRecord().getThreadId() == newItem.getThreadRecord().getThreadId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
      return oldItem.equals(newItem);
    }
  }

  private static class PlaceholderViewHolder extends RecyclerView.ViewHolder {
    PlaceholderViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    private TextView headerText;

    public HeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerText = itemView.findViewById(R.id.section_header);
    }
  }

  interface OnConversationClickListener {
    void onConversationClick(@NonNull Conversation conversation);
    boolean onConversationLongClick(@NonNull Conversation conversation, @NonNull View view);
    void onShowArchiveClick();
  }
}
