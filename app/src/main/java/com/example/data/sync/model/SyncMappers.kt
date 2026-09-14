package com.example.data.sync.model

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
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun toIso(epochMillis: Long?): String? {
        if (epochMillis == null) return null
        return synchronized(isoFormat) {
            isoFormat.format(Date(epochMillis))
        }
    }

    fun toEpoch(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoString).toEpochMilli()
            } else {
                synchronized(isoFormat) {
                    isoFormat.parse(isoString)?.time
                }
            }
        } catch (e: Exception) {
            try {
                // Fallback for formats without millis
                val fallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallback.parse(isoString)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }
}

// 1. Worker Mappers
fun Worker.toRemote(userId: String): WorkerRemote = WorkerRemote(
    id = id,
    userId = userId,
    name = name,
    mobileNumber = mobileNumber,
    dailyWageRate = CurrencyUtils.paiseToRupees(dailyWageRate),
    joiningDate = joiningDate,
    notes = notes,
    archived = archived,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun WorkerRemote.toEntity(): Worker = Worker(
    id = id,
    name = name,
    mobileNumber = mobileNumber,
    dailyWageRate = CurrencyUtils.rupeesToPaise(dailyWageRate),
    joiningDate = joiningDate,
    notes = notes,
    archived = archived,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 2. Attendance Mappers
fun Attendance.toRemote(userId: String): AttendanceRemote = AttendanceRemote(
    workerId = workerId,
    date = date,
    userId = userId,
    status = status,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun AttendanceRemote.toEntity(): Attendance = Attendance(
    workerId = workerId,
    date = date,
    status = status,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 3. Worker Transaction Mappers
fun WorkerTransaction.toRemote(userId: String): WorkerTransactionRemote = WorkerTransactionRemote(
    id = id,
    userId = userId,
    workerId = workerId,
    type = type,
    amount = CurrencyUtils.paiseToRupees(amount),
    date = date,
    notes = notes,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun WorkerTransactionRemote.toEntity(): WorkerTransaction = WorkerTransaction(
    id = id,
    workerId = workerId,
    type = type,
    amount = CurrencyUtils.rupeesToPaise(amount),
    date = date,
    notes = notes,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 4. Plot Mappers
fun Plot.toRemote(userId: String): PlotRemote = PlotRemote(
    id = id,
    userId = userId,
    name = name,
    area = area,
    areaUnit = areaUnit,
    soilType = soilType,
    irrigationType = irrigationType,
    notes = notes,
    archived = archived,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun PlotRemote.toEntity(): Plot = Plot(
    id = id,
    name = name,
    area = area,
    areaUnit = areaUnit,
    soilType = soilType,
    irrigationType = irrigationType,
    notes = notes,
    archived = archived,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 5. Crop Assignment Mappers
fun CropAssignment.toRemote(userId: String): CropAssignmentRemote = CropAssignmentRemote(
    id = id,
    userId = userId,
    plotId = plotId,
    cropName = cropName,
    variety = variety,
    plantingDate = plantingDate,
    expectedHarvestDate = expectedHarvestDate,
    status = status,
    perennial = perennial,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun CropAssignmentRemote.toEntity(): CropAssignment = CropAssignment(
    id = id,
    plotId = plotId,
    cropName = cropName,
    variety = variety,
    plantingDate = plantingDate,
    expectedHarvestDate = expectedHarvestDate,
    status = status,
    perennial = perennial,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 6. Yield Record Mappers
fun YieldRecord.toRemote(userId: String): YieldRecordRemote = YieldRecordRemote(
    id = id,
    userId = userId,
    cropAssignmentId = cropAssignmentId,
    date = date,
    quantity = quantity,
    unit = unit,
    ratePerUnit = CurrencyUtils.paiseToRupees(ratePerUnit),
    totalRevenue = CurrencyUtils.paiseToRupees(totalRevenue),
    notes = notes,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun YieldRecordRemote.toEntity(): YieldRecord = YieldRecord(
    id = id,
    cropAssignmentId = cropAssignmentId,
    date = date,
    quantity = quantity,
    unit = unit,
    ratePerUnit = CurrencyUtils.rupeesToPaise(ratePerUnit),
    totalRevenue = CurrencyUtils.rupeesToPaise(totalRevenue),
    notes = notes,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 7. Daily Task Mappers
fun DailyTask.toRemote(userId: String): DailyTaskRemote = DailyTaskRemote(
    id = id,
    userId = userId,
    date = date,
    plotId = plotId,
    taskType = taskType,
    description = description,
    durationHours = durationHours,
    isCompleted = isCompleted,
    notes = notes,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun DailyTaskRemote.toEntity(): DailyTask = DailyTask(
    id = id,
    date = date,
    plotId = plotId,
    taskType = taskType,
    description = description,
    durationHours = durationHours,
    isCompleted = isCompleted,
    notes = notes,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 8. Task-Worker Assignment Mappers
fun TaskWorkerAssignment.toRemote(userId: String): TaskWorkerAssignmentRemote = TaskWorkerAssignmentRemote(
    taskId = taskId,
    workerId = workerId,
    userId = userId,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun TaskWorkerAssignmentRemote.toEntity(): TaskWorkerAssignment = TaskWorkerAssignment(
    taskId = taskId,
    workerId = workerId,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)

// 9. Expense Mappers
fun Expense.toRemote(userId: String): ExpenseRemote = ExpenseRemote(
    id = id,
    userId = userId,
    date = date,
    category = category,
    amount = CurrencyUtils.paiseToRupees(amount),
    description = description,
    plotId = plotId,
    createdAt = TimeUtils.toIso(createdAt),
    updatedAt = TimeUtils.toIso(updatedAt),
    deletedAt = TimeUtils.toIso(deletedAt)
)

fun ExpenseRemote.toEntity(): Expense = Expense(
    id = id,
    date = date,
    category = category,
    amount = CurrencyUtils.rupeesToPaise(amount),
    description = description,
    plotId = plotId,
    userId = userId,
    createdAt = TimeUtils.toEpoch(createdAt) ?: System.currentTimeMillis(),
    updatedAt = TimeUtils.toEpoch(updatedAt) ?: System.currentTimeMillis(),
    deletedAt = TimeUtils.toEpoch(deletedAt),
    syncStatus = SyncStatus.SYNCED
)
