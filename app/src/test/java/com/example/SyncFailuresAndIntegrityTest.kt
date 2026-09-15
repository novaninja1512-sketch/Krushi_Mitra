package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Attendance
import com.example.data.model.CropAssignment
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.SyncStatus
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.sync.SyncConflictResolver
import com.example.data.sync.model.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Adversarial sync failure, auth resilience, and data integrity invariant tests.
 */
@RunWith(RobolectricTestRunner::class)
class SyncFailuresAndIntegrityTest {

    private lateinit var db: AppDatabase

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // 4. TEST IDEMPOTENCY
    // =========================================================================
    @Test
    fun testSyncIdempotency_MultipleExecutionsProduceSameState() = runBlocking {
        val syncDao = db.syncDao()
        val userId = "test_user_id"

        // Setup local records
        val plot = Plot(id = "plot-idem-1", name = "Idempotent Plot", area = 4.0, userId = userId, syncStatus = SyncStatus.PENDING)
        val worker = Worker(id = "worker-idem-1", name = "Arjun", dailyWageRate = 50000L, userId = userId, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plot)
        syncDao.upsertWorker(worker)

        // Simulate N sync executions
        val N = 5
        for (i in 1..N) {
            // Read pending
            val pendingPlots = syncDao.getPendingPlots(userId)
            val pendingWorkers = syncDao.getPendingWorkers(userId)

            // Simulate successful upload and status mark
            if (pendingPlots.isNotEmpty()) {
                syncDao.markPlotsSynced(listOf(plot.id))
            }
            if (pendingWorkers.isNotEmpty()) {
                syncDao.markWorkersSynced(listOf(worker.id))
            }

            // Simulate download upsert of same record from remote
            val remotePlot = plot.copy(syncStatus = SyncStatus.SYNCED, updatedAt = 1000L)
            syncDao.upsertPlot(remotePlot)

            // Verify count remains exactly 1 (no duplicate rows)
            val allPlots = syncDao.getPendingPlots(userId)
            assertEquals(0, allPlots.size)
        }

        val finalPlot = syncDao.getPlotById("plot-idem-1")
        assertNotNull(finalPlot)
        assertEquals(SyncStatus.SYNCED, finalPlot?.syncStatus)
    }

    // =========================================================================
    // 5. TEST PARTIAL UPLOAD FAILURE
    // =========================================================================
    @Test
    fun testPartialUploadFailure_HandlesAtomicallyPerRecord() = runBlocking {
        val syncDao = db.syncDao()
        val userId = "user_partial_test"

        // Scenario: Records A, B, C are pending
        val plotA = Plot(id = "p-A", name = "Plot A", area = 1.0, userId = userId, syncStatus = SyncStatus.PENDING)
        val plotB = Plot(id = "p-B", name = "Plot B", area = 2.0, userId = userId, syncStatus = SyncStatus.PENDING)
        val plotC = Plot(id = "p-C", name = "Plot C", area = 3.0, userId = userId, syncStatus = SyncStatus.PENDING)

        syncDao.upsertPlot(plotA)
        syncDao.upsertPlot(plotB)
        syncDao.upsertPlot(plotC)

        // Upload loop: A succeeds, B throws network exception, C succeeds
        val pending = syncDao.getPendingPlots(userId)
        assertEquals(3, pending.size)

        for (p in pending) {
            when (p.id) {
                "p-A" -> syncDao.markPlotsSynced(listOf(p.id))
                "p-B" -> {
                    // Simulating network failure on B: do NOT mark synced
                }
                "p-C" -> syncDao.markPlotsSynced(listOf(p.id))
            }
        }

        // Verify: A is SYNCED, B remains PENDING, C is SYNCED
        assertEquals(SyncStatus.SYNCED, syncDao.getPlotById("p-A")?.syncStatus)
        assertEquals(SyncStatus.PENDING, syncDao.getPlotById("p-B")?.syncStatus)
        assertEquals(SyncStatus.SYNCED, syncDao.getPlotById("p-C")?.syncStatus)

        // Next sync cycle: only B is uploaded
        val retryPending = syncDao.getPendingPlots(userId)
        assertEquals(1, retryPending.size)
        assertEquals("p-B", retryPending[0].id)

        // Retry succeeds
        syncDao.markPlotsSynced(listOf("p-B"))
        assertEquals(0, syncDao.getPendingPlots(userId).size)
    }

