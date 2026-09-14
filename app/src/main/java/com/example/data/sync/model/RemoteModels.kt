package com.example.data.sync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlotRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("area") val area: Double,
    @SerialName("area_unit") val areaUnit: String = "Acre",
    @SerialName("soil_type") val soilType: String = "",
    @SerialName("irrigation_type") val irrigationType: String = "",
    @SerialName("notes") val notes: String = "",
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class CropAssignmentRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("plot_id") val plotId: String,
    @SerialName("crop_name") val cropName: String,
    @SerialName("variety") val variety: String = "",
    @SerialName("planting_date") val plantingDate: String,
    @SerialName("expected_harvest_date") val expectedHarvestDate: String,
    @SerialName("status") val status: String = "PLANNED",
    @SerialName("perennial") val perennial: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class YieldRecordRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("crop_assignment_id") val cropAssignmentId: String,
    @SerialName("date") val date: String,
    @SerialName("quantity") val quantity: Double,
    @SerialName("unit") val unit: String = "kg",
    @SerialName("rate_per_unit") val ratePerUnit: Double = 0.0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("notes") val notes: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class WorkerRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("mobile_number") val mobileNumber: String = "",
    @SerialName("daily_wage_rate") val dailyWageRate: Double = 0.0,
    @SerialName("joining_date") val joiningDate: String = "",
    @SerialName("notes") val notes: String = "",
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class AttendanceRemote(
    @SerialName("worker_id") val workerId: String,
    @SerialName("date") val date: String,
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String = "PRESENT",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class WorkerTransactionRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("worker_id") val workerId: String,
    @SerialName("type") val type: String,
    @SerialName("amount") val amount: Double,
    @SerialName("date") val date: String,
    @SerialName("notes") val notes: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class DailyTaskRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("date") val date: String,
    @SerialName("plot_id") val plotId: String? = null,
    @SerialName("task_type") val taskType: String = "General Maintenance",
    @SerialName("description") val description: String,
    @SerialName("duration_hours") val durationHours: Double = 0.0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("notes") val notes: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class TaskWorkerAssignmentRemote(
    @SerialName("task_id") val taskId: String,
    @SerialName("worker_id") val workerId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class ExpenseRemote(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("date") val date: String,
    @SerialName("category") val category: String,
    @SerialName("amount") val amount: Double,
    @SerialName("description") val description: String,
    @SerialName("plot_id") val plotId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
