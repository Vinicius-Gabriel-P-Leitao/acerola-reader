package br.acerola.comic.worker.contract

enum class SyncType {
    INCREMENTAL,
    REFRESH,
    REBUILD,
    SPECIFIC,
    SYNC,
    RESCAN,
    ;

    companion object {
        fun from(value: String?): SyncType =
            when (value?.lowercase()) {
                "incremental" -> INCREMENTAL
                "refresh" -> REFRESH
                "rebuild" -> REBUILD
                "specific" -> SPECIFIC
                "sync" -> SYNC
                "rescan" -> RESCAN
                else -> INCREMENTAL
            }
    }
}
