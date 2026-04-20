package br.acerola.comic.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.error.message.IoError

data class FastFileMetadata(
    val id: String,
    val size: Long,
    val name: String,
    val mimeType: String,
    val lastModified: Long,
)

fun DocumentFile.toFastMetadata() =
    FastFileMetadata(
        mimeType = "",
        size = length(),
        name = name ?: "",
        lastModified = lastModified(),
        id = DocumentsContract.getDocumentId(uri),
    )

object ContentQueryHelper {
    fun listFiles(
        context: Context,
        treeUri: Uri,
        parentDocumentId: String? = null,
    ): Either<IoError, List<FastFileMetadata>> =
        Either
            .catch {
                val documentId = parentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

                val projection =
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    )

                val files = mutableListOf<FastFileMetadata>()

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val lastModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = if (idIndex != -1) cursor.getString(idIndex) ?: continue else continue
                        val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                        val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        val lastModified = if (lastModifiedIndex != -1) cursor.getLong(lastModifiedIndex) else 0L
                        val mimeType = if (mimeTypeIndex != -1) cursor.getString(mimeTypeIndex) ?: "" else ""

                        files.add(FastFileMetadata(id = id, name = name, size = size, lastModified = lastModified, mimeType = mimeType))
                    }
                }

                files
            }.mapLeft { cause ->
                IoError.FileReadError(path = treeUri.toString(), cause = cause)
            }
}
