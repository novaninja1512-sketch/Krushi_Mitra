package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatuses
import com.example.data.model.CropAssignment
import com.example.data.model.CropWithPlot
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.PlotDetails
import com.example.data.model.TaskWithDetails
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.TransactionTypes
import com.example.data.model.Worker
import com.example.data.model.WorkerDetails
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord
import com.example.data.model.YieldWithCrop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FarmRepository(private val db: AppDatabase) {

    private val workerDao = db.workerDao()
    private val attendanceDao = db.attendanceDao()
    private val workerTransactionDao = db.workerTransactionDao()
    private val plotDao = db.plotDao()
    private val cropAssignmentDao = db.cropAssignmentDao()
    private val yieldRecordDao = db.yieldRecordDao()
    private val dailyTaskDao = db.dailyTaskDao()
    private val expenseDao = db.expenseDao()

    // Plots
    fun getPlots(includeArchived: Boolean = false): Flow<List<Plot>> =
        plotDao.getPlotsFlow(includeArchived)

    fun getPlotById(id: String): Flow<Plot?> = plotDao.getPlotByIdFlow(id)

    suspend fun insertPlot(plot: Plot) = plotDao.insertPlot(plot)

    suspend fun updatePlot(plot: Plot) = plotDao.updatePlot(plot)

    suspend fun archivePlot(id: String, archived: Boolean) = plotDao.setPlotArchived(id, archived)

    suspend fun deletePlot(id: String) = plotDao.deletePlot(id)

    fun getPlotDetails(plotId: String): Flow<PlotDetails?> {
        return combine(
            plotDao.getPlotByIdFlow(plotId),
            cropAssignmentDao.getCropsForPlotFlow(plotId),
            dailyTaskDao.getTasksForPlotFlow(plotId),
            expenseDao.getExpensesForPlotFlow(plotId)
        ) { plot, crops, tasks, expenses ->
            if (plot == null) null
            else PlotDetails(
                plot = plot,
                crops = crops,
                tasks = tasks,
                expenses = expenses,
                yields = emptyList() // Yields are queried per crop
            )
        }
    }

    // Crops
    fun getAllCrops(): Flow<List<CropAssignment>> = cropAssignmentDao.getAllCropsFlow()

    fun getCropsWithPlot(): Flow<List<CropWithPlot>> = cropAssignmentDao.getCropsWithPlotFlow()

    fun getActiveCropsWithPlot(): Flow<List<CropWithPlot>> =
        cropAssignmentDao.getActiveCropsWithPlotFlow()

    fun getCropsForPlot(plotId: String): Flow<List<CropAssignment>> =
        cropAssignmentDao.getCropsForPlotFlow(plotId)

    fun getCropById(id: String): Flow<CropAssignment?> = cropAssignmentDao.getCropByIdFlow(id)

    suspend fun insertCrop(crop: CropAssignment) = cropAssignmentDao.insertCrop(crop)

    suspend fun updateCrop(crop: CropAssignment) = cropAssignmentDao.updateCrop(crop)

    suspend fun updateCropStatus(id: String, status: String) =
        cropAssignmentDao.updateCropStatus(id, status)

    suspend fun deleteCrop(id: String) = cropAssignmentDao.deleteCrop(id)

    // Workers
    fun getWorkers(includeArchived: Boolean = false): Flow<List<Worker>> =
        workerDao.getWorkersFlow(includeArchived)

    fun getWorkerById(id: String): Flow<Worker?> = workerDao.getWorkerByIdFlow(id)

    suspend fun insertWorker(worker: Worker) = workerDao.insertWorker(worker)

    suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker)

    suspend fun archiveWorker(id: String, archived: Boolean) =
        workerDao.setWorkerArchived(id, archived)

    suspend fun deleteWorker(id: String) = workerDao.deleteWorker(id)

    fun getWorkerDetails(workerId: String): Flow<WorkerDetails?> {
        return combine(
            workerDao.getWorkerByIdFlow(workerId),
            attendanceDao.getAttendanceForWorkerFlow(workerId),
            workerTransactionDao.getTransactionsForWorkerFlow(workerId),
            dailyTaskDao.getTasksForWorkerFlow(workerId)
        ) { worker, attendanceList, transactions, tasks ->
            if (worker == null) null
            else {
                val present = attendanceList.count { it.status == AttendanceStatuses.PRESENT }
                val halfDay = attendanceList.count { it.status == AttendanceStatuses.HALF_DAY }
                val absent = attendanceList.count { it.status == AttendanceStatuses.ABSENT }

                val advanceTotal = transactions
                    .filter { it.type == TransactionTypes.ADVANCE }
                    .sumOf { it.amount }
                val salaryTotal = transactions
                    .filter { it.type == TransactionTypes.SALARY }
                    .sumOf { it.amount }

                WorkerDetails(
                    worker = worker,
                    presentCount = present,
                    halfDayCount = halfDay,
                    absentCount = absent,
                    totalAdvance = advanceTotal,
                    totalSalary = salaryTotal,
                    recentTransactions = transactions.take(15),
                    recentTasks = tasks.take(15)
                )
            }
        }
    }

    // Attendance
    fun getAttendanceForDate(date: String): Flow<List<Attendance>> =
        attendanceDao.getAttendanceForDateFlow(date)

    suspend fun recordAttendance(attendance: Attendance) =
        attendanceDao.recordAttendance(attendance)

    suspend fun recordAttendanceBatch(list: List<Attendance>) =
        attendanceDao.recordAttendanceBatch(list)

    // Worker Transactions (Salary / Advance)
    fun getAllTransactions(): Flow<List<WorkerTransaction>> =
        workerTransactionDao.getAllTransactionsFlow()

    fun getTransactionsForWorker(workerId: String): Flow<List<WorkerTransaction>> =
        workerTransactionDao.getTransactionsForWorkerFlow(workerId)

    suspend fun insertTransaction(transaction: WorkerTransaction) =
        workerTransactionDao.insertTransaction(transaction)

    suspend fun deleteTransaction(id: String) =
        workerTransactionDao.deleteTransaction(id)

    // Expenses
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpensesFlow()

    fun getExpensesForPlot(plotId: String): Flow<List<Expense>> =
        expenseDao.getExpensesForPlotFlow(plotId)

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(id: String) = expenseDao.deleteExpense(id)

    // Tasks
    fun getAllTasksWithDetails(): Flow<List<TaskWithDetails>> =
        dailyTaskDao.getAllTasksWithDetailsFlow()

    fun getTasksForDate(date: String): Flow<List<TaskWithDetails>> =
        dailyTaskDao.getTasksWithDetailsForDateFlow(date)

    suspend fun insertTaskWithWorkers(task: DailyTask, workerIds: List<String>) {
        dailyTaskDao.insertTask(task)
        if (workerIds.isNotEmpty()) {
            val assignments = workerIds.map { TaskWorkerAssignment(taskId = task.id, workerId = it) }
            dailyTaskDao.insertTaskWorkers(assignments)
        }
    }

    suspend fun updateTaskWithWorkers(task: DailyTask, workerIds: List<String>) {
        dailyTaskDao.updateTask(task)
        dailyTaskDao.deleteTaskWorkers(task.id)
        if (workerIds.isNotEmpty()) {
            val assignments = workerIds.map { TaskWorkerAssignment(taskId = task.id, workerId = it) }
            dailyTaskDao.insertTaskWorkers(assignments)
        }
    }

    suspend fun setTaskCompletion(id: String, isCompleted: Boolean) =
        dailyTaskDao.setTaskCompleted(id, isCompleted)

    suspend fun deleteTask(id: String) {
        dailyTaskDao.deleteTaskWorkers(id)
        dailyTaskDao.deleteTask(id)
    }

    // Yield
    fun getAllYieldsWithCrop(): Flow<List<YieldWithCrop>> =
        yieldRecordDao.getYieldsWithCropFlow()

    fun getYieldsForCrop(cropId: String): Flow<List<YieldRecord>> =
        yieldRecordDao.getYieldsForCropFlow(cropId)

    suspend fun insertYield(yieldRecord: YieldRecord) =
        yieldRecordDao.insertYield(yieldRecord)

    suspend fun updateYield(yieldRecord: YieldRecord) =
        yieldRecordDao.updateYield(yieldRecord)

    suspend fun deleteYield(id: String) =
        yieldRecordDao.deleteYield(id)
}
