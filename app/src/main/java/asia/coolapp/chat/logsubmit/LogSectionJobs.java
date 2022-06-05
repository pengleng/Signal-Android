package asia.coolapp.chat.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import asia.coolapp.chat.dependencies.ApplicationDependencies;

public class LogSectionJobs implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "JOBS";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return ApplicationDependencies.getJobManager().getDebugInfo();
  }
}
