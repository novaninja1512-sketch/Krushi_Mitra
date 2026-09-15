package com.example

import com.example.data.model.SyncStatus
import com.example.data.sync.SyncConflictResolver
import com.example.data.sync.model.TimeUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adversarial Tombstone Tests exercising the actual production SyncConflictResolver.
 * Enforces the core invariant:
 * A tombstone is a version of a record. It must NOT automatically win merely because it represents deletion.
 */
class SyncTombstoneLogicTest {

    // Base epoch reference: 2026-09-14T10:00:00Z = 1726308000000L
    private val baseEpoch = 1726308000000L
    private val t1500 = baseEpoch + 1500000L // 10:25:00
    private val t1900 = baseEpoch + 1900000L // 10:31:40
    private val t2000 = baseEpoch + 2000000L // 10:33:20
    private val t2500 = baseEpoch + 2500000L // 10:41:40

    @Test
    fun testCaseA_DeviceAActive2000_DeviceBDelete1900_LocalActiveSurvives() {
        // Device A: active record updatedAt = 20:00
        // Device B: deletes same record updatedAt = 19:00
        // Expected: newer Device A record survives (shouldApplyRemote = false)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t1900),
            remoteUpdatedAt = TimeUtils.toIso(t1900)
        )
        assertFalse("Newer active record must survive stale remote deletion", shouldApply)
    }

    @Test
    fun testCaseB_DeviceAActive1900_DeviceBDelete2000_RemoteDeletionWins() {
        // Device A: active record updatedAt = 19:00
        // Device B: deletes record updatedAt = 20:00
        // Expected: deletion wins (shouldApplyRemote = true)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t1900,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2000),
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Newer remote deletion must win over older active record", shouldApply)
    }

    @Test
    fun testCaseC_LocalTombstone2000_RemoteActive1900_LocalTombstoneSurvives() {
        // Local: tombstone @ 20:00
        // Remote: active @ 19:00
        // Expected: tombstone survives (shouldApplyRemote = false)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t1900)
        )
        assertFalse("Local newer tombstone must survive stale remote active record", shouldApply)
    }

    @Test
    fun testCaseD_LocalTombstone1900_RemoteActive2000_NewerRemoteWins() {
        // Local: tombstone @ 19:00
        // Remote: active @ 20:00
        // Expected: newer remote version wins (shouldApplyRemote = true)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t1900,
            localUpdatedAt = t1900,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Newer remote active record must overwrite older local tombstone", shouldApply)
    }

    @Test
    fun testCaseE_LocalTombstone2000_RemoteTombstone1900_LocalTombstoneSurvives() {
        // Local: tombstone @ 20:00
        // Remote: tombstone @ 19:00
        // Expected: local tombstone survives (shouldApplyRemote = false)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t1900),
            remoteUpdatedAt = TimeUtils.toIso(t1900)
        )
        assertFalse("Newer local tombstone must survive older remote tombstone", shouldApply)
    }

    @Test
    fun testCaseF_LocalTombstone1900_RemoteTombstone2000_RemoteTombstoneWins() {
        // Local: tombstone @ 19:00
        // Remote: tombstone @ 20:00
        // Expected: remote tombstone wins (shouldApplyRemote = true)
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t1900,
            localUpdatedAt = t1900,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2000),
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Newer remote tombstone must overwrite older local tombstone", shouldApply)
    }

    @Test
    fun testTombstoneResurrectionAttempt_RepeatedSyncLoop() {
        // LOCAL: deleted @ 20:00
        // REMOTE: active @ 18:00
        // Run: sync, restart, sync, reconnect, sync
        // In each cycle, stale remote active record is rejected, local remains deleted.
        for (cycle in 1..5) {
            val shouldApply = SyncConflictResolver.shouldApplyRemote(
                localDeletedAt = t2000,
                localUpdatedAt = t2000,
                localSyncStatus = if (cycle == 1) SyncStatus.PENDING else SyncStatus.SYNCED,
                remoteDeletedAt = null,
                remoteUpdatedAt = TimeUtils.toIso(t1500)
            )
            assertFalse("Cycle $cycle: Stale remote resurrected local tombstone!", shouldApply)
        }
    }

    @Test
    fun testTombstoneOverwrittenByNewerRemoteActive_Allowed() {
        // LOCAL: deleted @ 18:00 (t1500)
        // REMOTE: active @ 20:00 (t2000)
        // Newer remote record is legitimately accepted per conflict policy.
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t1500,
            localUpdatedAt = t1500,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Newer remote active record must be allowed to win over older deletion", shouldApply)
    }
}
