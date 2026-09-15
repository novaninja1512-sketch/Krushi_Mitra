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

class FarmRepository(
    private val db: AppDatabase,
    private val syncEngine: com.example.data.sync.SyncEngine? = null,
    private val authRepository: com.example.data.auth.AuthRepository? = null
) {

    private val workerDao = db.workerDao()
    private val attendanceDao = db.attendanceDao()
    private val workerTransactionDao = db.workerTransactionDao()
    private val plotDao = db.plotDao()
    private val cropAssignmentDao = db.cropAssignmentDao()
    private val yieldRecordDao = db.yieldRecordDao()
    private val dailyTaskDao = db.dailyTaskDao()
    private val expenseDao = db.expenseDao()

    private fun currentUserId(): String? = authRepository?.currentUser?.value?.id

    private fun triggerSync() {
        syncEngine?.triggerSync()
    }

    // Plots
    fun getPlots(includeArchived: Boolean = false): Flow<List<Plot>> =
        plotDao.getPlotsFlow(includeArchived)

    fun getPlotById(id: String): Flow<Plot?> = plotDao.getPlotByIdFlow(id)

    suspend fun insertPlot(plot: Plot) {
        val toInsert = plot.copy(
            userId = plot.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        plotDao.insertPlot(toInsert)
        triggerSync()
    }

    suspend fun updatePlot(plot: Plot) {
        val toUpdate = plot.copy(
            userId = plot.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        plotDao.updatePlot(toUpdate)
        triggerSync()
    }

    suspend fun archivePlot(id: String, archived: Boolean) {
        plotDao.setPlotArchived(id, archived)
        triggerSync()
    }

    suspend fun deletePlot(id: String) {
        plotDao.deletePlot(id)
        triggerSync()
    }

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

    suspend fun insertCrop(crop: CropAssignment) {
        val toInsert = crop.copy(
            userId = crop.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        cropAssignmentDao.insertCrop(toInsert)
        triggerSync()
    }

    suspend fun updateCrop(crop: CropAssignment) {
        val toUpdate = crop.copy(
            userId = crop.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        cropAssignmentDao.updateCrop(toUpdate)
        triggerSync()
    }

    suspend fun updateCropStatus(id: String, status: String) {
        cropAssignmentDao.updateCropStatus(id, status)
        triggerSync()
    }

    suspend fun deleteCrop(id: String) {
        cropAssignmentDao.deleteCrop(id)
        triggerSync()
    }

    // Workers
    fun getWorkers(includeArchived: Boolean = false): Flow<List<Worker>> =
        workerDao.getWorkersFlow(includeArchived)

    fun getWorkerById(id: String): Flow<Worker?> = workerDao.getWorkerByIdFlow(id)

    suspend fun insertWorker(worker: Worker) {
        val toInsert = worker.copy(
            userId = worker.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        workerDao.insertWorker(toInsert)
        triggerSync()
    }

    suspend fun updateWorker(worker: Worker) {
        val toUpdate = worker.copy(
            userId = worker.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        workerDao.updateWorker(toUpdate)
        triggerSync()
    }

    suspend fun archiveWorker(id: String, archived: Boolean) {
        workerDao.setWorkerArchived(id, archived)
        triggerSync()
    }

    suspend fun deleteWorker(id: String) {
        workerDao.deleteWorker(id)
        triggerSync()
    }

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

    suspend fun recordAttendance(attendance: Attendance) {
        val toRecord = attendance.copy(
            userId = attendance.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        attendanceDao.recordAttendance(toRecord)
        triggerSync()
    }

    suspend fun recordAttendanceBatch(list: List<Attendance>) {
        val now = System.currentTimeMillis()
        val uid = currentUserId()
        val toRecord = list.map {
            it.copy(
                userId = it.userId ?: uid,
                updatedAt = now,
                syncStatus = com.example.data.model.SyncStatus.PENDING
            )
        }
        attendanceDao.recordAttendanceBatch(toRecord)
        triggerSync()
    }

    // Worker Transactions (Salary / Advance)
    fun getAllTransactions(): Flow<List<WorkerTransaction>> =
        workerTransactionDao.getAllTransactionsFlow()

    fun getTransactionsForWorker(workerId: String): Flow<List<WorkerTransaction>> =
        workerTransactionDao.getTransactionsForWorkerFlow(workerId)

    suspend fun insertTransaction(transaction: WorkerTransaction) {
        val toInsert = transaction.copy(
            userId = transaction.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        workerTransactionDao.insertTransaction(toInsert)
        triggerSync()
    }

    suspend fun deleteTransaction(id: String) {
        workerTransactionDao.deleteTransaction(id)
        triggerSync()
    }

    // Expenses
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpensesFlow()

    fun getExpensesForPlot(plotId: String): Flow<List<Expense>> =
        expenseDao.getExpensesForPlotFlow(plotId)

    suspend fun insertExpense(expense: Expense) {
        val toInsert = expense.copy(
            userId = expense.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        expenseDao.insertExpense(toInsert)
        triggerSync()
    }

    suspend fun updateExpense(expense: Expense) {
        val toUpdate = expense.copy(
            userId = expense.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        expenseDao.updateExpense(toUpdate)
        triggerSync()
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpense(id)
        triggerSync()
    }

    // Tasks
    fun getAllTasksWithDetails(): Flow<List<TaskWithDetails>> =
        dailyTaskDao.getAllTasksWithDetailsFlow()

    fun getTasksForDate(date: String): Flow<List<TaskWithDetails>> =
        dailyTaskDao.getTasksWithDetailsForDateFlow(date)

    suspend fun insertTaskWithWorkers(task: DailyTask, workerIds: List<String>) {
        val now = System.currentTimeMillis()
        val uid = currentUserId()
        val toInsert = task.copy(
            userId = task.userId ?: uid,
            updatedAt = now,
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        dailyTaskDao.insertTask(toInsert)
        if (workerIds.isNotEmpty()) {
            val assignments = workerIds.map {
                TaskWorkerAssignment(
                    taskId = task.id,
                    workerId = it,
                    userId = uid,
                    updatedAt = now,
                    syncStatus = com.example.data.model.SyncStatus.PENDING
                )
            }
            dailyTaskDao.insertTaskWorkers(assignments)
        }
        triggerSync()
    }

    suspend fun updateTaskWithWorkers(task: DailyTask, workerIds: List<String>) {
        val now = System.currentTimeMillis()
        val uid = currentUserId()
        val toUpdate = task.copy(
            userId = task.userId ?: uid,
            updatedAt = now,
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        dailyTaskDao.updateTask(toUpdate)
        dailyTaskDao.deleteTaskWorkers(task.id)
        if (workerIds.isNotEmpty()) {
            val assignments = workerIds.map {
                TaskWorkerAssignment(
                    taskId = task.id,
                    workerId = it,
                    userId = uid,
                    updatedAt = now,
                    syncStatus = com.example.data.model.SyncStatus.PENDING
                )
            }
            dailyTaskDao.insertTaskWorkers(assignments)
        }
        triggerSync()
    }

    suspend fun setTaskCompletion(id: String, isCompleted: Boolean) {
        dailyTaskDao.setTaskCompleted(id, isCompleted)
        triggerSync()
    }

    suspend fun deleteTask(id: String) {
        dailyTaskDao.deleteTaskWorkers(id)
        dailyTaskDao.deleteTask(id)
        triggerSync()
    }

    // Yield
    fun getAllYieldsWithCrop(): Flow<List<YieldWithCrop>> =
        yieldRecordDao.getYieldsWithCropFlow()

    fun getYieldsForCrop(cropId: String): Flow<List<YieldRecord>> =
        yieldRecordDao.getYieldsForCropFlow(cropId)

    suspend fun insertYield(yieldRecord: YieldRecord) {
        val toInsert = yieldRecord.copy(
            userId = yieldRecord.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        yieldRecordDao.insertYield(toInsert)
        triggerSync()
    }

    suspend fun updateYield(yieldRecord: YieldRecord) {
        val toUpdate = yieldRecord.copy(
            userId = yieldRecord.userId ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.model.SyncStatus.PENDING
        )
        yieldRecordDao.updateYield(toUpdate)
        triggerSync()
    }

    suspend fun deleteYield(id: String) {
        yieldRecordDao.deleteYield(id)
        triggerSync()
    }

    // Sync status marking (strictly user-scoped)
    suspend fun markPlotsSynced(userId: String, ids: List<String>) = db.syncDao().markPlotsSynced(userId, ids)
    suspend fun markWorkersSynced(userId: String, ids: List<String>) = db.syncDao().markWorkersSynced(userId, ids)
    suspend fun markAttendanceSynced(userId: String, workerId: String, date: String) = db.syncDao().markAttendanceSynced(userId, workerId, date)
    suspend fun markWorkerTransactionsSynced(userId: String, ids: List<String>) = db.syncDao().markWorkerTransactionsSynced(userId, ids)
    suspend fun markCropAssignmentsSynced(userId: String, ids: List<String>) = db.syncDao().markCropAssignmentsSynced(userId, ids)
    suspend fun markYieldRecordsSynced(userId: String, ids: List<String>) = db.syncDao().markYieldRecordsSynced(userId, ids)
    suspend fun markDailyTasksSynced(userId: String, ids: List<String>) = db.syncDao().markDailyTasksSynced(userId, ids)
    suspend fun markTaskWorkerAssignmentsSynced(userId: String, taskId: String, workerId: String) = db.syncDao().markTaskWorkerAssignmentsSynced(userId, taskId, workerId)
    suspend fun markExpensesSynced(userId: String, ids: List<String>) = db.syncDao().markExpensesSynced(userId, ids)
}
