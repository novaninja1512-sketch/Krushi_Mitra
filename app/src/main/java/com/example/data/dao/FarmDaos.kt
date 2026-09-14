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
    @Query("SELECT * FROM workers WHERE (:includeArchived = 1 OR archived = 0) ORDER BY name ASC")
    fun getWorkersFlow(includeArchived: Boolean): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    fun getWorkerByIdFlow(id: String): Flow<Worker?>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: String): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("UPDATE workers SET archived = :archived WHERE id = :id")
    suspend fun setWorkerArchived(id: String, archived: Boolean)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorker(id: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDateFlow(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceForWorkerFlow(workerId: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendanceBatch(list: List<Attendance>)

    @Query("DELETE FROM attendance WHERE workerId = :workerId AND date = :date")
    suspend fun deleteAttendance(workerId: String, date: String)
}

@Dao
interface WorkerTransactionDao {
    @Query("SELECT * FROM worker_transactions ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<WorkerTransaction>>

    @Query("SELECT * FROM worker_transactions WHERE workerId = :workerId ORDER BY date DESC")
    fun getTransactionsForWorkerFlow(workerId: String): Flow<List<WorkerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WorkerTransaction)

    @Query("DELETE FROM worker_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}

@Dao
interface PlotDao {
    @Query("SELECT * FROM plots WHERE (:includeArchived = 1 OR archived = 0) ORDER BY name ASC")
    fun getPlotsFlow(includeArchived: Boolean): Flow<List<Plot>>

    @Query("SELECT * FROM plots WHERE id = :id LIMIT 1")
    fun getPlotByIdFlow(id: String): Flow<Plot?>

    @Query("SELECT * FROM plots WHERE id = :id LIMIT 1")
    suspend fun getPlotById(id: String): Plot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlot(plot: Plot)

    @Update
    suspend fun updatePlot(plot: Plot)

    @Query("UPDATE plots SET archived = :archived WHERE id = :id")
    suspend fun setPlotArchived(id: String, archived: Boolean)

    @Query("DELETE FROM plots WHERE id = :id")
    suspend fun deletePlot(id: String)
}

@Dao
interface CropAssignmentDao {
    @Query("SELECT * FROM crop_assignments ORDER BY plantingDate DESC")
    fun getAllCropsFlow(): Flow<List<CropAssignment>>

    @Transaction
    @Query("SELECT * FROM crop_assignments ORDER BY plantingDate DESC")
    fun getCropsWithPlotFlow(): Flow<List<CropWithPlot>>

    @Query("SELECT * FROM crop_assignments WHERE plotId = :plotId ORDER BY plantingDate DESC")
    fun getCropsForPlotFlow(plotId: String): Flow<List<CropAssignment>>

    @Transaction
    @Query("SELECT * FROM crop_assignments WHERE status = 'ACTIVE' ORDER BY expectedHarvestDate ASC")
    fun getActiveCropsWithPlotFlow(): Flow<List<CropWithPlot>>

    @Query("SELECT * FROM crop_assignments WHERE id = :id LIMIT 1")
    fun getCropByIdFlow(id: String): Flow<CropAssignment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropAssignment)

    @Update
    suspend fun updateCrop(crop: CropAssignment)

    @Query("UPDATE crop_assignments SET status = :status WHERE id = :id")
    suspend fun updateCropStatus(id: String, status: String)

    @Query("DELETE FROM crop_assignments WHERE id = :id")
    suspend fun deleteCrop(id: String)
}

@Dao
interface YieldRecordDao {
    @Query("SELECT * FROM yield_records ORDER BY date DESC")
    fun getAllYieldsFlow(): Flow<List<YieldRecord>>

    @Transaction
    @Query("SELECT * FROM yield_records ORDER BY date DESC")
    fun getYieldsWithCropFlow(): Flow<List<YieldWithCrop>>

    @Query("SELECT * FROM yield_records WHERE cropAssignmentId = :cropId ORDER BY date DESC")
    fun getYieldsForCropFlow(cropId: String): Flow<List<YieldRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYield(yieldRecord: YieldRecord)

    @Update
    suspend fun updateYield(yieldRecord: YieldRecord)

    @Query("DELETE FROM yield_records WHERE id = :id")
    suspend fun deleteYield(id: String)
}

@Dao
interface DailyTaskDao {
    @Transaction
    @Query("SELECT * FROM daily_tasks ORDER BY date DESC, id DESC")
    fun getAllTasksWithDetailsFlow(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_tasks WHERE date = :date ORDER BY isCompleted ASC, id DESC")
    fun getTasksWithDetailsForDateFlow(date: String): Flow<List<TaskWithDetails>>

    @Query("SELECT * FROM daily_tasks WHERE plotId = :plotId ORDER BY date DESC")
    fun getTasksForPlotFlow(plotId: String): Flow<List<DailyTask>>

    @Transaction
    @Query("SELECT * FROM daily_tasks WHERE id = :id LIMIT 1")
    fun getTaskWithDetailsByIdFlow(id: String): Flow<TaskWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Query("UPDATE daily_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskWorkers(assignments: List<TaskWorkerAssignment>)

    @Query("DELETE FROM task_worker_assignments WHERE taskId = :taskId")
    suspend fun deleteTaskWorkers(taskId: String)

    @Transaction
    @Query("""
        SELECT t.* FROM daily_tasks t 
        INNER JOIN task_worker_assignments a ON t.id = a.taskId 
        WHERE a.workerId = :workerId 
        ORDER BY t.date DESC
    """)
    fun getTasksForWorkerFlow(workerId: String): Flow<List<DailyTask>>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE plotId = :plotId ORDER BY date DESC")
    fun getExpensesForPlotFlow(plotId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategoryFlow(category: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)
}
