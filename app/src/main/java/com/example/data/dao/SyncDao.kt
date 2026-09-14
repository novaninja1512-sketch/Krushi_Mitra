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

    // --- 1. Pending Local Changes Retrieval (for cloud upload) ---

    @Query("SELECT * FROM plots WHERE syncStatus = :pendingStatus")
    suspend fun getPendingPlots(pendingStatus: String = SyncStatus.PENDING): List<Plot>

    @Query("SELECT * FROM crop_assignments WHERE syncStatus = :pendingStatus")
    suspend fun getPendingCrops(pendingStatus: String = SyncStatus.PENDING): List<CropAssignment>

    @Query("SELECT * FROM yield_records WHERE syncStatus = :pendingStatus")
    suspend fun getPendingYields(pendingStatus: String = SyncStatus.PENDING): List<YieldRecord>

    @Query("SELECT * FROM workers WHERE syncStatus = :pendingStatus")
    suspend fun getPendingWorkers(pendingStatus: String = SyncStatus.PENDING): List<Worker>

    @Query("SELECT * FROM attendance WHERE syncStatus = :pendingStatus")
    suspend fun getPendingAttendance(pendingStatus: String = SyncStatus.PENDING): List<Attendance>

    @Query("SELECT * FROM worker_transactions WHERE syncStatus = :pendingStatus")
    suspend fun getPendingTransactions(pendingStatus: String = SyncStatus.PENDING): List<WorkerTransaction>

    @Query("SELECT * FROM daily_tasks WHERE syncStatus = :pendingStatus")
    suspend fun getPendingTasks(pendingStatus: String = SyncStatus.PENDING): List<DailyTask>

    @Query("SELECT * FROM task_worker_assignments WHERE syncStatus = :pendingStatus")
    suspend fun getPendingTaskWorkers(pendingStatus: String = SyncStatus.PENDING): List<TaskWorkerAssignment>

    @Query("SELECT * FROM expenses WHERE syncStatus = :pendingStatus")
    suspend fun getPendingExpenses(pendingStatus: String = SyncStatus.PENDING): List<Expense>

    // --- 2. Mark Synced After Upload ---

    @Query("UPDATE plots SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markPlotsSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE crop_assignments SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markCropsSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE yield_records SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markYieldsSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE workers SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markWorkersSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE attendance SET syncStatus = :syncedStatus WHERE workerId = :workerId AND date = :date")
    suspend fun markAttendanceSynced(workerId: String, date: String, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE worker_transactions SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE daily_tasks SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markTasksSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE task_worker_assignments SET syncStatus = :syncedStatus WHERE taskId = :taskId AND workerId = :workerId")
    suspend fun markTaskWorkerSynced(taskId: String, workerId: String, syncedStatus: String = SyncStatus.SYNCED)

    @Query("UPDATE expenses SET syncStatus = :syncedStatus WHERE id IN (:ids)")
    suspend fun markExpensesSynced(ids: List<String>, syncedStatus: String = SyncStatus.SYNCED)

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

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): Expense?

    // --- 5. Pending Changes Counter Flow ---
    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM plots WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM crop_assignments WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM yield_records WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM workers WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM attendance WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM worker_transactions WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM daily_tasks WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM task_worker_assignments WHERE syncStatus = 'PENDING') +
            (SELECT COUNT(*) FROM expenses WHERE syncStatus = 'PENDING')
        )
    """)
    fun getPendingCountFlow(): Flow<Int>
}
