package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Enums & Constants
object AreaUnits {
    const val ACRE = "Acre"
    const val HECTARE = "Hectare"
    const val GUNTHA = "Guntha"
    val all = listOf(ACRE, HECTARE, GUNTHA)
}

object CropStatuses {
    const val PLANNED = "PLANNED"
    const val ACTIVE = "ACTIVE"
    const val HARVESTED = "HARVESTED"
    val all = listOf(PLANNED, ACTIVE, HARVESTED)
}

object AttendanceStatuses {
    const val PRESENT = "PRESENT"
    const val HALF_DAY = "HALF_DAY"
    const val ABSENT = "ABSENT"
    val all = listOf(PRESENT, HALF_DAY, ABSENT)
}

object TransactionTypes {
    const val ADVANCE = "ADVANCE"
    const val SALARY = "SALARY"
    val all = listOf(ADVANCE, SALARY)
}

object ExpenseCategories {
    const val DIESEL = "Diesel"
    const val FERTILIZER = "Fertilizer"
    const val PESTICIDES = "Pesticides"
    const val SEEDS = "Seeds"
    const val LABOR = "Labor"
    const val EQUIPMENT_REPAIR = "Equipment Repair"
    const val IRRIGATION = "Irrigation"
    const val OTHER = "Other"
    val all = listOf(
        DIESEL,
        FERTILIZER,
        PESTICIDES,
        SEEDS,
        LABOR,
        EQUIPMENT_REPAIR,
        IRRIGATION,
        OTHER
    )
}

object TaskTypes {
    const val WEEDING = "Weeding"
    const val SPRAYING = "Spraying"
    const val IRRIGATION = "Irrigation"
    const val SOWING = "Sowing"
    const val HARVESTING = "Harvesting"
    const val FERTILIZER = "Fertilizer Application"
    const val TILLING = "Tilling / Plowing"
    const val PRUNING = "Pruning"
    const val GENERAL = "General Maintenance"

    val all = listOf(
        WEEDING,
        SPRAYING,
        IRRIGATION,
        SOWING,
        HARVESTING,
        FERTILIZER,
        TILLING,
        PRUNING,
        GENERAL
    )
}

object YieldUnits {
    const val KG = "kg"
    const val QUINTAL = "Quintal"
    const val TON = "Ton"
    const val CRATE = "Crate"
    const val BAG = "Bag"
    const val BOX = "Box"

    val all = listOf(KG, QUINTAL, TON, CRATE, BAG, BOX)
}

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING = "PENDING"
}

// 1. Worker
@Entity(
    tableName = "workers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ]
)
data class Worker(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobileNumber: String = "",
    val dailyWageRate: Long = 0L, // In paise (e.g. 45000L = ₹450.00)
    val joiningDate: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 2. Attendance
@Entity(
    tableName = "attendance",
    primaryKeys = ["workerId", "date"],
    indices = [
        Index(value = ["date"]),
        Index(value = ["workerId"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Attendance(
    val workerId: String,
    val date: String, // YYYY-MM-DD
    val status: String, // PRESENT, ABSENT, HALF_DAY
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 3. Worker Transaction
@Entity(
    tableName = "worker_transactions",
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["date"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkerTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val type: String, // ADVANCE, SALARY
    val amount: Long, // In paise (e.g. 125050L = ₹1,250.50)
    val date: String, // YYYY-MM-DD
    val notes: String = "",
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 4. Plot
@Entity(
    tableName = "plots",
    indices = [
        Index(value = ["name"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ]
)
data class Plot(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val area: Double,
    val areaUnit: String = AreaUnits.ACRE,
    val soilType: String = "",
    val irrigationType: String = "",
    val notes: String = "",
    val archived: Boolean = false,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 5. Crop Assignment
@Entity(
    tableName = "crop_assignments",
    indices = [
        Index(value = ["plotId"]),
        Index(value = ["status"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Plot::class,
            parentColumns = ["id"],
            childColumns = ["plotId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class CropAssignment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val plotId: String,
    val cropName: String,
    val variety: String = "",
    val plantingDate: String, // YYYY-MM-DD
    val expectedHarvestDate: String, // YYYY-MM-DD
    val status: String = CropStatuses.PLANNED, // PLANNED, ACTIVE, HARVESTED
    val perennial: Boolean = false,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 6. Yield Record
@Entity(
    tableName = "yield_records",
    indices = [
        Index(value = ["cropAssignmentId"]),
        Index(value = ["date"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CropAssignment::class,
            parentColumns = ["id"],
            childColumns = ["cropAssignmentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class YieldRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cropAssignmentId: String,
    val date: String, // YYYY-MM-DD
    val quantity: Double, // Physical quantity measurement (kg, quintal, etc.)
    val unit: String = "kg",
    val ratePerUnit: Long = 0L, // In paise per unit (e.g. 2550L = ₹25.50)
    val totalRevenue: Long = 0L, // In paise (e.g. 256275L = ₹2,562.75)
    val notes: String = "",
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 7. Daily Task
@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["plotId"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Plot::class,
            parentColumns = ["id"],
            childColumns = ["plotId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DailyTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val plotId: String? = null,
    val taskType: String = "General Maintenance",
    val description: String,
    val durationHours: Double = 0.0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 8. Task-Worker Assignment (Many-to-Many)
@Entity(
    tableName = "task_worker_assignments",
    primaryKeys = ["taskId", "workerId"],
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["taskId"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DailyTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskWorkerAssignment(
    val taskId: String,
    val workerId: String,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)

// 9. Expense
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"]),
        Index(value = ["plotId"]),
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Plot::class,
            parentColumns = ["id"],
            childColumns = ["plotId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val category: String,
    val amount: Long, // In paise (e.g. 350000L = ₹3,500.00)
    val description: String,
    val plotId: String? = null,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING
)
