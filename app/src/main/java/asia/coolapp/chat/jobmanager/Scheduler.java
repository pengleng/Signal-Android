package asia.coolapp.chat.jobmanager;

import androidx.annotation.NonNull;

import java.util.List;

public interface Scheduler {
  void schedule(long delay, @NonNull List<Constraint> constraints);
}
