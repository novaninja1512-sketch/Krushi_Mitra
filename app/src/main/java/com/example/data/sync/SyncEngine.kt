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

    companion object {
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_CHECKPOINT = "last_sync_checkpoint"
        private const val KEY_DEVICE_ESTABLISHED_USER_ID = "device_established_user_id"
        // 60-second overlap window to protect against clock skew or boundary race conditions
        private const val OVERLAP_WINDOW_MS = 60_000L
    }

    fun getLastSyncTime(userId: String? = null): Long {
        val uid = userId ?: authRepository.currentUser.value?.id ?: return 0L
        return prefs.getLong("${KEY_LAST_SYNC_TIME}_$uid", 0L)
    }

    private fun setLastSyncTime(userId: String, time: Long) {
        prefs.edit().putLong("${KEY_LAST_SYNC_TIME}_$userId", time).apply()
    }

    fun getLastSyncCheckpoint(userId: String): Long {
        return prefs.getLong("${KEY_LAST_SYNC_CHECKPOINT}_$userId", 0L)
    }

    fun setLastSyncCheckpoint(userId: String, time: Long) {
        prefs.edit().putLong("${KEY_LAST_SYNC_CHECKPOINT}_$userId", time).apply()
    }

    suspend fun executeFirstLoginMigrationIfNeeded(userId: String): Boolean = withContext(Dispatchers.IO) {
        val establishedUserId = prefs.getString(KEY_DEVICE_ESTABLISHED_USER_ID, null)

        if (establishedUserId == null) {
            val syncDao = database.syncDao()
            val unassignedCount = syncDao.getUnassignedRecordsCount()
            if (unassignedCount > 0) {
                Log.i(TAG, "First-login migration: Associating $unassignedCount unassigned local records to first account $userId")
                syncDao.associateAllLocalRecordsToUser(userId)
            }
            prefs.edit().putString(KEY_DEVICE_ESTABLISHED_USER_ID, userId).apply()
            true
        } else if (establishedUserId == userId) {
            false
        } else {
            Log.w(TAG, "Account B ($userId) signed in on device established by Account A ($establishedUserId). Unassigned records will not be reassigned.")
            false
        }
    }

    private var pendingCountJob: Job? = null

    private fun observePendingCount() {
        scope.launch {
            authRepository.currentUser.collect { user ->
                pendingCountJob?.cancel()
                if (user == null) {
                    val currentState = _syncState.value
                    if (currentState !is SyncState.Syncing) {
                        if (!isOnline()) {
                            _syncState.value = SyncState.Offline
                        } else {
                            _syncState.value = SyncState.Synced(0L)
                        }
                    }
                } else {
                    val userId = user.id
                    pendingCountJob = scope.launch {
                        database.syncDao().getPendingCountForUserFlow(userId)
                            .distinctUntilChanged()
                            .collect { count ->
                                val currentState = _syncState.value
                                if (currentState !is SyncState.Syncing) {
                                    if (!isOnline()) {
                                        _syncState.value = SyncState.Offline
                                    } else if (count > 0) {
                                        _syncState.value = SyncState.Pending(count)
                                    } else if (currentState !is SyncState.Synced) {
                                        _syncState.value = SyncState.Synced(getLastSyncTime(userId))
                                    }
                                }
                            }
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

    fun cancelSync() {
        syncJob?.cancel()
        val userId = authRepository.currentUser.value?.id
        _syncState.value = SyncState.Synced(getLastSyncTime(userId))
    }

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val user = authRepository.currentUser.value
            if (user == null || !authRepository.isSessionValid()) {
                Log.d(TAG, "User not authenticated or session is invalid. Sync skipped.")
                return@withContext Result.success(Unit)
            }

            val syncSessionUserId = user.id

            fun isSessionActive(): Boolean {
                val current = authRepository.currentUser.value ?: return false
                return current.id == syncSessionUserId && authRepository.isSessionValid()
            }

            if (!isOnline()) {
                _syncState.value = SyncState.Offline
                return@withContext Result.failure(IllegalStateException("No internet connection"))
            }

            val client = SupabaseClientProvider.getClient()
            if (client == null) {
                _syncState.value = SyncState.Synced(getLastSyncTime(syncSessionUserId))
                return@withContext Result.success(Unit)
            }

            _syncState.value = SyncState.Syncing

            try {
                val syncDao = database.syncDao()

                // 1. Safe first-account migration (claims legacy unassigned data only once on initial account setup)
                executeFirstLoginMigrationIfNeeded(syncSessionUserId)

                if (!isSessionActive()) {
                    Log.w(TAG, "Session changed or invalid before uploads. Aborting sync.")
                    return@withContext Result.success(Unit)
                }

                // 2. UPLOAD PHASE (Push local pending changes strictly scoped to this authenticated user)
                // Order strictly follows foreign keys
                uploadPlots(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadCrops(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadYieldRecords(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadWorkers(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadAttendance(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadTransactions(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadDailyTasks(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadTaskWorkers(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                uploadExpenses(client, syncDao, syncSessionUserId)
                if (!isSessionActive()) return@withContext Result.success(Unit)

                // 3. DOWNLOAD / INCREMENTAL PULL PHASE (Fetch cloud changes strictly for this authenticated user)
                val pullStartTime = System.currentTimeMillis()
                val lastCheckpoint = getLastSyncCheckpoint(syncSessionUserId)
                val queryCheckpoint = if (lastCheckpoint > 0L) maxOf(0L, lastCheckpoint - OVERLAP_WINDOW_MS) else 0L
                val sinceIso = if (queryCheckpoint > 0L) TimeUtils.toIso(queryCheckpoint) else null

                // Merged via version-based conflict resolution with full tombstone semantics
                pullPlots(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullCrops(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullYieldRecords(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullWorkers(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullAttendance(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullTransactions(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullDailyTasks(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullTaskWorkers(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)
                pullExpenses(client, syncDao, syncSessionUserId, sinceIso)
                if (!isSessionActive()) return@withContext Result.success(Unit)

                // Safely advance checkpoint only after all uploads & pulls succeed without failure
                setLastSyncCheckpoint(syncSessionUserId, pullStartTime)
                setLastSyncTime(syncSessionUserId, pullStartTime)
                _syncState.value = SyncState.Synced(pullStartTime)
                Log.d(TAG, "Sync completed successfully at $pullStartTime for user $syncSessionUserId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                _syncState.value = SyncState.Error(e.message ?: "Cloud sync failed")
                Result.failure(e)
            }
        }
    }

    // --- CONFLICT RESOLUTION & TOMBSTONE MERGING LOGIC ---
    /**
     * Determines whether an incoming remote entity should overwrite local entity state.
     * Rules:
     * - Deletion is treated strictly as a version with an authoritative timestamp (updatedAt).
     * - If remoteVersion > localVersion: remote wins (applies remote update or remote tombstone).
     * - If remoteVersion < localVersion: local wins (stale remote deletions cannot delete newer local records,
     *   and stale remote active updates cannot resurrect newer local tombstones).
     * - If remoteVersion == localVersion:
     *   - If local has un-synced pending changes (localSyncStatus == PENDING), protect local changes (return false).
     *   - If one is deleted and the other is active, tombstone wins tie-break (deletion precedence).
     *   - Otherwise apply remote idempotently (return true).
     */
    fun shouldApplyRemote(
        localDeletedAt: Long?,
        localUpdatedAt: Long,
        localSyncStatus: String,
        remoteDeletedAt: String?,
        remoteUpdatedAt: String?
    ): Boolean = SyncConflictResolver.shouldApplyRemote(
        localDeletedAt = localDeletedAt,
        localUpdatedAt = localUpdatedAt,
        localSyncStatus = localSyncStatus,
        remoteDeletedAt = remoteDeletedAt,
        remoteUpdatedAt = remoteUpdatedAt
    )

    // --- UPLOAD METHODS ---

    private suspend fun uploadPlots(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingPlots(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("plots").upsert(remoteList)
        syncDao.markPlotsSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadCrops(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingCrops(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("crop_assignments").upsert(remoteList)
        syncDao.markCropsSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadYieldRecords(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingYields(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("yield_records").upsert(remoteList)
        syncDao.markYieldsSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingWorkers(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("workers").upsert(remoteList)
        syncDao.markWorkersSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadAttendance(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingAttendance(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("attendance").upsert(remoteList)
        pending.forEach {
            syncDao.markAttendanceSynced(userId, it.workerId, it.date)
        }
    }

    private suspend fun uploadTransactions(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTransactions(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("worker_transactions").upsert(remoteList)
        syncDao.markTransactionsSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadDailyTasks(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTasks(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("daily_tasks").upsert(remoteList)
        syncDao.markTasksSynced(userId, pending.map { it.id })
    }

    private suspend fun uploadTaskWorkers(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingTaskWorkers(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("task_worker_assignments").upsert(remoteList)
        pending.forEach {
            syncDao.markTaskWorkerSynced(userId, it.taskId, it.workerId)
        }
    }

    private suspend fun uploadExpenses(client: io.github.jan.supabase.SupabaseClient, syncDao: com.example.data.dao.SyncDao, userId: String) {
        val pending = syncDao.getPendingExpenses(userId)
        if (pending.isEmpty()) return
        val remoteList = pending.map { it.toRemote(userId) }
        client.from("expenses").upsert(remoteList)
        syncDao.markExpensesSynced(userId, pending.map { it.id })
    }

    // --- PULL / DOWNLOAD METHODS (Last-Write-Wins with safe Tombstone handling and Incremental Checkpointing) ---

    private suspend fun pullPlots(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remotePlots = client.from("plots")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<PlotRemote>()

        for (remote in remotePlots) {
            val local = syncDao.getPlotById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertPlot(remote.toEntity())
            }
        }
    }

    private suspend fun pullCrops(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteCrops = client.from("crop_assignments")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<CropAssignmentRemote>()

        for (remote in remoteCrops) {
            val local = syncDao.getCropById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertCrop(remote.toEntity())
            }
        }
    }

    private suspend fun pullYieldRecords(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteYields = client.from("yield_records")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<YieldRecordRemote>()

        for (remote in remoteYields) {
            val local = syncDao.getYieldById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertYield(remote.toEntity())
            }
        }
    }

    private suspend fun pullWorkers(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteWorkers = client.from("workers")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<WorkerRemote>()

        for (remote in remoteWorkers) {
            val local = syncDao.getWorkerById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertWorker(remote.toEntity())
            }
        }
    }

    private suspend fun pullAttendance(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteAttendance = client.from("attendance")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<AttendanceRemote>()

        for (remote in remoteAttendance) {
            val local = syncDao.getAttendance(remote.workerId, remote.date)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertAttendance(remote.toEntity())
            }
        }
    }

    private suspend fun pullTransactions(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteTransactions = client.from("worker_transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<WorkerTransactionRemote>()

        for (remote in remoteTransactions) {
            val local = syncDao.getTransactionById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertTransaction(remote.toEntity())
            }
        }
    }

    private suspend fun pullDailyTasks(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteTasks = client.from("daily_tasks")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<DailyTaskRemote>()

        for (remote in remoteTasks) {
            val local = syncDao.getTaskById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertTask(remote.toEntity())
            }
        }
    }

    private suspend fun pullTaskWorkers(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteTaskWorkers = client.from("task_worker_assignments")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<TaskWorkerAssignmentRemote>()

        for (remote in remoteTaskWorkers) {
            val local = syncDao.getTaskWorker(remote.taskId, remote.workerId)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertTaskWorker(remote.toEntity())
            }
        }
    }

    private suspend fun pullExpenses(
        client: io.github.jan.supabase.SupabaseClient,
        syncDao: com.example.data.dao.SyncDao,
        userId: String,
        sinceIso: String? = null
    ) {
        val remoteExpenses = client.from("expenses")
            .select {
                filter {
                    eq("user_id", userId)
                    if (sinceIso != null) {
                        gte("updated_at", sinceIso)
                    }
                }
            }
            .decodeList<ExpenseRemote>()

        for (remote in remoteExpenses) {
            val local = syncDao.getExpenseById(remote.id)
            if (local == null || shouldApplyRemote(local.deletedAt, local.updatedAt, local.syncStatus, remote.deletedAt, remote.updatedAt)) {
                syncDao.upsertExpense(remote.toEntity())
            }
        }
    }
}
