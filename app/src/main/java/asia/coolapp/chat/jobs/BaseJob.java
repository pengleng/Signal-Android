package asia.coolapp.chat.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.signal.core.util.tracing.Tracer;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.JobLogger;
import asia.coolapp.chat.jobmanager.JobManager.Chain;
import asia.coolapp.chat.jobmanager.impl.BackoffUtil;
import asia.coolapp.chat.util.FeatureFlags;

public abstract class BaseJob extends Job {

  private static final String TAG = Log.tag(BaseJob.class);

  private Data outputData;

  public BaseJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull Result run() {
    if (shouldTrace()) {
      Tracer.getInstance().start(getClass().getSimpleName());
    }

    try {
      onRun();
      return Result.success(outputData);
    } catch (RuntimeException e) {
      Log.e(TAG, "Encountered a fatal exception. Crash imminent.", e);
      return Result.fatalFailure(e);
    } catch (Exception e) {
      if (onShouldRetry(e)) {
        Log.i(TAG, JobLogger.format(this, "Encountered a retryable exception."), e);
        return Result.retry(getNextRunAttemptBackoff(getRunAttempt() + 1, e));
      } else {
        Log.w(TAG, JobLogger.format(this, "Encountered a failing exception."), e);
        return Result.failure();
      }
    } finally {
      if (shouldTrace()) {
        Tracer.getInstance().end(getClass().getSimpleName());
      }
    }
  }

  /**
   * Should return how long you'd like to wait until the next retry, given the attempt count and
   * exception that caused the retry. The attempt count is the number of attempts that have been
   * made already, so this value will be at least 1.
   *
   * There is a sane default implementation here that uses exponential backoff, but jobs can
   * override this behavior to define custom backoff behavior.
   */
  public long getNextRunAttemptBackoff(int pastAttemptCount, @NonNull Exception exception) {
    return BackoffUtil.exponentialBackoff(pastAttemptCount, FeatureFlags.getDefaultMaxBackoff());
  }

  protected abstract void onRun() throws Exception;

  protected abstract boolean onShouldRetry(@NonNull Exception e);

  /**
   * Whether or not the job should be traced with the {@link org.signal.core.util.tracing.Tracer}.
   */
  protected boolean shouldTrace() {
    return false;
  }

  /**
   * If this job is part of a {@link Chain}, data set here will be passed as input data to the next
   * job(s) in the chain.
   */
  protected void setOutputData(@Nullable Data outputData) {
    this.outputData = outputData;
  }

  protected void log(@NonNull String tag, @NonNull String message) {
    Log.i(tag, JobLogger.format(this, message));
  }

  protected void log(@NonNull String tag, @NonNull String extra, @NonNull String message) {
    Log.i(tag, JobLogger.format(this, extra, message));
  }

  protected void warn(@NonNull String tag, @NonNull String message) {
    warn(tag, "", message, null);
  }

  protected void warn(@NonNull String tag, @NonNull String event, @NonNull String message) {
    warn(tag, event, message, null);
  }

  protected void warn(@NonNull String tag, @Nullable Throwable t) {
    warn(tag, "", t);
  }

  protected void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
    warn(tag, "", message, t);
  }

  protected void warn(@NonNull String tag, @NonNull String extra, @NonNull String message, @Nullable Throwable t) {
    Log.w(tag, JobLogger.format(this, extra, message), t);
  }
}
