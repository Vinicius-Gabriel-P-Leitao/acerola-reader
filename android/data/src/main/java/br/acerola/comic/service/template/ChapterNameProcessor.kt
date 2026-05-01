package br.acerola.comic.service.template

import android.database.sqlite.SQLiteConstraintException
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.comic.dto.archive.ArchiveTemplateDto
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.local.dao.archive.ArchiveTemplateDao
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.local.translator.persistence.toDto
import br.acerola.comic.pattern.template.TemplateMacro
import br.acerola.comic.pattern.template.TemplateValidator
import br.acerola.comic.util.sort.SortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterNameProcessor
    @Inject
    constructor(
        private val dao: ArchiveTemplateDao,
    ) {
        fun observeTemplates(): Flow<List<ArchiveTemplate>> = dao.observeAllTemplates()

        fun observeTemplatesAsDto(): Flow<List<ArchiveTemplateDto>> =
            dao
                .observeAllTemplates()
                .map { list -> list.map { it.toDto() } }

        suspend fun getTemplates(): List<ArchiveTemplate> = dao.getAllTemplates()

        suspend fun getTemplatesAsDto(): List<ArchiveTemplateDto> = dao.getAllTemplates().map { it.toDto() }

        suspend fun addTemplate(
            label: String,
            pattern: String,
            type: SortType,
        ): Either<TemplateError, Unit> {
            val transformedPattern = transformPattern(pattern.trim(), type)

            return TemplateValidator
                .validateCustomTemplate(transformedPattern)
                .flatMap {
                    val entity = createCustomTemplate(label.trim(), transformedPattern, type)
                    val insertedId = dao.insert(entity)
                    if (insertedId == -1L) {
                        Either.Left(TemplateError.Duplicate)
                    } else {
                        Either.Right(Unit)
                    }
                }
        }

        private fun createCustomTemplate(
            label: String,
            pattern: String,
            type: SortType,
        ): ArchiveTemplate =
            ArchiveTemplate(
                label = label,
                pattern = pattern,
                type = type,
                isDefault = false,
                priority = 1,
            )

        suspend fun updateTemplate(
            id: Long,
            label: String,
            pattern: String,
            type: SortType,
        ): Either<TemplateError, Unit> {
            val existing =
                dao.getTemplateById(id)
                    ?: return Either.Left(TemplateError.SystemProtected)

            if (existing.isDefault) return Either.Left(TemplateError.SystemProtected)

            val transformedPattern = transformPattern(pattern.trim(), type)

            return TemplateValidator
                .validateCustomTemplate(transformedPattern)
                .flatMap {
                    val updated = existing.copy(label = label.trim(), pattern = transformedPattern, type = type)
                    try {
                        dao.update(updated)
                        Either.Right(Unit)
                    } catch (e: SQLiteConstraintException) {
                        Either.Left(TemplateError.Duplicate)
                    }
                }
        }

        suspend fun removeTemplate(id: Long): Either<TemplateError, Unit> {
            val deleted = dao.deleteNonDefaultTemplate(id)
            return if (deleted > 0) {
                Either.Right(Unit)
            } else {
                Either.Left(TemplateError.SystemProtected)
            }
        }

        private fun transformPattern(
            trimmed: String,
            type: SortType,
        ): String {
            if (type == SortType.VOLUME) return trimmed

            val extensionTag = "{${TemplateMacro.EXTENSION.tag}}"
            return when {
                !trimmed.contains(extensionTag) -> {
                    val connector = if (trimmed.endsWith("*") || trimmed.endsWith(".")) "" else "*"
                    "$trimmed$connector$extensionTag"
                }
                else -> trimmed.substring(0, trimmed.indexOf(extensionTag) + extensionTag.length)
            }
        }
    }
