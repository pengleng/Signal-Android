package asia.coolapp.chat;

import androidx.annotation.NonNull;

import asia.coolapp.chat.conversationlist.model.ConversationSet;
import asia.coolapp.chat.database.model.ThreadRecord;
import asia.coolapp.chat.mms.GlideRequests;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

  void bind(@NonNull ThreadRecord thread,
            @NonNull GlideRequests glideRequests, @NonNull Locale locale,
            @NonNull Set<Long> typingThreads,
            @NonNull ConversationSet selectedConversations);

  void setSelectedConversations(@NonNull ConversationSet conversations);
  void updateTypingIndicator(@NonNull Set<Long> typingThreads);
}
