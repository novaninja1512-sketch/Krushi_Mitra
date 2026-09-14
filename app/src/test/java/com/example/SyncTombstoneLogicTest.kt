package com.example

import com.example.data.model.SyncStatus
import com.example.data.sync.model.TimeUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTombstoneLogicTest {

    // Helper implementing the conflict/tombstone resolution logic
    private fun shouldApplyRemote(
        localDeletedAt: Long?,
        localUpdatedAt: Long,
        localSyncStatus: String,
        remoteDeletedAt: String?,
        remoteUpdatedAt: String?
    ): Boolean {
        val remoteEpoch = TimeUtils.toEpoch(remoteUpdatedAt) ?: 0L
        val remoteIsDeleted = !remoteDeletedAt.isNullOrBlank()
        val localIsDeleted = localDeletedAt != null

        // Rule 1: Stale remote copy must NOT overwrite a local tombstone
        if (localIsDeleted && !remoteIsDeleted) {
            return false
        }

        // Rule 2: Remote tombstone takes precedence over local active record
        if (remoteIsDeleted && !localIsDeleted) {
            return true
        }

        // Rule 3: Local pending un-synced changes newer than remote take precedence
        if (localSyncStatus == SyncStatus.PENDING && localUpdatedAt > remoteEpoch) {
            return false
        }

        // Rule 4: Otherwise apply remote
        return true
    }

    @Test
    fun testLocalTombstoneIsNotOverwrittenByStaleRemoteActiveRecord() {
        // Scenario:
        // 1. Record was created and synced
        // 2. User deletes record locally at t=2000 (tombstone)
        // 3. Sync engine pulls from cloud which has active record with t=1500 or even t=2500
        val localDeletedAt = 2000L
        val localUpdatedAt = 2000L
        val localSyncStatus = SyncStatus.PENDING
        val remoteDeletedAt: String? = null
        val remoteUpdatedAt = "2026-09-14T10:00:00Z"

        val shouldApply = shouldApplyRemote(
            localDeletedAt = localDeletedAt,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = localSyncStatus,
            remoteDeletedAt = remoteDeletedAt,
            remoteUpdatedAt = remoteUpdatedAt
        )

        // Stale remote MUST NOT revive/resurrect the local record
        assertFalse("Local tombstone was resurrected by remote active record!", shouldApply)
    }

    @Test
    fun testRemoteTombstoneDeletesLocalActiveRecord() {
        // Scenario:
        // Another device deleted the record in the cloud.
        // Local record is still active (localDeletedAt == null).
        val localDeletedAt: Long? = null
        val localUpdatedAt = 1000L
        val localSyncStatus = SyncStatus.SYNCED
        val remoteDeletedAt = "2026-09-14T12:00:00Z"
        val remoteUpdatedAt = "2026-09-14T12:00:00Z"

        val shouldApply = shouldApplyRemote(
            localDeletedAt = localDeletedAt,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = localSyncStatus,
            remoteDeletedAt = remoteDeletedAt,
            remoteUpdatedAt = remoteUpdatedAt
        )

        // Remote tombstone must be accepted so local deletes the record
        assertTrue("Remote tombstone was not applied locally!", shouldApply)
    }

    @Test
    fun testLocalPendingEditTakesPrecedenceOverOlderRemote() {
        // Scenario:
        // Local has pending edit at t=5000
        // Remote has older update at t=4000
        val localDeletedAt: Long? = null
        val localUpdatedAt = 1726300000000L
        val localSyncStatus = SyncStatus.PENDING
        val remoteDeletedAt: String? = null
        // Remote time is 1 hour before local
        val remoteUpdatedAt = TimeUtils.toIso(localUpdatedAt - 3600000L)

        val shouldApply = shouldApplyRemote(
            localDeletedAt = localDeletedAt,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = localSyncStatus,
            remoteDeletedAt = remoteDeletedAt,
            remoteUpdatedAt = remoteUpdatedAt
        )

        assertFalse("Older remote update overwrote local pending edit!", shouldApply)
    }

    @Test
    fun testNewerRemoteUpdateAppliesToSyncedLocal() {
        // Scenario:
        // Local record is SYNCED at t=1000
        // Remote has newer update at t=2000
        val localDeletedAt: Long? = null
        val localUpdatedAt = 1000L
        val localSyncStatus = SyncStatus.SYNCED
        val remoteDeletedAt: String? = null
        val remoteUpdatedAt = TimeUtils.toIso(2000L)

        val shouldApply = shouldApplyRemote(
            localDeletedAt = localDeletedAt,
            localUpdatedAt = localUpdatedAt,
            localSyncStatus = localSyncStatus,
            remoteDeletedAt = remoteDeletedAt,
            remoteUpdatedAt = remoteUpdatedAt
        )

        assertTrue("Newer remote update was rejected!", shouldApply)
    }
}
