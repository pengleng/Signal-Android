package asia.coolapp.chat.database

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import asia.coolapp.chat.database.MmsSmsColumns.Types
import asia.coolapp.chat.database.model.StoryType
import asia.coolapp.chat.database.model.StoryViewState
import asia.coolapp.chat.testing.TestDatabaseUtil

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class MmsDatabaseTest {
  private lateinit var db: SQLiteDatabase
  private lateinit var mmsDatabase: MmsDatabase

  @Before
  fun setup() {
    val sqlCipher = TestDatabaseUtil.inMemoryDatabase {
      execSQL(MmsDatabase.CREATE_TABLE)
    }

    db = sqlCipher.writableDatabase
    mmsDatabase = MmsDatabase(ApplicationProvider.getApplicationContext(), sqlCipher)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `isGroupQuitMessage when normal message, return false`() {
    val id = TestMms.insert(db, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT)
    assertFalse(mmsDatabase.isGroupQuitMessage(id))
  }

  @Test
  fun `isGroupQuitMessage when legacy quit message, return true`() {
    val id = TestMms.insert(db, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT or Types.GROUP_LEAVE_BIT)
    assertTrue(mmsDatabase.isGroupQuitMessage(id))
  }

  @Test
  fun `isGroupQuitMessage when GV2 leave update, return false`() {
    val id = TestMms.insert(db, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT or Types.GROUP_LEAVE_BIT or Types.GROUP_V2_BIT or Types.GROUP_UPDATE_BIT)
    assertFalse(mmsDatabase.isGroupQuitMessage(id))
  }

  @Test
  fun `getLatestGroupQuitTimestamp when only normal message, return -1`() {
    TestMms.insert(db, threadId = 1, sentTimeMillis = 1, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT)
    assertEquals(-1, mmsDatabase.getLatestGroupQuitTimestamp(1, 4))
  }

  @Test
  fun `getLatestGroupQuitTimestamp when legacy quit, return message timestamp`() {
    TestMms.insert(db, threadId = 1, sentTimeMillis = 2, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT or Types.GROUP_LEAVE_BIT)
    assertEquals(2, mmsDatabase.getLatestGroupQuitTimestamp(1, 4))
  }

  @Test
  fun `getLatestGroupQuitTimestamp when GV2 leave update message, return -1`() {
    TestMms.insert(db, threadId = 1, sentTimeMillis = 3, type = Types.BASE_SENDING_TYPE or Types.SECURE_MESSAGE_BIT or Types.PUSH_MESSAGE_BIT or Types.GROUP_LEAVE_BIT or Types.GROUP_V2_BIT or Types.GROUP_UPDATE_BIT)
    assertEquals(-1, mmsDatabase.getLatestGroupQuitTimestamp(1, 4))
  }

  @Test
  fun `Given no stories in database, when I getStoryViewState, then I expect NONE`() {
    assertEquals(StoryViewState.NONE, mmsDatabase.getStoryViewState(1))
  }

  @Test
  fun `Given stories in database not in thread 1, when I getStoryViewState for thread 1, then I expect NONE`() {
    TestMms.insert(db, threadId = 2, storyType = StoryType.STORY_WITH_REPLIES)
    TestMms.insert(db, threadId = 2, storyType = StoryType.STORY_WITH_REPLIES)
    assertEquals(StoryViewState.NONE, mmsDatabase.getStoryViewState(1))
  }

  @Test
  fun `Given viewed incoming stories in database, when I getStoryViewState, then I expect VIEWED`() {
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = true)
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = true)
    assertEquals(StoryViewState.VIEWED, mmsDatabase.getStoryViewState(1))
  }

  @Test
  fun `Given unviewed incoming stories in database, when I getStoryViewState, then I expect UNVIEWED`() {
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = false)
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = false)
    assertEquals(StoryViewState.UNVIEWED, mmsDatabase.getStoryViewState(1))
  }

  @Test
  fun `Given mix of viewed and unviewed incoming stories in database, when I getStoryViewState, then I expect UNVIEWED`() {
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = true)
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, viewed = false)
    assertEquals(StoryViewState.UNVIEWED, mmsDatabase.getStoryViewState(1))
  }

  @Test
  fun `Given only outgoing story in database, when I getStoryViewState, then I expect VIEWED`() {
    TestMms.insert(db, threadId = 1, storyType = StoryType.STORY_WITH_REPLIES, type = Types.BASE_OUTBOX_TYPE)
    assertEquals(StoryViewState.VIEWED, mmsDatabase.getStoryViewState(1))
  }
}
