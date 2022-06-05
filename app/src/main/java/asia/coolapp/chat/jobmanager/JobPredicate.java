package asia.coolapp.chat.jobmanager;

import androidx.annotation.NonNull;

import asia.coolapp.chat.jobmanager.persistence.JobSpec;

public interface JobPredicate {
  JobPredicate NONE = jobSpec -> true;

  boolean shouldRun(@NonNull JobSpec jobSpec);
}
