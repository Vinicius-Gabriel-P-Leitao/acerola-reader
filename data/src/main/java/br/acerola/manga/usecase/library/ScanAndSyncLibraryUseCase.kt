package br.acerola.manga.usecase.library

import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.config.permission.FileSystemAccessManager
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import br.acerola.manga.usecase.metadata.SyncMangaMetadataUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanAndSyncLibraryUseCase @Inject constructor(
    private val syncLibraryUseCase: SyncLibraryUseCase<MangaDirectoryDto>,
    private val observeLibraryUseCase: ObserveLibraryUseCase<MangaDirectoryDto>,
    private val syncMangaMetadataUseCase: SyncMangaMetadataUseCase,
    private val fileSystemAccessManager: FileSystemAccessManager
) {

    suspend fun execute(baseUri: Uri?) {
        syncLibraryUseCase.sync(baseUri)

        val directories = observeLibraryUseCase().first()
        val rootUri = fileSystemAccessManager.folderUri ?: return

        directories.filter { it.hasComicInfo }.forEach { dir ->
            // NOTE: Aqui fazemos o sync automático.
            // Precisamos do mangaId (remoteInfo?.id)
            // Mas no momento do scan, talvez ele ainda não tenha remoteInfo.
            syncMangaMetadataUseCase.syncFromComicInfo(
                mangaId = -1L, // Tenta criar ou atualizar pelo título
                folderId = dir.id,
                title = dir.name,
                folderUri = dir.path.toUri(),
                rootUri = rootUri
            )
        }
    }
}
