package asia.coolapp.chat.blocked;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import asia.coolapp.chat.BlockUnblockDialog;
import asia.coolapp.chat.R;
import asia.coolapp.chat.recipients.Recipient;

public class BlockedUsersFragment extends Fragment {

  private BlockedUsersViewModel viewModel;
  private Listener              listener;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    if (context instanceof Listener) {
      listener = (Listener) context;
    } else {
      throw new ClassCastException("Expected context to implement Listener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();

    listener = null;
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.blocked_users_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    View                addUser  = view.findViewById(R.id.add_blocked_user_touch_target);
    RecyclerView        recycler = view.findViewById(R.id.blocked_users_recycler);
    View                empty    = view.findViewById(R.id.no_blocked_users);
    BlockedUsersAdapter adapter  = new BlockedUsersAdapter(this::handleRecipientClicked);

    recycler.setAdapter(adapter);

    addUser.setOnClickListener(unused -> {
      if (listener != null) {
        listener.handleAddUserToBlockedList();
      }
    });

    viewModel = ViewModelProviders.of(requireActivity()).get(BlockedUsersViewModel.class);
    viewModel.getRecipients().observe(getViewLifecycleOwner(), list -> {
      if (list.isEmpty()) {
        empty.setVisibility(View.VISIBLE);
      } else {
        empty.setVisibility(View.GONE);
      }

      adapter.submitList(list);
    });
  }

  private void handleRecipientClicked(@NonNull Recipient recipient) {
    BlockUnblockDialog.showUnblockFor(requireContext(), getViewLifecycleOwner().getLifecycle(), recipient, () -> {
      viewModel.unblock(recipient.getId());
    });
  }

  interface Listener {
    void handleAddUserToBlockedList();
  }
}
