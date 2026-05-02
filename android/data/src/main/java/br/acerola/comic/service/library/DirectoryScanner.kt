package br.acerola.comic.service.library

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.translator.persistence.toMangaDirectoryEntity
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.archive.ArchiveFormat
import br.acerola.comic.pattern.media.MediaFile
import br.acerola.comic.service.template.TemplateMatcher
import arrow.core.getOrElse
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.sort.SortType
import br.acerola.comic.util.template.templateToRegex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsabilidade única: IO puro sobre o filesystem.
 * Lista arquivos, filtra por extensão e detecta quais pastas são comics.
 * Não conhece Room nem qualquer conceito de sincronização.
 */
@Singleton
class DirectoryScanner
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val templateMatcher: TemplateMatcher,
    ) {
        suspend fun buildLibrary(
            rootUri: Uri,
            templates: List<ArchiveTemplate>,
        ): List<ComicDirectory> =
            withContext(Dispatchers.IO) {
                val comicDirectories = mutableListOf<ComicDirectory>()
                val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
                scanRecursive(rootUri, rootDocId, templates, comicDirectories)
                comicDirectories
            }

        private fun scanRecursive(
            rootUri: Uri,
            currentDocId: String,
            templates: List<ArchiveTemplate>,
            comicDirectories: MutableList<ComicDirectory>,
        ) {
            val children = ContentQueryHelper.listFiles(context, rootUri, currentDocId).getOrElse {
                AcerolaLogger.e(TAG, "Failed to list files for docId: $currentDocId", LogSource.REPOSITORY)
                return
            }

            val hasMangaFiles = children.any {
                it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR && ArchiveFormat.isSupported(it.name)
            }

            val subDirs = children.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
            val volumeTemplates = templates.filter { it.type == SortType.VOLUME }.map { it.pattern }
            val hasVolumeSubDirs = subDirs.any { subDir ->
                isVolumeName(subDir.name, volumeTemplates) && folderContainsManga(rootUri, subDir.id)
            }

            if (!hasMangaFiles && !hasVolumeSubDirs) {
                subDirs.forEach { subDir -> scanRecursive(rootUri, subDir.id, templates, comicDirectories) }
                return
            }

            val currentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, currentDocId)
            val folderDoc = DocumentFile.fromSingleUri(context, currentUri) ?: return

            val coverMetadata = children.firstOrNull { MediaFile.isCover(it.name) }
            val bannerMetadata = children.firstOrNull { MediaFile.isBanner(it.name) }
            val firstChapterName = children.firstOrNull { ArchiveFormat.isSupported(it.name) }?.name
            val detectedTemplate = firstChapterName?.let {
                templateMatcher.detect(it, templates.filter { t -> t.type == SortType.CHAPTER })
            }

            comicDirectories.add(
                folderDoc.toMangaDirectoryEntity(
                    cover = coverMetadata?.let {
                        DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                    },
                    banner = bannerMetadata?.let {
                        DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                    },
                    archiveTemplateFk = detectedTemplate?.id,
                ),
            )
        }

        fun isVolumeName(name: String, volumeTemplates: List<String>): Boolean =
            volumeTemplates.any { template -> templateToRegex(template).containsMatchIn(name) } ||
                name.startsWith("Vol", ignoreCase = true) ||
                name.startsWith("V0", ignoreCase = true)

        private fun folderContainsManga(rootUri: Uri, docId: String): Boolean {
            val children = ContentQueryHelper.listFiles(context, rootUri, docId).getOrElse { return false }
            if (children.any { it.isFile && ArchiveFormat.isSupported(it.name) }) return true
            return children.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
                .any { folderContainsManga(rootUri, it.id) }
        }

        companion object {
            private const val TAG = "DirectoryScanner"
        }
    }
