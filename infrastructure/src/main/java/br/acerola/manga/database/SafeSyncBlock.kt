package br.acerola.manga.database

import android.database.sqlite.SQLiteException
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.error.exception.SyncError
import br.acerola.manga.error.exception.SyncException
import java.io.IOException

@Deprecated("Use Either.catch for database operations")
suspend inline fun <T> safeSyncBlock(block: suspend () -> T): Result<T> =
    try {
        Result.success(value = block())
    } catch (integrityException: IntegrityException) {
        Result.failure(exception = SyncException(error = SyncError.Skipped(cause = integrityException)))
    } catch (sQLiteException: SQLiteException) {
        Result.failure(exception = SyncException(error = SyncError.Fatal(cause = sQLiteException)))
    } catch (iOException: IOException) {
        Result.failure(exception = SyncException(error = SyncError.Recoverable(cause = iOException)))
    }
