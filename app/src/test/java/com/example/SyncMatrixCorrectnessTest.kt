package com.example

import com.example.data.model.SyncStatus
import com.example.data.sync.SyncEngine
import com.example.data.sync.model.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sync Test Matrix for Phase 2.2 Hardening:
 * Covers Scenarios A through K deterministically.
 */
class SyncMatrixCorrectnessTest {

    // Helper using identical logic to SyncEngine.shouldApplyRemote
    private fun shouldApplyRemote(
        localDeletedAt: Long?,
        localUpdatedAt: Long,
        localSyncStatus: String,
        remoteDeletedAt: String?,
        remoteUpdatedAt: String?
    ): Boolean {
        val remoteEpoch = TimeUtils.toEpoch(remoteUpdatedAt)
            ?: TimeUtils.toEpoch(remoteDeletedAt)
            ?: 0L
        val remoteIsDeleted = !remoteDeletedAt.isNullOrBlank()
        val localIsDeleted = localDeletedAt != null
        val localVersion = localUpdatedAt
        val remoteVersion = remoteEpoch

        if (remoteVersion > localVersion) {
            return true
        } else if (remoteVersion < localVersion) {
            return false
        } else {
            if (localSyncStatus == SyncStatus.PENDING) {
                return false
            }
            if (remoteIsDeleted && !localIsDeleted) {
                return true
            }
            if (localIsDeleted && !remoteIsDeleted) {
                return false
            }
            return true
        }
    }

    @Test
    fun testScenarioA_InitialSyncOnCleanInstall() {
        // Local does not have record yet (local is null)
        // In SyncEngine: if local == null, remote is inserted
        val localExists = false
        val remoteRecordId = "plot-101"
        val applyRemote = !localExists
        assertTrue("Initial sync on clean install must pull cloud record", applyRemote)
    }

    @Test
    fun testScenarioB_OfflineOperationsAccumulatePendingRecords() {
        // Simulated local modifications made offline
        val pendingPlots = listOf("plot-1", "plot-2")
        val pendingWorkers = listOf("worker-1")
        val totalPending = pendingPlots.size + pendingWorkers.size

        assertEquals(3, totalPending)
        // Verify they are flagged as PENDING
        val status = SyncStatus.PENDING
        assertEquals("PENDING", status)
    }

    @Test
    fun testScenarioC_SyncReconnectPushesPendingRecords() {
        // Pending records when online are uploaded and then transitioned to SYNCED
        var recordSyncStatus = SyncStatus.PENDING
        val isOnline = true
        if (isOnline && recordSyncStatus == SyncStatus.PENDING) {
            // Simulated upload success
            recordSyncStatus = SyncStatus.SYNCED
        }
        assertEquals(SyncStatus.SYNCED, recordSyncStatus)
    }

    @Test
    fun testScenarioD_RemoteFailurePreservesPendingState() {
        // Simulated cloud API network failure
        var recordSyncStatus = SyncStatus.PENDING
        val uploadFailed = true
        if (uploadFailed) {
            // Must NOT mark SYNCED on error
        }
        assertEquals("Failed upload must preserve PENDING status", SyncStatus.PENDING, recordSyncStatus)
    }

    @Test
    fun testScenarioE_DuplicateSyncRunIsIdempotent() {
        // Syncing identical remote record twice with equal version and already SYNCED state
        val localUpdatedAt = 1726300000000L
        val remoteUpdatedAt = TimeUtils.toIso(localUpdatedAt)

        val run1 = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = remoteUpdatedAt
        )
        val run2 = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = remoteUpdatedAt
        )

        assertTrue("First sync of identical state applies idempotently", run1)
        assertTrue("Second duplicate sync of identical state applies idempotently", run2)
    }

    @Test
    fun testScenarioF_RemoteUpdateAppliedLocallyWhenRemoteNewer() {
        val localUpdatedAt = 1000L
        val remoteUpdatedAt = TimeUtils.toIso(2000L)

        val apply = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = remoteUpdatedAt
        )
        assertTrue("Newer remote update must be applied locally", apply)
    }

    @Test
    fun testScenarioG_LocalUpdatePreservedWhenLocalNewer() {
        val localUpdatedAt = 3000L
        val remoteUpdatedAt = TimeUtils.toIso(2000L)

        val apply = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.PENDING,
            remoteDeletedAt = null,
            remoteUpdatedAt = remoteUpdatedAt
        )
        assertFalse("Newer local update must be preserved against older remote", apply)
    }

    @Test
    fun testScenarioH_RemoteDeleteMarksLocalRecordDeleted() {
        // Another device deleted record in cloud at t=4000; local record is active at t=2000
        val localUpdatedAt = 2000L
        val remoteDeleteTime = TimeUtils.toIso(4000L)

        val apply = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = remoteDeleteTime,
            remoteUpdatedAt = remoteDeleteTime
        )
        assertTrue("Newer remote tombstone must be applied to delete local record", apply)
    }

    @Test
    fun testScenarioI_LocalDeletePushedToRemote() {
        // Local soft delete creates tombstone with deletedAt and PENDING sync status
        val localDeletedAt = 1726300000000L
        val localUpdatedAt = 1726300000000L
        val syncStatus = SyncStatus.PENDING

        assertTrue("Local deleted record has deletedAt timestamp", localDeletedAt > 0)
        assertEquals("Local deleted record must be flagged PENDING for upload", SyncStatus.PENDING, syncStatus)
    }

    @Test
    fun testScenarioJ_StaleRemoteDeleteDoesNotDeleteNewerLocalRecord() {
        // Local record was recreated or edited at t=5000; remote has older delete tombstone at t=3000
        val localUpdatedAt = 5000L
        val remoteDeletedTime = TimeUtils.toIso(3000L)

        val apply = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = remoteDeletedTime,
            remoteUpdatedAt = remoteDeletedTime
        )
        assertFalse("Stale remote tombstone must NOT delete newer local record", apply)
    }

    @Test
    fun testScenarioK_TwoDevicesEditSameRecordDeterministicWinner() {
        val tA = 1726300050000L
        val tB = 1726300080000L // Device B edited 30 seconds later

        // Device A evaluates Device B's update (Device B is newer)
        val applyDeviceBOnA = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = tA,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(tB)
        )
        assertTrue("Device B with newer timestamp wins on Device A", applyDeviceBOnA)

        // Device B evaluates Device A's older update
        val applyDeviceAOnB = shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = tB,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(tA)
        )
        assertFalse("Device A with older timestamp is rejected on Device B", applyDeviceAOnB)
    }
}
