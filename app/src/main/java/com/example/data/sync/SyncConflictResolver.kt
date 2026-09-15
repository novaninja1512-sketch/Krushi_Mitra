package com.example.data.sync

import com.example.data.model.SyncStatus
import com.example.data.sync.model.TimeUtils

/**
 * Pure production conflict resolution and tombstone merging component.
 * Authoritatively determines whether an incoming remote entity overwrites local entity state.
 */
object SyncConflictResolver {
    /**
     * Determines whether an incoming remote entity should overwrite local entity state.
     * Rules:
     * - Deletion is treated strictly as a version with an authoritative timestamp.
     * - Version timestamps consider both updatedAt and deletedAt (the latest activity time).
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
    ): Boolean {
        val remoteUpdatedEpoch = TimeUtils.toEpoch(remoteUpdatedAt) ?: 0L
        val remoteDeletedEpoch = TimeUtils.toEpoch(remoteDeletedAt) ?: 0L
        val remoteEpoch = maxOf(remoteUpdatedEpoch, remoteDeletedEpoch)

        val localEpoch = maxOf(localUpdatedAt, localDeletedAt ?: 0L)
        val remoteIsDeleted = !remoteDeletedAt.isNullOrBlank()
        val localIsDeleted = localDeletedAt != null

        if (remoteEpoch > localEpoch) {
            // Strictly newer remote version wins (whether active or tombstone)
            return true
        } else if (remoteEpoch < localEpoch) {
            // Strictly newer local version wins (whether active or tombstone)
            // Stale remote tombstone does NOT delete newer local record
            // Stale remote active record does NOT resurrect newer local tombstone
            return false
        } else {
            // Identical version / timestamp tie-breaking
            if (localSyncStatus == SyncStatus.PENDING) {
                // Local un-synced user changes take precedence
                return false
            }
            if (remoteIsDeleted && !localIsDeleted) {
                return true
            }
            if (localIsDeleted && !remoteIsDeleted) {
                return false
            }
            // Identical state and timestamp -> idempotent apply
            return true
        }
    }
}
