package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.auth.AuthRepository
import com.example.data.database.AppDatabase
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
import com.example.data.sync.model.AttendanceRemote
import com.example.data.sync.model.CropAssignmentRemote
import com.example.data.sync.model.DailyTaskRemote
import com.example.data.sync.model.ExpenseRemote
import com.example.data.sync.model.PlotRemote
import com.example.data.sync.model.TaskWorkerAssignmentRemote
import com.example.data.sync.model.TimeUtils
import com.example.data.sync.model.WorkerRemote
import com.example.data.sync.model.WorkerTransactionRemote
import com.example.data.sync.model.YieldRecordRemote
import com.example.data.sync.model.toEntity
import com.example.data.sync.model.toRemote
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SyncState {
    data class Synced(val lastSyncTime: Long) : SyncState
    object Syncing : SyncState
    object Offline : SyncState
    data class Pending(val count: Int) : SyncState
    data class Error(val message: String) : SyncState
}

class SyncEngine(
    private val context: Context,
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) {
    private val TAG = "SyncEngine"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    private val prefs = context.getSharedPreferences("krushi_sync_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced(getLastSyncTime()))
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var syncJob: Job? = null

    init {
        registerNetworkCallback()
        observePendingCount()
    }

    private fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", 0L)
    }

    private fun setLastSyncTime(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
    }

    private fun observePendingCount() {
        scope.launch {
            database.syncDao().getPendingCountFlow().distinctUntilChanged().collect { count ->
                val currentState = _syncState.value
                if (currentState !is SyncState.Syncing) {
                    if (!isOnline()) {
                        _syncState.value = SyncState.Offline
                    } else if (count > 0) {
                        _syncState.value = SyncState.Pending(count)
                    } else if (currentState !is SyncState.Synced) {
                        _syncState.value = SyncState.Synced(getLastSyncTime())
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network became available. Triggering auto-sync.")
                    triggerSync()
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost. Switching to offline mode.")
                    _syncState.value = SyncState.Offline
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback: ${e.message}", e)
        }
    }

    fun isOnline(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    fun triggerSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            performSync()
        }
    }

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val user = authRepository.currentUser.value
            if (user == null) {
                Log.d(TAG, "User not authenticated. Sync skipped.")
                return@withContext Result.success(Unit)
            }

            if (!isOnline()) {
                _syncState.value = SyncState.Offline
                return@withContext Result.failure(IllegalStateException("No internet connection"))
            }

            val client = SupabaseClientProvider.getClient()
            if (client == null) {
                _syncState.value = SyncState.Synced(getLastSyncTime())
                return@withContext Result.success(Unit)
            }

            _syncState.value = SyncState.Syncing

            try {
                val syncDao = database.syncDao()
                val userId = user.id

                // 1. Associate any existing unassigned local records to this authenticated user
                syncDao.associateAllLocalRecordsToUser(userId)

                // 2. UPLOAD PHASE (Push local pending changes to Supabase)
                // Order strictly follows foreign keys
                uploadPlots(client, syncDao, userId)
                uploadCrops(client, syncDao, userId)
                uploadYieldRecords(client, syncDao, userId)
                uploadWorkers(client, syncDao, userId)
                uploadAttendance(client, syncDao, userId)
                uploadTransactions(client, syncDao, userId)
                uploadDailyTasks(client, syncDao, userId)
                uploadTaskWorkers(client, syncDao, userId)
                uploadExpenses(client, syncDao, userId)

                // 3. DOWNLOAD / PULL PHASE (Fetch cloud changes and merge via Last-Write-Wins)
                pullPlots(client, syncDao, userId)
                pullCrops(client, syncDao, userId)
                pullYieldRecords(client, syncDao, userId)
                pullWorkers(client, syncDao, userId)
                pullAttendance(client, syncDao, userId)
                pullTransactions(client, syncDao, userId)
                pullDailyTasks(client, syncDao, userId)
                pullTaskWorkers(client, syncDao, userId)
                pullExpenses(client, syncDao, userId)

                val now = System.currentTimeMillis()
                setLastSyncTime(now)
                _syncState.value = SyncState.Synced(now)
                Log.d(TAG, "Sync completed successfully at $now")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                _syncState.value = SyncState.Error(e.message ?: "Cloud sync failed")
                Result.failure(e)
            }
        }
    }

    // --- UPLOAD METHODS ---

    private suspend fun uploadPlots(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingPlots()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("plots").upsert(remoteList)
        syncDao.markPlotsSynced(pending.map { it.id })
    }

    private suspend fun uploadCrops(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingCrops()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("crop_assignments").upsert(remoteList)
        syncDao.markCropsSynced(pending.map { it.id })
    }

    private suspend fun uploadYieldRecords(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingYields()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("yield_records").upsert(remoteList)
        syncDao.markYieldsSynced(pending.map { it.id })
    }

    private suspend fun uploadWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingWorkers()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("workers").upsert(remoteList)
        syncDao.markWorkersSynced(pending.map { it.id })
    }

    private suspend fun uploadAttendance(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingAttendance()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("attendance").upsert(remoteList)
        pending.forEach {
            syncDao.markAttendanceSynced(it.workerId, it.date)
        }
    }

    private suspend fun uploadTransactions(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTransactions()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("worker_transactions").upsert(remoteList)
        syncDao.markTransactionsSynced(pending.map { it.id })
    }

    private suspend fun uploadDailyTasks(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTasks()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("daily_tasks").upsert(remoteList)
        syncDao.markTasksSynced(pending.map { it.id })
    }

    private suspend fun uploadTaskWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTaskWorkers()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("task_worker_assignments").upsert(remoteList)
        pending.forEach {
            syncDao.markTaskWorkerSynced(it.taskId, it.workerId)
        }
    }

    private suspend fun uploadExpenses(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingExpenses()
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("expenses").upsert(remoteList)
        syncDao.markExpensesSynced(pending.map { it.id })
    }

    // --- PULL / DOWNLOAD METHODS (Last-Write-Wins based on updatedAt) ---

    private suspend fun pullPlots(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remotePlots = client.from("plots")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<PlotRemote>()

        for (remote in remotePlots) {
            val local = syncDao.getPlotById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertPlot(remote.toEntity())
            }
        }
    }

    private suspend fun pullCrops(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteCrops = client.from("crop_assignments")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<CropAssignmentRemote>()

        for (remote in remoteCrops) {
            val local = syncDao.getCropById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertCrop(remote.toEntity())
            }
        }
    }

    private suspend fun pullYieldRecords(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteYields = client.from("yield_records")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<YieldRecordRemote>()

        for (remote in remoteYields) {
            val local = syncDao.getYieldById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertYield(remote.toEntity())
            }
        }
    }

    private suspend fun pullWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteWorkers = client.from("workers")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<WorkerRemote>()

        for (remote in remoteWorkers) {
            val local = syncDao.getWorkerById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertWorker(remote.toEntity())
            }
        }
    }

    private suspend fun pullAttendance(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteAttendance = client.from("attendance")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<AttendanceRemote>()

        for (remote in remoteAttendance) {
            val local = syncDao.getAttendance(remote.workerId, remote.date)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertAttendance(remote.toEntity())
            }
        }
    }

    private suspend fun pullTransactions(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteTransactions = client.from("worker_transactions")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<WorkerTransactionRemote>()

        for (remote in remoteTransactions) {
            val local = syncDao.getTransactionById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertTransaction(remote.toEntity())
            }
        }
    }

    private suspend fun pullDailyTasks(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteTasks = client.from("daily_tasks")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<DailyTaskRemote>()

        for (remote in remoteTasks) {
            val local = syncDao.getTaskById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertTask(remote.toEntity())
            }
        }
    }

    private suspend fun pullTaskWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteTaskWorkers = client.from("task_worker_assignments")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<TaskWorkerAssignmentRemote>()

        for (remote in remoteTaskWorkers) {
            syncDao.upsertTaskWorker(remote.toEntity())
        }
    }

    private suspend fun pullExpenses(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val remoteExpenses = client.from("expenses")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<ExpenseRemote>()

        for (remote in remoteExpenses) {
            val local = syncDao.getExpenseById(remote.id)
            val remoteUpdated = TimeUtils.toEpoch(remote.updatedAt) ?: 0L
            if (local == null || local.syncStatus != SyncStatus.PENDING || remoteUpdated >= local.updatedAt) {
                syncDao.upsertExpense(remote.toEntity())
            }
        }
    }
}
