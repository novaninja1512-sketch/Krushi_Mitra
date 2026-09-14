package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Attendance
import com.example.data.model.CropAssignment
import com.example.data.model.CropWithPlot
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.TaskWithDetails
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord
import com.example.data.model.YieldWithCrop
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE (:includeArchived = 1 OR archived = 0) AND deletedAt IS NULL ORDER BY name ASC")
    fun getWorkersFlow(includeArchived: Boolean): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getWorkerByIdFlow(id: String): Flow<Worker?>

    @Query("SELECT * FROM workers WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getWorkerById(id: String): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("UPDATE workers SET archived = :archived, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setWorkerArchived(id: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE workers SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteWorker(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date AND deletedAt IS NULL")
    fun getAttendanceForDateFlow(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId AND deletedAt IS NULL ORDER BY date DESC")
    fun getAttendanceForWorkerFlow(workerId: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendanceBatch(list: List<Attendance>)

    @Query("UPDATE attendance SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE workerId = :workerId AND date = :date")
    suspend fun deleteAttendance(workerId: String, date: String, now: Long = System.currentTimeMillis())
}

@Dao
interface WorkerTransactionDao {
    @Query("SELECT * FROM worker_transactions WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<WorkerTransaction>>

    @Query("SELECT * FROM worker_transactions WHERE workerId = :workerId AND deletedAt IS NULL ORDER BY date DESC")
    fun getTransactionsForWorkerFlow(workerId: String): Flow<List<WorkerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WorkerTransaction)

    @Query("UPDATE worker_transactions SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteTransaction(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface PlotDao {
    @Query("SELECT * FROM plots WHERE (:includeArchived = 1 OR archived = 0) AND deletedAt IS NULL ORDER BY name ASC")
    fun getPlotsFlow(includeArchived: Boolean): Flow<List<Plot>>

    @Query("SELECT * FROM plots WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getPlotByIdFlow(id: String): Flow<Plot?>

    @Query("SELECT * FROM plots WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getPlotById(id: String): Plot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlot(plot: Plot)

    @Update
    suspend fun updatePlot(plot: Plot)

    @Query("UPDATE plots SET archived = :archived, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setPlotArchived(id: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE plots SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deletePlot(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface CropAssignmentDao {
    @Query("SELECT * FROM crop_assignments WHERE deletedAt IS NULL ORDER BY plantingDate DESC")
    fun getAllCropsFlow(): Flow<List<CropAssignment>>

    @Transaction
    @Query("SELECT * FROM crop_assignments WHERE deletedAt IS NULL ORDER BY plantingDate DESC")
    fun getCropsWithPlotFlow(): Flow<List<CropWithPlot>>

    @Query("SELECT * FROM crop_assignments WHERE plotId = :plotId AND deletedAt IS NULL ORDER BY plantingDate DESC")
    fun getCropsForPlotFlow(plotId: String): Flow<List<CropAssignment>>

    @Transaction
    @Query("SELECT * FROM crop_assignments WHERE status = 'ACTIVE' AND deletedAt IS NULL ORDER BY expectedHarvestDate ASC")
    fun getActiveCropsWithPlotFlow(): Flow<List<CropWithPlot>>

    @Query("SELECT * FROM crop_assignments WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getCropByIdFlow(id: String): Flow<CropAssignment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropAssignment)

    @Update
    suspend fun updateCrop(crop: CropAssignment)

    @Query("UPDATE crop_assignments SET status = :status, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateCropStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE crop_assignments SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteCrop(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface YieldRecordDao {
    @Query("SELECT * FROM yield_records WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getAllYieldsFlow(): Flow<List<YieldRecord>>

    @Transaction
    @Query("SELECT * FROM yield_records WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getYieldsWithCropFlow(): Flow<List<YieldWithCrop>>

    @Query("SELECT * FROM yield_records WHERE cropAssignmentId = :cropId AND deletedAt IS NULL ORDER BY date DESC")
    fun getYieldsForCropFlow(cropId: String): Flow<List<YieldRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYield(yieldRecord: YieldRecord)

    @Update
    suspend fun updateYield(yieldRecord: YieldRecord)

    @Query("UPDATE yield_records SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteYield(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface DailyTaskDao {
    @Transaction
    @Query("SELECT * FROM daily_tasks WHERE deletedAt IS NULL ORDER BY date DESC, id DESC")
    fun getAllTasksWithDetailsFlow(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_tasks WHERE date = :date AND deletedAt IS NULL ORDER BY isCompleted ASC, id DESC")
    fun getTasksWithDetailsForDateFlow(date: String): Flow<List<TaskWithDetails>>

    @Query("SELECT * FROM daily_tasks WHERE plotId = :plotId AND deletedAt IS NULL ORDER BY date DESC")
    fun getTasksForPlotFlow(plotId: String): Flow<List<DailyTask>>

    @Transaction
    @Query("SELECT * FROM daily_tasks WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getTaskWithDetailsByIdFlow(id: String): Flow<TaskWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Query("UPDATE daily_tasks SET isCompleted = :isCompleted, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_tasks SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteTask(id: String, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskWorkers(assignments: List<TaskWorkerAssignment>)

    @Query("UPDATE task_worker_assignments SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE taskId = :taskId")
    suspend fun deleteTaskWorkers(taskId: String, now: Long = System.currentTimeMillis())

    @Transaction
    @Query("""
        SELECT t.* FROM daily_tasks t 
        INNER JOIN task_worker_assignments a ON t.id = a.taskId 
        WHERE a.workerId = :workerId AND t.deletedAt IS NULL AND a.deletedAt IS NULL
        ORDER BY t.date DESC
    """)
    fun getTasksForWorkerFlow(workerId: String): Flow<List<DailyTask>>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE plotId = :plotId AND deletedAt IS NULL ORDER BY date DESC")
    fun getExpensesForPlotFlow(plotId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category AND deletedAt IS NULL ORDER BY date DESC")
    fun getExpensesByCategoryFlow(category: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("UPDATE expenses SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deleteExpense(id: String, now: Long = System.currentTimeMillis())
}
