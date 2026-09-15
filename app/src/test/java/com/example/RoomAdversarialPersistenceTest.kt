package com.example

import android.database.sqlite.SQLiteConstraintException
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Real Room Database adversarial and persistence integration tests.
 * Runs on real SQLite via Robolectric with foreign keys enabled.
 */
@RunWith(RobolectricTestRunner::class)
class RoomAdversarialPersistenceTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // =========================================================================
    // 11. ACCOUNT SWITCHING REAL DATABASE TEST
    // =========================================================================
    @Test
    fun testAccountSwitching_CompletePersistenceIsolation() = runBlocking {
        val userA = "farmer_uuid_aaa"
        val userB = "farmer_uuid_bbb"
        val syncDao = db.syncDao()
        val plotDao = db.plotDao()
        val workerDao = db.workerDao()
        val expenseDao = db.expenseDao()

        // Step 1: User A creates records
        val plotA = Plot(id = "plot-a-1", name = "North Orchard", area = 3.5, userId = userA, syncStatus = SyncStatus.PENDING)
        val workerA = Worker(id = "worker-a-1", name = "Tukaram", dailyWageRate = 45000L, userId = userA, syncStatus = SyncStatus.PENDING)
        val expenseA = Expense(id = "expense-a-1", date = "2026-09-14", category = "Diesel", amount = 150000L, description = "Tractor fuel", userId = userA, syncStatus = SyncStatus.PENDING)

        syncDao.upsertPlot(plotA)
        syncDao.upsertWorker(workerA)
        syncDao.upsertExpense(expenseA)

        // Step 2: Simulate User A sign out, User B signs in.
        // User B's sync engine queries pending uploads for User B
        val pendingPlotsForB = syncDao.getPendingPlots(userB)
        val pendingWorkersForB = syncDao.getPendingWorkers(userB)
        val pendingExpensesForB = syncDao.getPendingExpenses(userB)

        // Verify User B cannot see or upload User A's pending records
        assertEquals(0, pendingPlotsForB.size)
        assertEquals(0, pendingWorkersForB.size)
        assertEquals(0, pendingExpensesForB.size)

        // Step 3: User B creates their own records
        val plotB = Plot(id = "plot-b-1", name = "East Sugarcane", area = 5.0, userId = userB, syncStatus = SyncStatus.PENDING)
        val workerB = Worker(id = "worker-b-1", name = "Pandurang", dailyWageRate = 50000L, userId = userB, syncStatus = SyncStatus.PENDING)
        val expenseB = Expense(id = "expense-b-1", date = "2026-09-14", category = "Seeds", amount = 85000L, description = "Cotton seeds", userId = userB, syncStatus = SyncStatus.PENDING)

        syncDao.upsertPlot(plotB)
        syncDao.upsertWorker(workerB)
        syncDao.upsertExpense(expenseB)

        // Step 4: Verify User A's sync query only pulls User A's records, never User B's
        val pendingPlotsForA = syncDao.getPendingPlots(userA)
        val pendingWorkersForA = syncDao.getPendingWorkers(userA)
        val pendingExpensesForA = syncDao.getPendingExpenses(userA)

        assertEquals(1, pendingPlotsForA.size)
        assertEquals("plot-a-1", pendingPlotsForA[0].id)
        assertEquals(1, pendingWorkersForA.size)
        assertEquals("worker-a-1", pendingWorkersForA[0].id)
        assertEquals(1, pendingExpensesForA.size)
        assertEquals("expense-a-1", pendingExpensesForA[0].id)

        // Verify User B's sync query only pulls User B's records
        val finalPendingPlotsForB = syncDao.getPendingPlots(userB)
        assertEquals(1, finalPendingPlotsForB.size)
        assertEquals("plot-b-1", finalPendingPlotsForB[0].id)
    }

    // =========================================================================
    // 12. FIRST-LOGIN LEGACY DATA TEST
    // =========================================================================
    @Test
    fun testFirstLoginLegacyDataEstablishment_And_AccountBIsolation() = runBlocking {
        val syncDao = db.syncDao()
        val userA = "first_user_alice_uuid"
        val userB = "second_user_bob_uuid"

        // Step 1: Fresh local database with unassigned legacy offline records (userId == null)
        val legacyPlot = Plot(id = "legacy-plot-1", name = "Old Family Field", area = 2.0, userId = null, syncStatus = SyncStatus.PENDING)
        val legacyWorker = Worker(id = "legacy-worker-1", name = "Mahadev", dailyWageRate = 40000L, userId = null, syncStatus = SyncStatus.PENDING)
        val legacyExpense = Expense(id = "legacy-exp-1", date = "2026-09-01", category = "Seeds", amount = 50000L, description = "Bajra seeds", userId = null, syncStatus = SyncStatus.PENDING)

        syncDao.upsertPlot(legacyPlot)
        syncDao.upsertWorker(legacyWorker)
        syncDao.upsertExpense(legacyExpense)

        // Verify unassigned count
        val unassignedCount = syncDao.getUnassignedRecordsCount()
        assertTrue("Unassigned count must be at least 3", unassignedCount >= 3)

        // Step 2: First user (User A) signs in and associates unassigned records
        syncDao.associateAllLocalRecordsToUser(userA)

        // Verify records are now owned by User A
        val plotAfterClaim = syncDao.getPlotById("legacy-plot-1")
        val workerAfterClaim = syncDao.getWorkerById("legacy-worker-1")
        val expenseAfterClaim = syncDao.getExpenseById("legacy-exp-1")

        assertEquals(userA, plotAfterClaim?.userId)
        assertEquals(userA, workerAfterClaim?.userId)
        assertEquals(userA, expenseAfterClaim?.userId)
        assertEquals(0, syncDao.getUnassignedRecordsCount())

        // Step 3: User A signs out, User B signs in.
        // User B attempts to claim unassigned records (should be 0)
        assertEquals(0, syncDao.getUnassignedRecordsCount())
        syncDao.associateAllLocalRecordsToUser(userB)

        // Step 4: Verify User A's records were NOT reassigned to User B!
        val plotAfterBobLogin = syncDao.getPlotById("legacy-plot-1")
        assertEquals("User A's claimed records must NOT be reassigned to User B", userA, plotAfterBobLogin?.userId)

        // User B's pending query returns 0
        val bobPending = syncDao.getPendingPlots(userB)
        assertEquals(0, bobPending.size)
    }

    // =========================================================================
    // 16. FOREIGN-KEY FAILURE TESTING
    // =========================================================================
    @Test
    fun testForeignKey_ChildWithoutParent_Fails() = runBlocking {
        // Attempting to insert CropAssignment for nonexistent plot must fail
        val crop = CropAssignment(
            id = "crop-orphan",
            plotId = "non-existent-plot-999",
            cropName = "Wheat",
            plantingDate = "2026-09-14",
            expectedHarvestDate = "2026-12-15"
        )

        try {
            db.cropAssignmentDao().insertCrop(crop)
            fail("Inserting CropAssignment without parent Plot must throw SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            // Expected
            assertTrue(true)
        }
    }

    @Test
    fun testForeignKey_DeleteParentWithRestrictedChild_Fails() = runBlocking {
        // Create Plot
        val plot = Plot(id = "plot-fk-1", name = "Test Plot", area = 2.0)
        db.plotDao().insertPlot(plot)

        // Create CropAssignment referencing Plot (onDelete = RESTRICT)
        val crop = CropAssignment(
            id = "crop-fk-1",
            plotId = "plot-fk-1",
            cropName = "Soybean",
            plantingDate = "2026-06-01",
            expectedHarvestDate = "2026-10-01"
        )
        db.cropAssignmentDao().insertCrop(crop)

        // Hard deleting Plot directly via SQLite query must fail due to RESTRICT constraint
        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM plots WHERE id = 'plot-fk-1'")
            fail("Deleting Plot with active CropAssignment must fail due to RESTRICT constraint")
        } catch (e: SQLiteConstraintException) {
            // Expected
            assertTrue(true)
        }
    }

    @Test
    fun testForeignKey_DeleteParentWithCascadeChild_CascadesSuccessfully() = runBlocking {
        // Create Worker
        val worker = Worker(id = "worker-fk-1", name = "Baban", dailyWageRate = 45000L)
        db.workerDao().insertWorker(worker)

        // Create Attendance referencing Worker (onDelete = CASCADE)
        val attendance = Attendance(workerId = "worker-fk-1", date = "2026-09-14", status = "PRESENT")
        db.attendanceDao().recordAttendance(attendance)

        // Create Transaction referencing Worker (onDelete = CASCADE)
        val tx = WorkerTransaction(id = "tx-fk-1", workerId = "worker-fk-1", type = "ADVANCE", amount = 10000L, date = "2026-09-14")
        db.workerTransactionDao().insertTransaction(tx)

        // Delete Worker directly from database
        db.openHelper.writableDatabase.execSQL("DELETE FROM workers WHERE id = 'worker-fk-1'")

        // Verify Attendance and Transaction were CASCADE deleted
        val cursorAtt = db.openHelper.readableDatabase.query("SELECT * FROM attendance WHERE workerId = 'worker-fk-1'")
        assertEquals(0, cursorAtt.count)
        cursorAtt.close()

        val cursorTx = db.openHelper.readableDatabase.query("SELECT * FROM worker_transactions WHERE workerId = 'worker-fk-1'")
        assertEquals(0, cursorTx.count)
        cursorTx.close()
    }

    @Test
    fun testForeignKey_NullifyAllowedRelationship_SetsNullOnParentDelete() = runBlocking {
        // Create Plot
        val plot = Plot(id = "plot-null-1", name = "Vegetable Bed", area = 1.0)
        db.plotDao().insertPlot(plot)

        // Create Expense referencing Plot (onDelete = SET NULL)
        val expense = Expense(id = "exp-null-1", date = "2026-09-14", category = "Fertilizer", amount = 25000L, description = "Organic manure", plotId = "plot-null-1")
        db.expenseDao().insertExpense(expense)

        // Create Task referencing Plot (onDelete = SET NULL)
        val task = DailyTask(id = "task-null-1", date = "2026-09-14", description = "Watering", plotId = "plot-null-1")
        db.dailyTaskDao().insertTask(task)

        // Delete Plot
        db.openHelper.writableDatabase.execSQL("DELETE FROM plots WHERE id = 'plot-null-1'")

        // Verify Expense and Task still exist, but plotId is now null
        val expAfter = db.syncDao().getExpenseById("exp-null-1")
        assertNotNull(expAfter)
        assertNull(expAfter?.plotId)

        val taskAfter = db.syncDao().getTaskById("task-null-1")
        assertNotNull(taskAfter)
        assertNull(taskAfter?.plotId)
    }
}
