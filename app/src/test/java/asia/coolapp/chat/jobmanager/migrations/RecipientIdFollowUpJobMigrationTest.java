package asia.coolapp.chat.jobmanager.migrations;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import asia.coolapp.chat.jobmanager.Data;
import asia.coolapp.chat.jobmanager.Job;
import asia.coolapp.chat.jobmanager.JobMigration.JobData;
import asia.coolapp.chat.jobs.FailingJob;
import asia.coolapp.chat.jobs.RequestGroupInfoJob;
import asia.coolapp.chat.jobs.SendDeliveryReceiptJob;
import asia.coolapp.chat.recipients.Recipient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class RecipientIdFollowUpJobMigrationTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private MockedStatic<Recipient> recipientMockedStatic;

  @Mock
  private MockedStatic<Job.Parameters> jobParametersMockedStatic;

  @Test
  public void migrate_requestGroupInfoJob_good() throws Exception {
    JobData testData = new JobData("RequestGroupInfoJob", null, new Data.Builder().putString("source", "1")
                                                                                  .putString("group_id", "__textsecure_group__!abcdef0123456789abcdef0123456789")
                                                                                  .build());
    RecipientIdFollowUpJobMigration subject   = new RecipientIdFollowUpJobMigration();
    JobData                         converted = subject.migrate(testData);

    assertEquals("RequestGroupInfoJob", converted.getFactoryKey());
    assertNull(converted.getQueueKey());
    assertEquals("1", converted.getData().getString("source"));
    assertEquals("__textsecure_group__!abcdef0123456789abcdef0123456789", converted.getData().getString("group_id"));

    new RequestGroupInfoJob.Factory().create(mock(Job.Parameters.class), converted.getData());
  }

  @Test
  public void migrate_requestGroupInfoJob_bad() throws Exception {
    JobData testData = new JobData("RequestGroupInfoJob", null, new Data.Builder().putString("source", "1")
                                                                                  .build());
    RecipientIdFollowUpJobMigration subject   = new RecipientIdFollowUpJobMigration();
    JobData                         converted = subject.migrate(testData);

    assertEquals("FailingJob", converted.getFactoryKey());
    assertNull(converted.getQueueKey());

    new FailingJob.Factory().create(mock(Job.Parameters.class), converted.getData());
  }

  @Test
  public void migrate_sendDeliveryReceiptJob_good() throws Exception {
    JobData testData = new JobData("SendDeliveryReceiptJob", null, new Data.Builder().putString("recipient", "1")
                                                                                     .putLong("message_id", 1)
                                                                                     .putLong("timestamp", 2)
                                                                                     .build());
    RecipientIdFollowUpJobMigration subject   = new RecipientIdFollowUpJobMigration();
    JobData                         converted = subject.migrate(testData);

    assertEquals("SendDeliveryReceiptJob", converted.getFactoryKey());
    assertNull(converted.getQueueKey());
    assertEquals("1", converted.getData().getString("recipient"));
    assertEquals(1, converted.getData().getLong("message_id"));
    assertEquals(2, converted.getData().getLong("timestamp"));

    new SendDeliveryReceiptJob.Factory().create(mock(Job.Parameters.class), converted.getData());
  }

  @Test
  public void migrate_sendDeliveryReceiptJob_bad() throws Exception {
    JobData testData = new JobData("SendDeliveryReceiptJob", null, new Data.Builder().putString("recipient", "1")
                                                                                     .build());
    RecipientIdFollowUpJobMigration subject   = new RecipientIdFollowUpJobMigration();
    JobData                         converted = subject.migrate(testData);

    assertEquals("FailingJob", converted.getFactoryKey());
    assertNull(converted.getQueueKey());

    new FailingJob.Factory().create(mock(Job.Parameters.class), converted.getData());
  }
}
