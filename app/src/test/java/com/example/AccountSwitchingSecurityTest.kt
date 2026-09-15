package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.SessionVerificationState
import com.example.data.database.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.SyncStatus
import com.example.data.model.Worker
import com.example.data.sync.SyncEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AccountSwitchingSecurityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSessionVerificationStates() {
        var state = SessionVerificationState.SESSION_UNKNOWN
        assertEquals(SessionVerificationState.SESSION_UNKNOWN, state)

        state = SessionVerificationState.SESSION_VALID
        assertEquals(SessionVerificationState.SESSION_VALID, state)

        state = SessionVerificationState.SESSION_INVALID
        assertEquals(SessionVerificationState.SESSION_INVALID, state)
    }

    @Test
    fun testAccountSwitching_PendingCountIsolation() = runBlocking {
        val syncDao = db.syncDao()
        val userA = "user_farmer_A"
        val userB = "user_farmer_B"

        // Account A creates 3 records
        val plotA = Plot(id = "plot_A_1", name = "A Field", area = 2.0, userId = userA, syncStatus = SyncStatus.PENDING)
        val workerA = Worker(id = "worker_A_1", name = "Suresh", dailyWageRate = 50000L, userId = userA, syncStatus = SyncStatus.PENDING)
        val expenseA = Expense(id = "expense_A_1", date = "2026-09-15", category = "Seeds", amount = 120000L, description = "Mustard seeds", userId = userA, syncStatus = SyncStatus.PENDING)

        syncDao.upsertPlot(plotA)
        syncDao.upsertWorker(workerA)
        syncDao.upsertExpense(expenseA)

        // Account B creates 1 record
        val plotB = Plot(id = "plot_B_1", name = "B Field", area = 5.0, userId = userB, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plotB)

        // Verify Account A's pending indicator counts strictly Account A's records (3)
        val countA = syncDao.getPendingCountForUserFlow(userA).first()
        assertEquals("Account A pending count must be exactly 3", 3, countA)

        // Verify Account B's pending indicator counts strictly Account B's records (1)
        // Account A's pending changes must NEVER leak into Account B's count!
        val countB = syncDao.getPendingCountForUserFlow(userB).first()
        assertEquals("Account B pending count must be exactly 1", 1, countB)

        // Account A marks all records synced
        syncDao.markPlotsSynced(userA, listOf(plotA.id))
        syncDao.markWorkersSynced(userA, listOf(workerA.id))
        syncDao.markExpensesSynced(userA, listOf(expenseA.id))

        val countAAfter = syncDao.getPendingCountForUserFlow(userA).first()
        assertEquals(0, countAAfter)

        // Account B's pending count must remain untouched (still 1)
        val countBAfter = syncDao.getPendingCountForUserFlow(userB).first()
        assertEquals(1, countBAfter)
    }

    @Test
    fun testAccountSwitching_MutationIsolation_UserBCannotMarkUserASynced() = runBlocking {
        val syncDao = db.syncDao()
        val userA = "user_farmer_A"
        val userB = "user_farmer_B"

        val plotA = Plot(id = "plot_A_secure", name = "A Field", area = 2.0, userId = userA, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plotA)

        // Adversarial test: User B attempts to mark User A's record as SYNCED
        syncDao.markPlotsSynced(userB, listOf(plotA.id))

        // Record must remain PENDING because userId = userB did not match plotA.userId
        val plotAfterUnauthorizedMutation = syncDao.getPlotById(plotA.id)
        assertNotNull(plotAfterUnauthorizedMutation)
        assertEquals(
            "User A record must remain PENDING when User B tries to mutate it",
            SyncStatus.PENDING,
            plotAfterUnauthorizedMutation?.syncStatus
        )

        // Authorized User A mutation succeeds
        syncDao.markPlotsSynced(userA, listOf(plotA.id))
        val plotAfterAuthorizedMutation = syncDao.getPlotById(plotA.id)
        assertEquals(
            "User A record must be SYNCED after User A marks it",
            SyncStatus.SYNCED,
            plotAfterAuthorizedMutation?.syncStatus
        )
    }

    @Test
    fun testAccountSwitching_PerUserCheckpointIsolation() {
        val authRepo = AuthRepository(context)
        val syncEngine = SyncEngine(context, db, authRepo)

        val userA = "user_checkpoint_A"
        val userB = "user_checkpoint_B"

        // Initial checkpoints are 0
        assertEquals(0L, syncEngine.getLastSyncCheckpoint(userA))
        assertEquals(0L, syncEngine.getLastSyncCheckpoint(userB))

        // User A sync completes
        val timestampA = 1726350000000L
        syncEngine.setLastSyncCheckpoint(userA, timestampA)

        // User A has checkpoint, User B must still be 0 (isolated per-user checkpoints)
        assertEquals(timestampA, syncEngine.getLastSyncCheckpoint(userA))
        assertEquals(0L, syncEngine.getLastSyncCheckpoint(userB))

        // User B sync completes
        val timestampB = 1726400000000L
        syncEngine.setLastSyncCheckpoint(userB, timestampB)

        // Both maintain their own checkpoints without overwrite
        assertEquals(timestampA, syncEngine.getLastSyncCheckpoint(userA))
        assertEquals(timestampB, syncEngine.getLastSyncCheckpoint(userB))
    }

    @Test
    fun testFirstLoginClaiming_DoesNotStealExistingUserData() = runBlocking {
        val syncDao = db.syncDao()
        val userA = "user_owner_A"
        val userB = "user_new_login_B"

        // Pre-existing records belonging to User A
        val plotA = Plot(id = "plot_A_existing", name = "A Plot", area = 2.0, userId = userA)
        syncDao.upsertPlot(plotA)

        // Offline unassigned record (created before any user logged in)
        val plotOffline = Plot(id = "plot_offline_unassigned", name = "Offline Plot", area = 1.0, userId = null)
        syncDao.upsertPlot(plotOffline)

        // User B logs in and runs first login migration
        val authRepo = AuthRepository(context)
        val syncEngine = SyncEngine(context, db, authRepo)
        syncEngine.executeFirstLoginMigrationIfNeeded(userB)

        // User A's plot must NOT be stolen by User B
        val plotACheck = syncDao.getPlotById("plot_A_existing")
        assertEquals("User A record must remain owned by User A", userA, plotACheck?.userId)

        // Only the unassigned record is claimed by User B
        val plotOfflineCheck = syncDao.getPlotById("plot_offline_unassigned")
        assertEquals("Unassigned record claimed by User B", userB, plotOfflineCheck?.userId)
    }
}
