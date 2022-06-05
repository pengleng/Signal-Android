package asia.coolapp.chat.groups.ui.migration;

import android.content.DialogInterface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import asia.coolapp.chat.R;
import asia.coolapp.chat.database.SignalDatabase;
import asia.coolapp.chat.groups.GroupChangeBusyException;
import asia.coolapp.chat.groups.GroupChangeFailedException;
import asia.coolapp.chat.groups.GroupId;
import asia.coolapp.chat.groups.GroupInsufficientRightsException;
import asia.coolapp.chat.groups.GroupManager;
import asia.coolapp.chat.groups.GroupNotAMemberException;
import asia.coolapp.chat.groups.MembershipNotSuitableForV2Exception;
import asia.coolapp.chat.groups.ui.GroupMemberListView;
import asia.coolapp.chat.recipients.Recipient;
import asia.coolapp.chat.recipients.RecipientId;
import asia.coolapp.chat.util.concurrent.SimpleTask;
import asia.coolapp.chat.util.views.SimpleProgressDialog;

import java.io.IOException;
import java.util.List;

/**
 * Shows a list of members that got lost when migrating from a V1->V2 group, giving you the chance
 * to add them back.
 */
public final class GroupsV1MigrationSuggestionsDialog {

  private static final String TAG = Log.tag(GroupsV1MigrationSuggestionsDialog.class);

  private final FragmentActivity  fragmentActivity;
  private final GroupId.V2        groupId;
  private final List<RecipientId> suggestions;

  public static void show(@NonNull FragmentActivity activity,
                          @NonNull GroupId.V2 groupId,
                          @NonNull List<RecipientId> suggestions)
  {
    new GroupsV1MigrationSuggestionsDialog(activity, groupId, suggestions).display();
  }

  private GroupsV1MigrationSuggestionsDialog(@NonNull FragmentActivity activity,
                                            @NonNull GroupId.V2 groupId,
                                            @NonNull List<RecipientId> suggestions)
  {
    this.fragmentActivity = activity;
    this.groupId          = groupId;
    this.suggestions      = suggestions;
  }

  private void display() {
    AlertDialog dialog = new AlertDialog.Builder(fragmentActivity)
                                        .setTitle(fragmentActivity.getResources().getQuantityString(R.plurals.GroupsV1MigrationSuggestionsDialog_add_members_question, suggestions.size()))
                                        .setMessage(fragmentActivity.getResources().getQuantityString(R.plurals.GroupsV1MigrationSuggestionsDialog_these_members_couldnt_be_automatically_added, suggestions.size()))
                                        .setView(R.layout.dialog_group_members)
                                        .setPositiveButton(fragmentActivity.getResources().getQuantityString(R.plurals.GroupsV1MigrationSuggestionsDialog_add_members, suggestions.size()), (d, i) -> onAddClicked(d))
                                        .setNegativeButton(android.R.string.cancel, (d, i) -> d.dismiss())
                                        .show();

    GroupMemberListView memberListView = dialog.findViewById(R.id.list_members);
    memberListView.initializeAdapter(fragmentActivity);

    SimpleTask.run(() -> Recipient.resolvedList(suggestions),
                   memberListView::setDisplayOnlyMembers);
  }

  private void onAddClicked(@NonNull DialogInterface rootDialog) {
    SimpleProgressDialog.DismissibleDialog progressDialog = SimpleProgressDialog.showDelayed(fragmentActivity, 300, 0);
    SimpleTask.run(SignalExecutors.UNBOUNDED, () -> {
      try {
        GroupManager.addMembers(fragmentActivity, groupId.requirePush(), suggestions);
        Log.i(TAG, "Successfully added members! Removing these dropped members from the list.");
        SignalDatabase.groups().removeUnmigratedV1Members(groupId, suggestions);
        return Result.SUCCESS;
      } catch (IOException | GroupChangeBusyException e) {
        Log.w(TAG, "Temporary failure.", e);
        return Result.NETWORK_ERROR;
      } catch (GroupNotAMemberException | GroupInsufficientRightsException | MembershipNotSuitableForV2Exception | GroupChangeFailedException e) {
        Log.w(TAG, "Permanent failure! Removing these dropped members from the list.", e);
        SignalDatabase.groups().removeUnmigratedV1Members(groupId, suggestions);
        return Result.IMPOSSIBLE;
      }
    }, result -> {
      progressDialog.dismiss();
      rootDialog.dismiss();

      switch (result) {
        case NETWORK_ERROR:
          Toast.makeText(fragmentActivity, fragmentActivity.getResources().getQuantityText(R.plurals.GroupsV1MigrationSuggestionsDialog_failed_to_add_members_try_again_later, suggestions.size()), Toast.LENGTH_SHORT).show();
          break;
        case IMPOSSIBLE:
          Toast.makeText(fragmentActivity, fragmentActivity.getResources().getQuantityText(R.plurals.GroupsV1MigrationSuggestionsDialog_cannot_add_members, suggestions.size()), Toast.LENGTH_SHORT).show();
          break;
      }
    });
  }

  private enum Result {
    SUCCESS, NETWORK_ERROR, IMPOSSIBLE
  }
}
