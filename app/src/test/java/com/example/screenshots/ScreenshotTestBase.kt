package com.example.screenshots

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.UserProfile
import com.example.data.database.AppDatabase
import com.example.data.model.AreaUnits
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatuses
import com.example.data.model.CropAssignment
import com.example.data.model.CropStatuses
import com.example.data.model.DailyTask
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.Plot
import com.example.data.model.TaskTypes
import com.example.data.model.TaskWorkerAssignment
import com.example.data.model.TransactionTypes
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.model.YieldRecord
import com.example.data.model.YieldUnits
import com.example.data.repository.FarmRepository
import com.example.data.sync.SyncEngine
import com.example.data.sync.SyncState
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.io.File

abstract class ScreenshotTestBase {

    protected lateinit var context: Context
    protected lateinit var db: AppDatabase
    protected lateinit var authRepository: AuthRepository
    protected lateinit var syncEngine: SyncEngine
    protected lateinit var repository: FarmRepository
    protected lateinit var viewModel: FarmViewModel

    protected val testUserId = "farmer-user-101"

    protected val baseDir: File = File(System.getProperty("user.dir") ?: ".").let {
        if (it.name == "app") it.parentFile ?: it else it
    }

    @Before
    fun baseSetUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        authRepository = AuthRepository(context)
        syncEngine = SyncEngine(context, db, authRepository)
        repository = FarmRepository(db, syncEngine, authRepository)
        viewModel = FarmViewModel(repository, authRepository, syncEngine)
    }

    @After
    fun baseTearDown() {
        db.close()
    }

    protected fun setAuthUser(profile: UserProfile?) {
        val userField = AuthRepository::class.java.getDeclaredField("_currentUser")
        userField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (userField.get(authRepository) as MutableStateFlow<UserProfile?>).value = profile

        val authStateField = AuthRepository::class.java.getDeclaredField("_authState")
        authStateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (authStateField.get(authRepository) as MutableStateFlow<AuthState>).value =
            if (profile != null) AuthState.Authenticated(profile) else AuthState.Unauthenticated
    }

    protected fun setSyncEngineState(state: SyncState) {
        val syncField = SyncEngine::class.java.getDeclaredField("_syncState")
        syncField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (syncField.get(syncEngine) as MutableStateFlow<SyncState>).value = state
    }

    protected fun seedRealisticData() = runBlocking {
        val plot1 = Plot(
            id = "plot-1",
            name = "North Field",
            area = 4.5,
            areaUnit = AreaUnits.ACRE,
            soilType = "Black Cotton",
            irrigationType = "Drip Irrigation",
            notes = "Main commercial parcel for Kharif & Rabi crops",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val plot2 = Plot(
            id = "plot-2",
            name = "Canal Field",
            area = 3.0,
            areaUnit = AreaUnits.ACRE,
            soilType = "Clay Loam",
            irrigationType = "Canal & Furrow",
            notes = "Adjacent to the western canal distributor",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val plot3 = Plot(
            id = "plot-3",
            name = "Orchard",
            area = 2.0,
            areaUnit = AreaUnits.ACRE,
            soilType = "Red Sandy",
            irrigationType = "Drip & Sprinkler",
            notes = "Pomegranate and seasonal intercrops",
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.plotDao().insertPlot(plot1)
        db.plotDao().insertPlot(plot2)
        db.plotDao().insertPlot(plot3)

        val crop1 = CropAssignment(
            id = "crop-1",
            plotId = "plot-1",
            cropName = "Soybean",
            variety = "JS 335",
            plantingDate = DateUtils.offsetDate(DateUtils.today(), -45),
            expectedHarvestDate = DateUtils.offsetDate(DateUtils.today(), 25),
            status = CropStatuses.ACTIVE,
            perennial = false,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val crop2 = CropAssignment(
            id = "crop-2",
            plotId = "plot-2",
            cropName = "Tomato",
            variety = "Abhinav (F1 Hybrid)",
            plantingDate = DateUtils.offsetDate(DateUtils.today(), -30),
            expectedHarvestDate = DateUtils.offsetDate(DateUtils.today(), 15),
            status = CropStatuses.ACTIVE,
            perennial = false,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val crop3 = CropAssignment(
            id = "crop-3",
            plotId = "plot-3",
            cropName = "Wheat",
            variety = "Lokwan HD 2189",
            plantingDate = DateUtils.offsetDate(DateUtils.today(), -15),
            expectedHarvestDate = DateUtils.offsetDate(DateUtils.today(), 40),
            status = CropStatuses.ACTIVE,
            perennial = false,
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.cropAssignmentDao().insertCrop(crop1)
        db.cropAssignmentDao().insertCrop(crop2)
        db.cropAssignmentDao().insertCrop(crop3)

        val worker1 = Worker(
            id = "worker-1",
            name = "Kailash Patil",
            mobileNumber = "+91 98220 12345",
            dailyWageRate = 45000L,
            joiningDate = DateUtils.offsetDate(DateUtils.today(), -120),
            notes = "Senior farm supervisor",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val worker2 = Worker(
            id = "worker-2",
            name = "Sunita Shinde",
            mobileNumber = "+91 98221 23456",
            dailyWageRate = 35000L,
            joiningDate = DateUtils.offsetDate(DateUtils.today(), -90),
            notes = "Field harvesting and weeding",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val worker3 = Worker(
            id = "worker-3",
            name = "Ramesh Pawar",
            mobileNumber = "+91 98222 34567",
            dailyWageRate = 40000L,
            joiningDate = DateUtils.offsetDate(DateUtils.today(), -60),
            notes = "Tractor operator and spraying",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val worker4 = Worker(
            id = "worker-4",
            name = "Anand More",
            mobileNumber = "+91 98223 45678",
            dailyWageRate = 35000L,
            joiningDate = DateUtils.offsetDate(DateUtils.today(), -40),
            notes = "General farm worker",
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.workerDao().insertWorker(worker1)
        db.workerDao().insertWorker(worker2)
        db.workerDao().insertWorker(worker3)
        db.workerDao().insertWorker(worker4)

        val att1 = Attendance(
            workerId = "worker-1",
            date = DateUtils.today(),
            status = AttendanceStatuses.PRESENT,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val att2 = Attendance(
            workerId = "worker-2",
            date = DateUtils.today(),
            status = AttendanceStatuses.PRESENT,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val att3 = Attendance(
            workerId = "worker-3",
            date = DateUtils.today(),
            status = AttendanceStatuses.HALF_DAY,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val att4 = Attendance(
            workerId = "worker-4",
            date = DateUtils.today(),
            status = AttendanceStatuses.ABSENT,
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.attendanceDao().recordAttendanceBatch(listOf(att1, att2, att3, att4))

        val tx1 = WorkerTransaction(
            id = "tx-1",
            workerId = "worker-1",
            type = TransactionTypes.ADVANCE,
            amount = 200000L,
            date = DateUtils.offsetDate(DateUtils.today(), -5),
            notes = "Festival advance",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val tx2 = WorkerTransaction(
            id = "tx-2",
            workerId = "worker-2",
            type = TransactionTypes.SALARY,
            amount = 210000L,
            date = DateUtils.offsetDate(DateUtils.today(), -2),
            notes = "Weekly wages",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val tx3 = WorkerTransaction(
            id = "tx-3",
            workerId = "worker-3",
            type = TransactionTypes.ADVANCE,
            amount = 100000L,
            date = DateUtils.offsetDate(DateUtils.today(), -1),
            notes = "Medical assistance",
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.workerTransactionDao().insertTransaction(tx1)
        db.workerTransactionDao().insertTransaction(tx2)
        db.workerTransactionDao().insertTransaction(tx3)

        val task1 = DailyTask(
            id = "task-1",
            date = DateUtils.today(),
            plotId = "plot-1",
            taskType = TaskTypes.IRRIGATION,
            description = "Morning drip irrigation cycle (3 hours)",
            durationHours = 3.0,
            notes = "Pump pressure stable at 2.5 bar",
            isCompleted = true,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val task2 = DailyTask(
            id = "task-2",
            date = DateUtils.today(),
            plotId = "plot-2",
            taskType = TaskTypes.WEEDING,
            description = "Manual inter-row weeding in tomato beds",
            durationHours = 4.5,
            notes = "Completed western parcel half",
            isCompleted = false,
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val task3 = DailyTask(
            id = "task-3",
            date = DateUtils.today(),
            plotId = "plot-3",
            taskType = TaskTypes.SPRAYING,
            description = "Neem oil foliar spray application",
            durationHours = 2.0,
            notes = "Applied before midday heat",
            isCompleted = false,
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.dailyTaskDao().insertTask(task1)
        db.dailyTaskDao().insertTask(task2)
        db.dailyTaskDao().insertTask(task3)

        db.dailyTaskDao().insertTaskWorkers(listOf(
            TaskWorkerAssignment(taskId = "task-1", workerId = "worker-1", syncStatus = "SYNCED", userId = testUserId),
            TaskWorkerAssignment(taskId = "task-2", workerId = "worker-2", syncStatus = "SYNCED", userId = testUserId),
            TaskWorkerAssignment(taskId = "task-3", workerId = "worker-3", syncStatus = "SYNCED", userId = testUserId)
        ))

        val exp1 = Expense(
            id = "exp-1",
            date = DateUtils.today(),
            category = ExpenseCategories.DIESEL,
            amount = 185000L,
            description = "20L diesel fuel for tractor tilling",
            plotId = "plot-1",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val exp2 = Expense(
            id = "exp-2",
            date = DateUtils.offsetDate(DateUtils.today(), -3),
            category = ExpenseCategories.FERTILIZER,
            amount = 345000L,
            description = "2 bags DAP + 1 bag Zinc micronutrient",
            plotId = "plot-2",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val exp3 = Expense(
            id = "exp-3",
            date = DateUtils.offsetDate(DateUtils.today(), -7),
            category = ExpenseCategories.SEEDS,
            amount = 240000L,
            description = "Certified hybrid seeds packet",
            plotId = "plot-3",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val exp4 = Expense(
            id = "exp-4",
            date = DateUtils.offsetDate(DateUtils.today(), -10),
            category = ExpenseCategories.OTHER,
            amount = 150000L,
            description = "Cartage and tempo freight to mandi",
            plotId = "plot-1",
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.expenseDao().insertExpense(exp1)
        db.expenseDao().insertExpense(exp2)
        db.expenseDao().insertExpense(exp3)
        db.expenseDao().insertExpense(exp4)

        val yield1 = YieldRecord(
            id = "yield-1",
            cropAssignmentId = "crop-1",
            date = DateUtils.offsetDate(DateUtils.today(), -4),
            quantity = 25.0,
            unit = YieldUnits.QUINTAL,
            ratePerUnit = 480000L,
            totalRevenue = 12000000L,
            notes = "Grade A quality produce sold at APMC",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val yield2 = YieldRecord(
            id = "yield-2",
            cropAssignmentId = "crop-2",
            date = DateUtils.offsetDate(DateUtils.today(), -2),
            quantity = 80.0,
            unit = YieldUnits.CRATE,
            ratePerUnit = 45000L,
            totalRevenue = 3600000L,
            notes = "Delivered to local wholesale vegetable market",
            syncStatus = "SYNCED",
            userId = testUserId
        )
        val yield3 = YieldRecord(
            id = "yield-3",
            cropAssignmentId = "crop-3",
            date = DateUtils.offsetDate(DateUtils.today(), -8),
            quantity = 40.0,
            unit = YieldUnits.QUINTAL,
            ratePerUnit = 245000L,
            totalRevenue = 9800000L,
            notes = "Grain storage and mandi dispatch",
            syncStatus = "SYNCED",
            userId = testUserId
        )

        db.yieldRecordDao().insertYield(yield1)
        db.yieldRecordDao().insertYield(yield2)
        db.yieldRecordDao().insertYield(yield3)
    }
}