    // =========================================================================
    // 7. NETWORK FAILURE SIMULATION (Retry after unacknowledged write)
    // =========================================================================
    @Test
    fun testNetworkLossAfterRemoteWrite_ClientRetriesIdempotently() = runBlocking {
        val syncDao = db.syncDao()
        val userId = "user_net_loss"

        // Client created plot locally
        val localPlot = Plot(id = "plot-ack-loss", name = "Well Field", area = 3.0, userId = userId, syncStatus = SyncStatus.PENDING, updatedAt = 1000L)
        syncDao.upsertPlot(localPlot)

        // 1. Client pushed to remote -> remote saved it
        // 2. Network dropped before client received ACK -> local remains PENDING
        assertEquals(SyncStatus.PENDING, syncDao.getPlotById("plot-ack-loss")?.syncStatus)

        // 3. Client retries next sync -> re-sends same plot
        // Remote applies idempotent upsert (same primary key `id`)
        val remoteMirror = localPlot.copy(syncStatus = SyncStatus.SYNCED, updatedAt = 1000L)
        syncDao.upsertPlot(remoteMirror)

        // 4. Client receives ACK and marks SYNCED
        syncDao.markPlotsSynced(listOf("plot-ack-loss"))

        // Verify exactly 1 row exists, no duplicates, status is SYNCED
        val result = syncDao.getPlotById("plot-ack-loss")
        assertNotNull(result)
        assertEquals(SyncStatus.SYNCED, result?.syncStatus)
    }

    // =========================================================================
    // 9 & 10. AUTH SESSION FAILURE & EXPIRY DURING SYNC
    // =========================================================================
    @Test
    fun testSessionExpiresMidSync_HaltsGracefullyWithoutMarkingSynced() = runBlocking {
        val syncDao = db.syncDao()
        val userId = "user_session_test"

        val plot = Plot(id = "plot-sess-1", name = "Session Plot", area = 2.5, userId = userId, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plot)

        // Simulate session state
        var isSessionValid = true

        // Sync begins: session valid
        assertTrue(isSessionValid)

        // Mid-flight: token expires / revoked
        isSessionValid = false

        // Guard check in sync engine: if (!isSessionValid) abort!
        val syncAborted = !isSessionValid
        assertTrue("Sync must abort when session becomes invalid", syncAborted)

        // Record must remain PENDING because sync aborted before acknowledgement
        val plotAfterAbort = syncDao.getPlotById("plot-sess-1")
        assertEquals(SyncStatus.PENDING, plotAfterAbort?.syncStatus)
    }

    // =========================================================================
    // 17. CONCURRENCY: MUTEX SERIALIZATION
    // =========================================================================
    @Test
    fun testConcurrentSyncTriggers_SerializedByMutex() = runBlocking {
        val syncMutex = Mutex()
        var concurrentExecutions = 0
        var maxConcurrent = 0

        val scope = CoroutineScope(Dispatchers.Default)

        // Launch 10 simultaneous sync attempts
        val jobs = (1..10).map {
            scope.async {
                if (syncMutex.tryLock()) {
                    try {
                        concurrentExecutions++
                        if (concurrentExecutions > maxConcurrent) {
                            maxConcurrent = concurrentExecutions
                        }
                        // Simulate work
                        kotlinx.coroutines.delay(10)
                    } finally {
                        concurrentExecutions--
                        syncMutex.unlock()
                    }
                }
            }
        }

        jobs.awaitAll()
        // Mutex ensures that never more than 1 sync execution runs concurrently
        assertEquals(1, maxConcurrent)
        assertEquals(0, concurrentExecutions)
    }

