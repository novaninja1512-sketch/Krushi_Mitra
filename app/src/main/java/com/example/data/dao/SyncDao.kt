package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Attendance
import com.example.data.model.CropAssignment
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.SyncStatus
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    // --- 1. Pending Local Changes Retrieval (for cloud upload, strictly scoped by user) ---

    @Query("SELECT * FROM plots WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingPlots(userId: String, pendingStatus: String = SyncStatus.PENDING): List<Plot>

    @Query("SELECT * FROM crop_assignments WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingCrops(userId: String, pendingStatus: String = SyncStatus.PENDING): List<CropAssignment>

    @Query("SELECT * FROM yield_records WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingYields(userId: String, pendingStatus: String = SyncStatus.PENDING): List<YieldRecord>

    @Query("SELECT * FROM workers WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingWorkers(userId: String, pendingStatus: String = SyncStatus.PENDING): List<Worker>

    @Query("SELECT * FROM attendance WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingAttendance(userId: String, pendingStatus: String = SyncStatus.PENDING): List<Attendance>

    @Query("SELECT * FROM worker_transactions WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingTransactions(userId: String, pendingStatus: String = SyncStatus.PENDING): List<WorkerTransaction>

    @Query("SELECT * FROM daily_tasks WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingTasks(userId: String, pendingStatus: String = SyncStatus.PENDING): List<DailyTask>

    @Query("SELECT * FROM task_worker_assignments WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingTaskWorkers(userId: String, pendingStatus: String = SyncStatus.PENDING): List<TaskWorkerAssignment>

    @Query("SELECT * FROM expenses WHERE syncStatus = :pendingStatus AND userId = :userId")
    suspend fun getPendingExpenses(userId: String, pendingStatus: String = SyncStatus.PENDING): List<Expense>

    // --- Query for detecting unassigned legacy local records (prior to first-login migration) ---
    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM plots WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM crop_assignments WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM yield_records WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM workers WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM attendance WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM worker_transactions WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM daily_tasks WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM task_worker_assignments WHERE userId IS NULL OR userId = '') +
            (SELECT COUNT(*) FROM expenses WHERE userId IS NULL OR userId = '')
        )
    """)
    suspend fun getUnassignedRecordsCount(): Int

    // --- 2. Mark Synced After Upload (Strictly user-scoped to authenticated user) ---

    @Query("UPDATE plots SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markPlotsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE crop_assignments SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markCropAssignmentsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    suspend fun markCropsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED) =
        markCropAssignmentsSynced(userId, ids, syncedStatus)

    @Query("UPDATE yield_records SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markYieldRecordsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    suspend fun markYieldsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED) =
        markYieldRecordsSynced(userId, ids, syncedStatus)

    @Query("UPDATE workers SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markWorkersSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE attendance SET syncStatus = :syncedStatus WHERE workerId = :workerId AND date = :date AND userId = :userId")
    suspend fun markAttendanceSynced(userId: String, workerId: String, date: String, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE worker_transactions SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markWorkerTransactionsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    suspend fun markTransactionsSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED) =
        markWorkerTransactionsSynced(userId, ids, syncedStatus)

    @Query("UPDATE daily_tasks SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markDailyTasksSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    suspend fun markTasksSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED) =
        markDailyTasksSynced(userId, ids, syncedStatus)

    @Query("UPDATE task_worker_assignments SET syncStatus = :syncedStatus WHERE taskId = :taskId AND workerId = :workerId AND userId = :userId")
    suspend fun markTaskWorkerAssignmentsSynced(userId: String, taskId: String, workerId: String, syncedStatus: String = SyncStatus.SYNCED)

    suspend fun markTaskWorkerSynced(userId: String, taskId: String, workerId: String, syncedStatus: String = SyncStatus.SYNCED) =
        markTaskWorkerAssignmentsSynced(userId, taskId, workerId, syncedStatus)

    @Query("UPDATE expenses SET syncStatus = :syncedStatus WHERE id IN (:ids) AND userId = :userId")
    suspend fun markExpensesSynced(userId: String, ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    // --- 3. First Login / Migration of existing local records to authenticated user ---

    @Query("UPDATE plots SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalPlots(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE crop_assignments SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalCrops(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE yield_records SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalYields(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE workers SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalWorkers(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE attendance SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalAttendance(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE worker_transactions SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalTransactions(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE daily_tasks SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalTasks(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE task_worker_assignments SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalTaskWorkers(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Query("UPDATE expenses SET userId = :userId, syncStatus = :pendingStatus WHERE userId IS NULL OR userId = ''")
    suspend fun associateLocalExpenses(userId: String, pendingStatus: String = SyncStatus.PENDING)

    @Transaction
    suspend fun associateAllLocalRecordsToUser(userId: String) {
        associateLocalPlots(userId)
        associateLocalCrops(userId)
        associateLocalYields(userId)
        associateLocalWorkers(userId)
        associateLocalAttendance(userId)
        associateLocalTransactions(userId)
        associateLocalTasks(userId)
        associateLocalTaskWorkers(userId)
        associateLocalExpenses(userId)
    }

    // --- 4. Remote to Local Merging (Upserting with REPLACE) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlot(plot: Plot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrop(crop: CropAssignment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertYield(yieldRecord: YieldRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorker(worker: Worker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: WorkerTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: DailyTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskWorker(assignment: TaskWorkerAssignment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: Expense)

    // Lookup individual local entities for updatedAt comparison
    @Query("SELECT * FROM plots WHERE id = :id LIMIT 1")
    suspend fun getPlotById(id: String): Plot?

    @Query("SELECT * FROM crop_assignments WHERE id = :id LIMIT 1")
    suspend fun getCropById(id: String): CropAssignment?

    @Query("SELECT * FROM yield_records WHERE id = :id LIMIT 1")
    suspend fun getYieldById(id: String): YieldRecord?

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: String): Worker?

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getAttendance(workerId: String, date: String): Attendance?

    @Query("SELECT * FROM worker_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): WorkerTransaction?

    @Query("SELECT * FROM daily_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): DailyTask?

    @Query("SELECT * FROM task_worker_assignments WHERE taskId = :taskId AND workerId = :workerId LIMIT 1")
    suspend fun getTaskWorker(taskId: String, workerId: String): TaskWorkerAssignment?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): Expense?

    // --- 5. Pending Changes Counter Flow (Strictly scoped by userId) ---
    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM plots WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM crop_assignments WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM yield_records WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM workers WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM attendance WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM worker_transactions WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM daily_tasks WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM task_worker_assignments WHERE syncStatus = 'PENDING' AND userId = :userId) +
            (SELECT COUNT(*) FROM expenses WHERE syncStatus = 'PENDING' AND userId = :userId)
        )
    """)
    fun getPendingCountForUserFlow(userId: String): Flow<Int>

    // --- 6. User-Scoped Local Data Verification Queries ---
    @Query("SELECT * FROM plots WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getPlotsForUser(userId: String): List<Plot>

    @Query("SELECT * FROM workers WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getWorkersForUser(userId: String): List<Worker>

    @Query("SELECT * FROM crop_assignments WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getCropsForUser(userId: String): List<CropAssignment>

    @Query("SELECT * FROM yield_records WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getYieldsForUser(userId: String): List<YieldRecord>

    @Query("SELECT * FROM attendance WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getAttendanceForUser(userId: String): List<Attendance>

    @Query("SELECT * FROM worker_transactions WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getTransactionsForUser(userId: String): List<WorkerTransaction>

    @Query("SELECT * FROM daily_tasks WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getTasksForUser(userId: String): List<DailyTask>

    @Query("SELECT * FROM task_worker_assignments WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getTaskWorkersForUser(userId: String): List<TaskWorkerAssignment>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getExpensesForUser(userId: String): List<Expense>
}
