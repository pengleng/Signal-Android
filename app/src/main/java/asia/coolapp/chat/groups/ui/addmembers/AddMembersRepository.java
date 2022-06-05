package asia.coolapp.chat.groups.ui.addmembers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import asia.coolapp.chat.contacts.SelectedContact;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.dependencies.ApplicationDependencies;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.recipients.RecipientId;

final class AddMembersRepository {

  private final Context context;
  private final GroupId groupId;

  AddMembersRepository(@NonNull GroupId groupId) {
    this.groupId = groupId;
    this.context = ApplicationDependencies.getApplication();
  }

  @WorkerThread
  RecipientId getOrCreateRecipientId(@NonNull SelectedContact selectedContact) {
    return selectedContact.getOrCreateRecipientId(context);
  }

  @WorkerThread
  String getGroupTitle() {
    return SignalDatabase.groups().requireGroup(groupId).getTitle();
  }
}