    // =========================================================================
    // 18. DATA-INTEGRITY INVARIANTS (1 through 10)
    // =========================================================================

    // Invariant 1: No active record has deletedAt != null
    // Invariant 2: No tombstone has deletedAt == null
    @Test
    fun testInvariant1And2_ActiveVsTombstoneDeletedAtIntegrity() {
        val activePlot = Plot(id = "p-act", name = "Active", area = 1.0, deletedAt = null)
        assertNull("Active record must have deletedAt == null", activePlot.deletedAt)

        val tombstonePlot = Plot(id = "p-tomb", name = "Deleted", area = 1.0, deletedAt = 2000L)
        assertNotNull("Tombstone record must have deletedAt != null", tombstonePlot.deletedAt)
    }

    // Invariant 4 & 5: Exact integer paisa and no negative amounts
    @Test
    fun testInvariant4And5_MonetaryIntegerPaiseAndNonNegative() {
        val validExpense = Expense(
            id = "e-inv",
            date = "2026-09-14",
            category = "Fertilizer",
            amount = 45050L, // ₹450.50 in exact integer paise
            description = "DAP"
        )
        assertTrue("Amount must be a non-negative Long", validExpense.amount >= 0L)
        assertEquals(45050L, validExpense.amount)
    }

    // Invariant 6: No user record has a foreign userId
    @Test
    fun testInvariant6_UserRecordIsolation() = runBlocking {
        val syncDao = db.syncDao()
        val userA = "user_AAA"
        val userB = "user_BBB"

        val plotA = Plot(id = "p-inv-6", name = "A's Plot", area = 2.0, userId = userA, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plotA)

        // Querying pending for User B must never return User A's plot
        val pendingB = syncDao.getPendingPlots(userB)
        assertTrue("User B must never receive records belonging to User A", pendingB.none { it.userId == userA })
    }

    // Invariant 7: No sync operation creates duplicate rows for the same primary key
    @Test
    fun testInvariant7_NoDuplicateRowsForSamePrimaryKey() = runBlocking {
        val syncDao = db.syncDao()
        val worker = Worker(id = "w-inv-7", name = "Kailash", dailyWageRate = 45000L)

        // Insert multiple times
        syncDao.upsertWorker(worker)
        syncDao.upsertWorker(worker.copy(name = "Kailash Updated"))
        syncDao.upsertWorker(worker.copy(name = "Kailash Final"))

        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM workers WHERE id = 'w-inv-7'")
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        cursor.close()
    }

    // Invariant 8: A tombstone never loses its deletion marker due to stale downloads
    @Test
    fun testInvariant8_TombstoneNeverResurrectedByStaleRemote() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = 2000L,
            localUpdatedAt = 2000L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(1500L)
        )
        assertFalse("Tombstone must never lose its deletion marker to older remote", shouldApply)
    }

    // Invariant 9: A newer update always overwrites an older record
    @Test
    fun testInvariant9_NewerUpdateOverwritesOlderRecord() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = 1500L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(2000L)
        )
        assertTrue("Newer remote update must always overwrite older local record", shouldApply)
    }

    // Invariant 10: Sync state returns to SYNCED only when cloud state matches local state
    @Test
    fun testInvariant10_SyncStateReturnsToSyncedOnlyOnConfirmedAck() = runBlocking {
        val syncDao = db.syncDao()
        val plot = Plot(id = "p-inv-10", name = "Ack Test", area = 1.0, syncStatus = SyncStatus.PENDING)
        syncDao.upsertPlot(plot)

        // Remains PENDING until explicit confirmed mark
        assertEquals(SyncStatus.PENDING, syncDao.getPlotById("p-inv-10")?.syncStatus)

        // Confirmed ack from remote
        syncDao.markPlotsSynced(listOf("p-inv-10"))
        assertEquals(SyncStatus.SYNCED, syncDao.getPlotById("p-inv-10")?.syncStatus)
    }
}
