package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatuses
import com.example.data.model.CropAssignment
import com.example.data.model.CropStatuses
import com.example.data.model.CropWithPlot
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.PlotDetails
import com.example.data.model.TaskWithDetails
import com.example.data.model.Worker
import com.example.data.model.WorkerDetails
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord
import com.example.data.model.YieldWithCrop
import com.example.data.repository.FarmRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardState(
    val activePlotsCount: Int = 0,
    val activeCrops: List<CropWithPlot> = emptyList(),
    val todayTasks: List<TaskWithDetails> = emptyList(),
    val upcomingHarvests: List<CropWithPlot> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val recentPayments: List<WorkerTransaction> = emptyList(),
    val activeWorkersCount: Int = 0,
    val isLoading: Boolean = false
)

class FarmViewModel(private val repository: FarmRepository) : ViewModel() {

    // Notification / UI Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Selected Date for Attendance & Daily Tasks (defaults to today)
    private val _selectedAttendanceDate = MutableStateFlow(DateUtils.today())
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    // Plots
    private val _showArchivedPlots = MutableStateFlow(false)
    val showArchivedPlots: StateFlow<Boolean> = _showArchivedPlots.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val plots: StateFlow<List<Plot>> = _showArchivedPlots
        .flatMapLatest { showArchived -> repository.getPlots(includeArchived = showArchived) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlots: StateFlow<List<Plot>> = repository.getPlots(includeArchived = false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Crops
    val cropsWithPlot: StateFlow<List<CropWithPlot>> = repository.getCropsWithPlot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCrops: StateFlow<List<CropWithPlot>> = repository.getActiveCropsWithPlot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workers
    private val _showArchivedWorkers = MutableStateFlow(false)
    val showArchivedWorkers: StateFlow<Boolean> = _showArchivedWorkers.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val workers: StateFlow<List<Worker>> = _showArchivedWorkers
        .flatMapLatest { showArchived -> repository.getWorkers(includeArchived = showArchived) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkers: StateFlow<List<Worker>> = repository.getWorkers(includeArchived = false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for Selected Date
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForSelectedDate: StateFlow<List<Attendance>> = _selectedAttendanceDate
        .flatMapLatest { date -> repository.getAttendanceForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payments
    val transactions: StateFlow<List<WorkerTransaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses
    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks
    val tasksWithDetails: StateFlow<List<TaskWithDetails>> = repository.getAllTasksWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<TaskWithDetails>> = repository.getTasksForDate(DateUtils.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Yields
    val yieldsWithCrop: StateFlow<List<YieldWithCrop>> = repository.getAllYieldsWithCrop()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard State
    val dashboardState: StateFlow<DashboardState> = combine(
        combine(activePlots, activeCrops, todayTasks) { plotsList, cropsList, tasksList ->
            Triple(plotsList, cropsList, tasksList)
        },
        combine(expenses, transactions, activeWorkers) { expensesList, transactionsList, workersList ->
            Triple(expensesList, transactionsList, workersList)
        }
    ) { (activePlotsList, activeCropsList, tasksToday), (expensesList, transactionsList, workersList) ->
        val upcoming = activeCropsList.filter { cropWithPlot ->
            val days = DateUtils.daysFromToday(cropWithPlot.crop.expectedHarvestDate)
            days in 0..45
        }
        DashboardState(
            activePlotsCount = activePlotsList.size,
            activeCrops = activeCropsList,
            todayTasks = tasksToday,
            upcomingHarvests = upcoming,
            recentExpenses = expensesList.take(5),
            recentPayments = transactionsList.take(5),
            activeWorkersCount = workersList.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    // ==========================================
    // PLOT ACTIONS
    // ==========================================
    fun toggleShowArchivedPlots() {
        _showArchivedPlots.value = !_showArchivedPlots.value
    }

    fun getPlotDetailsFlow(plotId: String): StateFlow<PlotDetails?> {
        return repository.getPlotDetails(plotId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun savePlot(
        id: String?,
        name: String,
        area: Double,
        areaUnit: String,
        soilType: String,
        irrigationType: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    _userMessage.emit("Plot name is required")
                    return@launch
                }
                if (area <= 0.0) {
                    _userMessage.emit("Area must be greater than zero")
                    return@launch
                }
                val plot = Plot(
                    id = id ?: UUID.randomUUID().toString(),
                    name = trimmedName,
                    area = area,
                    areaUnit = areaUnit,
                    soilType = soilType.trim(),
                    irrigationType = irrigationType.trim(),
                    notes = notes.trim()
                )
                if (id == null) {
                    repository.insertPlot(plot)
                    _userMessage.emit("Plot added successfully")
                } else {
                    repository.updatePlot(plot)
                    _userMessage.emit("Plot updated successfully")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving plot: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun setPlotArchived(plotId: String, archived: Boolean) {
        viewModelScope.launch {
            try {
                repository.archivePlot(plotId, archived)
                _userMessage.emit(if (archived) "Plot archived" else "Plot restored")
            } catch (e: Exception) {
                _userMessage.emit("Failed to update plot archive state")
            }
        }
    }

    fun deletePlot(plotId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePlot(plotId)
                _userMessage.emit("Plot deleted")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Cannot delete plot with linked crops or records. Consider archiving instead.")
            }
        }
    }

    // ==========================================
    // CROP ACTIONS
    // ==========================================
    fun saveCrop(
        id: String?,
        plotId: String,
        cropName: String,
        variety: String,
        plantingDate: String,
        expectedHarvestDate: String,
        status: String,
        perennial: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val trimmedCrop = cropName.trim()
                if (trimmedCrop.isEmpty()) {
                    _userMessage.emit("Crop name is required")
                    return@launch
                }
                if (plotId.isEmpty()) {
                    _userMessage.emit("Please select a plot")
                    return@launch
                }
                val crop = CropAssignment(
                    id = id ?: UUID.randomUUID().toString(),
                    plotId = plotId,
                    cropName = trimmedCrop,
                    variety = variety.trim(),
                    plantingDate = plantingDate,
                    expectedHarvestDate = expectedHarvestDate,
                    status = status,
                    perennial = perennial
                )
                if (id == null) {
                    repository.insertCrop(crop)
                    _userMessage.emit("Crop assignment created")
                } else {
                    repository.updateCrop(crop)
                    _userMessage.emit("Crop assignment updated")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving crop: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun updateCropStatus(cropId: String, status: String) {
        viewModelScope.launch {
            try {
                repository.updateCropStatus(cropId, status)
                _userMessage.emit("Crop status updated to $status")
            } catch (e: Exception) {
                _userMessage.emit("Failed to update crop status")
            }
        }
    }

    fun deleteCrop(cropId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCrop(cropId)
                _userMessage.emit("Crop removed")
            } catch (e: Exception) {
                _userMessage.emit("Cannot delete crop with existing yield records.")
            }
        }
    }

    // ==========================================
    // WORKER ACTIONS
    // ==========================================
    fun toggleShowArchivedWorkers() {
        _showArchivedWorkers.value = !_showArchivedWorkers.value
    }

    fun getWorkerDetailsFlow(workerId: String): StateFlow<WorkerDetails?> {
        return repository.getWorkerDetails(workerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun saveWorker(
        id: String?,
        name: String,
        mobileNumber: String,
        dailyWageRate: Double,
        joiningDate: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    _userMessage.emit("Worker name is required")
                    return@launch
                }
                if (dailyWageRate < 0.0) {
                    _userMessage.emit("Wage rate cannot be negative")
                    return@launch
                }
                val worker = Worker(
                    id = id ?: UUID.randomUUID().toString(),
                    name = trimmedName,
                    mobileNumber = mobileNumber.trim(),
                    dailyWageRate = dailyWageRate,
                    joiningDate = joiningDate.ifEmpty { DateUtils.today() },
                    notes = notes.trim()
                )
                if (id == null) {
                    repository.insertWorker(worker)
                    _userMessage.emit("Worker registered")
                } else {
                    repository.updateWorker(worker)
                    _userMessage.emit("Worker details updated")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving worker: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun setWorkerArchived(workerId: String, archived: Boolean) {
        viewModelScope.launch {
            try {
                repository.archiveWorker(workerId, archived)
                _userMessage.emit(if (archived) "Worker archived" else "Worker reactivated")
            } catch (e: Exception) {
                _userMessage.emit("Failed to update worker status")
            }
        }
    }

    fun deleteWorker(workerId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteWorker(workerId)
                _userMessage.emit("Worker deleted")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error deleting worker: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // ==========================================
    // ATTENDANCE ACTIONS
    // ==========================================
    fun setAttendanceDate(date: String) {
        _selectedAttendanceDate.value = date
    }

    fun recordWorkerAttendance(workerId: String, status: String) {
        viewModelScope.launch {
            try {
                val attendance = Attendance(
                    workerId = workerId,
                    date = _selectedAttendanceDate.value,
                    status = status
                )
                repository.recordAttendance(attendance)
            } catch (e: Exception) {
                _userMessage.emit("Failed to save attendance")
            }
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            try {
                val date = _selectedAttendanceDate.value
                val currentWorkers = activeWorkers.value
                if (currentWorkers.isEmpty()) {
                    _userMessage.emit("No active workers found")
                    return@launch
                }
                val batch = currentWorkers.map { worker ->
                    Attendance(
                        workerId = worker.id,
                        date = date,
                        status = AttendanceStatuses.PRESENT
                    )
                }
                repository.recordAttendanceBatch(batch)
                _userMessage.emit("Marked all ${batch.size} workers present for $date")
            } catch (e: Exception) {
                _userMessage.emit("Failed to mark all present")
            }
        }
    }

    // ==========================================
    // PAYMENTS / ADVANCE / SALARY ACTIONS
    // ==========================================
    fun recordTransaction(
        workerId: String,
        type: String,
        amount: Double,
        date: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (workerId.isEmpty()) {
                    _userMessage.emit("Please select a worker")
                    return@launch
                }
                if (amount <= 0.0) {
                    _userMessage.emit("Amount must be greater than zero")
                    return@launch
                }
                val transaction = WorkerTransaction(
                    id = UUID.randomUUID().toString(),
                    workerId = workerId,
                    type = type,
                    amount = amount,
                    date = date.ifEmpty { DateUtils.today() },
                    notes = notes.trim()
                )
                repository.insertTransaction(transaction)
                _userMessage.emit("$type payment of ${DateUtils.formatCurrency(amount)} recorded")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error recording payment: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                _userMessage.emit("Payment record deleted")
            } catch (e: Exception) {
                _userMessage.emit("Failed to delete payment record")
            }
        }
    }

    // ==========================================
    // EXPENSE ACTIONS
    // ==========================================
    fun recordExpense(
        category: String,
        amount: Double,
        date: String,
        description: String,
        plotId: String?,
        id: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (amount <= 0.0) {
                    _userMessage.emit("Expense amount must be greater than zero")
                    return@launch
                }
                if (description.trim().isEmpty()) {
                    _userMessage.emit("Description is required")
                    return@launch
                }
                val expense = Expense(
                    id = id ?: UUID.randomUUID().toString(),
                    date = date.ifEmpty { DateUtils.today() },
                    category = category,
                    amount = amount,
                    description = description.trim(),
                    plotId = plotId?.ifEmpty { null }
                )
                if (id == null) {
                    repository.insertExpense(expense)
                    _userMessage.emit("Expense recorded")
                } else {
                    repository.updateExpense(expense)
                    _userMessage.emit("Expense updated")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving expense: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(id)
                _userMessage.emit("Expense deleted")
            } catch (e: Exception) {
                _userMessage.emit("Failed to delete expense")
            }
        }
    }

    // ==========================================
    // TASK ACTIONS
    // ==========================================
    fun saveTask(
        id: String?,
        date: String,
        plotId: String?,
        taskType: String,
        description: String,
        durationHours: Double,
        workerIds: List<String>,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val trimmedDesc = description.trim()
                if (trimmedDesc.isEmpty()) {
                    _userMessage.emit("Task description is required")
                    return@launch
                }
                val task = DailyTask(
                    id = id ?: UUID.randomUUID().toString(),
                    date = date.ifEmpty { DateUtils.today() },
                    plotId = plotId?.ifEmpty { null },
                    taskType = taskType,
                    description = trimmedDesc,
                    durationHours = durationHours.coerceAtLeast(0.0),
                    notes = notes.trim()
                )
                if (id == null) {
                    repository.insertTaskWithWorkers(task, workerIds)
                    _userMessage.emit("Task created")
                } else {
                    repository.updateTaskWithWorkers(task, workerIds)
                    _userMessage.emit("Task updated")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving task: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun toggleTaskCompletion(task: DailyTask) {
        viewModelScope.launch {
            try {
                val newStatus = !task.isCompleted
                repository.setTaskCompletion(task.id, newStatus)
                _userMessage.emit(if (newStatus) "Task marked completed" else "Task marked pending")
            } catch (e: Exception) {
                _userMessage.emit("Failed to update task status")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _userMessage.emit("Task deleted")
            } catch (e: Exception) {
                _userMessage.emit("Failed to delete task")
            }
        }
    }

    // ==========================================
    // YIELD / HARVEST ACTIONS
    // ==========================================
    fun recordYield(
        id: String?,
        cropAssignmentId: String,
        date: String,
        quantity: Double,
        unit: String,
        ratePerUnit: Double,
        totalRevenue: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (cropAssignmentId.isEmpty()) {
                    _userMessage.emit("Please select a crop assignment")
                    return@launch
                }
                if (quantity <= 0.0) {
                    _userMessage.emit("Quantity must be greater than zero")
                    return@launch
                }
                val calculatedRevenue = if (totalRevenue > 0.0) totalRevenue else (quantity * ratePerUnit)
                val yieldRecord = YieldRecord(
                    id = id ?: UUID.randomUUID().toString(),
                    cropAssignmentId = cropAssignmentId,
                    date = date.ifEmpty { DateUtils.today() },
                    quantity = quantity,
                    unit = unit,
                    ratePerUnit = ratePerUnit.coerceAtLeast(0.0),
                    totalRevenue = calculatedRevenue.coerceAtLeast(0.0),
                    notes = notes.trim()
                )
                if (id == null) {
                    repository.insertYield(yieldRecord)
                    _userMessage.emit("Harvest record saved: ${DateUtils.formatCurrency(calculatedRevenue)}")
                } else {
                    repository.updateYield(yieldRecord)
                    _userMessage.emit("Harvest record updated")
                }
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("Error saving harvest: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteYield(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteYield(id)
                _userMessage.emit("Harvest record deleted")
            } catch (e: Exception) {
                _userMessage.emit("Failed to delete harvest record")
            }
        }
    }
}

class FarmViewModelFactory(private val repository: FarmRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FarmViewModel::class.java)) {
            return FarmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
