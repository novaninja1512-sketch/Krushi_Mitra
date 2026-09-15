package com.example

import com.example.data.model.SyncStatus
import com.example.data.sync.SyncConflictResolver
import com.example.data.sync.model.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direct test suite for the actual production conflict-resolution implementation.
 * Tests the complete conflict matrix specified in Phase 2.3 Section 1.
 */
class ConflictMatrixProductionTest {

    private val baseEpoch = 1726308000000L
    private val t1500 = baseEpoch + 1500000L
    private val t2000 = baseEpoch + 2000000L
    private val t2500 = baseEpoch + 2500000L

    // 1. active @ 2000 vs delete @ 1500 -> EXPECTED: LOCAL (false)
    @Test
    fun testActive2000_vs_Delete1500_LocalWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t1500),
            remoteUpdatedAt = TimeUtils.toIso(t1500)
        )
        assertFalse("Active local @ 2000 must win over delete remote @ 1500", shouldApply)
    }

    // 2. active @ 2000 vs delete @ 2500 -> EXPECTED: REMOTE DELETE (true)
    @Test
    fun testActive2000_vs_Delete2500_RemoteDeleteWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2500),
            remoteUpdatedAt = TimeUtils.toIso(t2500)
        )
        assertTrue("Delete remote @ 2500 must win over active local @ 2000", shouldApply)
    }

    // 3. deleted @ 2000 vs active @ 1500 -> EXPECTED: LOCAL TOMBSTONE (false)
    @Test
    fun testDeleted2000_vs_Active1500_LocalTombstoneWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t1500)
        )
        assertFalse("Deleted local @ 2000 must win over active remote @ 1500", shouldApply)
    }

    // 4. deleted @ 2000 vs active @ 2500 -> EXPECTED: REMOTE ACTIVE (true)
    @Test
    fun testDeleted2000_vs_Active2500_RemoteActiveWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2500)
        )
        assertTrue("Active remote @ 2500 must win over deleted local @ 2000", shouldApply)
    }

    // 5. active @ 2000 vs active @ 1500 -> EXPECTED: LOCAL (false)
    @Test
    fun testActive2000_vs_Active1500_LocalWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t1500)
        )
        assertFalse("Active local @ 2000 must win over active remote @ 1500", shouldApply)
    }

    // 6. active @ 2000 vs active @ 2500 -> EXPECTED: REMOTE (true)
    @Test
    fun testActive2000_vs_Active2500_RemoteWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2500)
        )
        assertTrue("Active remote @ 2500 must win over active local @ 2000", shouldApply)
    }

    // 7. pending @ 2000 vs remote @ 2000 -> EXPECTED: deterministic tie-break (false, local pending protected)
    @Test
    fun testPending2000_vs_Remote2000_LocalPendingProtected() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.PENDING,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertFalse("Local un-synced pending change must be protected on equal timestamp", shouldApply)
    }

    // 8. synced @ 2000 vs remote @ 2500 -> EXPECTED: REMOTE (true)
    @Test
    fun testSynced2000_vs_Remote2500_RemoteWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2500)
        )
        assertTrue("Newer remote @ 2500 must win over synced local @ 2000", shouldApply)
    }

    // --- Equal Timestamps Matrix ---
    @Test
    fun testEqualTimestamps_BothActiveSynced_IdempotentTrue() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Identical active state on equal timestamp applies idempotently", shouldApply)
    }

    @Test
    fun testEqualTimestamps_BothDeletedSynced_IdempotentTrue() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2000),
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("Identical tombstone state on equal timestamp applies idempotently", shouldApply)
    }

    @Test
    fun testEqualTimestamps_LocalActive_RemoteDeleted_TombstonePrecedenceTrue() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2000),
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertTrue("On equal timestamp, deletion takes precedence over active", shouldApply)
    }

    @Test
    fun testEqualTimestamps_LocalDeleted_RemoteActive_LocalTombstonePrecedenceFalse() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000)
        )
        assertFalse("On equal timestamp, local tombstone takes precedence over active remote", shouldApply)
    }

    // --- Extremely Close Timestamps (1 millisecond precision) ---
    @Test
    fun testExtremelyCloseTimestamps_RemotePlusOneMilli_RemoteWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000 + 1L)
        )
        assertTrue("Remote +1ms must win", shouldApply)
    }

    @Test
    fun testExtremelyCloseTimestamps_RemoteMinusOneMilli_LocalWins() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2000 - 1L)
        )
        assertFalse("Local must win when remote is -1ms older", shouldApply)
    }

    // --- DeletedAt Present vs Absent with Stale/Newer combinations ---
    @Test
    fun testRemoteDeletedAtPresent_UpdatedAtAbsent_RemoteDeleteWinsIfNewer() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t2500),
            remoteUpdatedAt = null
        )
        assertTrue("Remote delete without explicit updatedAt uses deletedAt epoch and wins", shouldApply)
    }

    @Test
    fun testRemoteDeletedAtPresent_UpdatedAtAbsent_LocalWinsIfNewer() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = null,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.SYNCED,
            remoteDeletedAt = TimeUtils.toIso(t1500),
            remoteUpdatedAt = null
        )
        assertFalse("Local active record @ 2000 must win over stale remote delete @ 1500", shouldApply)
    }

    @Test
    fun testPendingLocalDeletion_vs_OlderRemoteActive() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t2000,
            localUpdatedAt = t2000,
            localSyncStatus = SyncStatus.PENDING,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t1500)
        )
        assertFalse("Pending local deletion must not be overwritten by older remote active", shouldApply)
    }

    @Test
    fun testPendingLocalDeletion_vs_NewerRemoteActive() {
        val shouldApply = SyncConflictResolver.shouldApplyRemote(
            localDeletedAt = t1500,
            localUpdatedAt = t1500,
            localSyncStatus = SyncStatus.PENDING,
            remoteDeletedAt = null,
            remoteUpdatedAt = TimeUtils.toIso(t2500)
        )
        assertTrue("Newer remote active @ 2500 wins over older local pending tombstone @ 1500", shouldApply)
    }
}
