package asia.coolapp.chat.jobs;

import org.signal.core.util.logging.Log;
import asia.coolapp.chat.jobmanager.Job;

public abstract class PushReceivedJob extends BaseJob {

  private static final String TAG = Log.tag(PushReceivedJob.class);


  protected PushReceivedJob(Job.Parameters parameters) {
    super(parameters);
  }

}
