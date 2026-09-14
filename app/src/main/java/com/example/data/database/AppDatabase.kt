package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.CropAssignmentDao
import com.example.data.dao.DailyTaskDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.PlotDao
import com.example.data.dao.WorkerDao
import com.example.data.dao.WorkerTransactionDao
import com.example.data.dao.YieldRecordDao
import com.example.data.model.Attendance
import com.example.data.model.CropAssignment
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.Plot
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord

@Database(
    entities = [
        Worker::class,
        Attendance::class,
        WorkerTransaction::class,
        Plot::class,
        CropAssignment::class,
        YieldRecord::class,
        DailyTask::class,
        TaskWorkerAssignment::class,
        Expense::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun workerTransactionDao(): WorkerTransactionDao
    abstract fun plotDao(): PlotDao
    abstract fun cropAssignmentDao(): CropAssignmentDao
    abstract fun yieldRecordDao(): YieldRecordDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun syncDao(): com.example.data.dao.SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "krushi_mitra_database"
                )
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_1_2,
                        DatabaseMigrations.MIGRATION_2_3,
                        DatabaseMigrations.MIGRATION_1_3
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
