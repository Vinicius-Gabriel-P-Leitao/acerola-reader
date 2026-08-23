package br.acerola.comic.local.entity.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of the persisted history of P2P sync sessions (history and files) — terminal
 * states only (`complete`/`error`), not `started`/`progress`. Mirrors `sync_history_log` on
 * Desktop (`infra/db/migrations/models/sync/001_create_sync_history_log.sql`). Exists to
 * survive an app restart; [br.acerola.comic.service.network.P2pEventBus] remains the source
 * of live events during the current session.
 */
@Entity(
    tableName = "sync_history_log",
    indices = [Index(value = ["created_at"])],
)
data class SyncHistoryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "peer_id")
    val peerId: String,
    /** `"history"` or `"files"`. */
    @ColumnInfo(name = "kind")
    val kind: String,
    /** `"complete"` or `"error"`. */
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "message")
    val message: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
