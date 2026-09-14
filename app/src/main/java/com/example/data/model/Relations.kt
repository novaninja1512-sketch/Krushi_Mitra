package com.example.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TaskWithDetails(
    @Embedded val task: DailyTask,
    @Relation(
        parentColumn = "plotId",
        entityColumn = "id"
    )
    val plot: Plot?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskWorkerAssignment::class,
            parentColumn = "taskId",
            entityColumn = "workerId"
        )
    )
    val workers: List<Worker>
)

data class CropWithPlot(
    @Embedded val crop: CropAssignment,
    @Relation(
        parentColumn = "plotId",
        entityColumn = "id"
    )
    val plot: Plot?
)

data class YieldWithCrop(
    @Embedded val yieldRecord: YieldRecord,
    @Relation(
        entity = CropAssignment::class,
        parentColumn = "cropAssignmentId",
        entityColumn = "id"
    )
    val cropWithPlot: CropWithPlot?
)

data class PlotDetails(
    val plot: Plot,
    val crops: List<CropAssignment>,
    val tasks: List<DailyTask>,
    val expenses: List<Expense>,
    val yields: List<YieldRecord>
)

data class WorkerDetails(
    val worker: Worker,
    val presentCount: Int,
    val halfDayCount: Int,
    val absentCount: Int,
    val totalAdvance: Long, // in paise
    val totalSalary: Long,  // in paise
    val recentTransactions: List<WorkerTransaction>,
    val recentTasks: List<DailyTask>
) {
    /**
     * Total earned wage in paise:
     * Full days (PRESENT) * dailyWageRate + Half days (HALF_DAY) * (dailyWageRate / 2)
     */
    val totalEarnedPaise: Long
        get() = (presentCount.toLong() * worker.dailyWageRate) + (halfDayCount.toLong() * (worker.dailyWageRate / 2L))

    /**
     * Remaining worker balance in paise:
     * totalEarnedPaise - totalAdvance - totalSalary
     */
    val remainingBalancePaise: Long
        get() = totalEarnedPaise - totalAdvance - totalSalary
}

data class AttendanceSummary(
    val presentCount: Int,
    val halfDayCount: Int,
    val absentCount: Int,
    val totalWorkers: Int
)
