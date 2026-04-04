package br.acerola.manga.service.template

import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.local.dao.archive.ChapterTemplateDao
import br.acerola.manga.local.entity.archive.ChapterTemplate
import br.acerola.manga.dto.archive.ChapterTemplateDto
import br.acerola.manga.local.translator.persistence.toDto
import br.acerola.manga.pattern.TemplateMacro

import br.acerola.manga.pattern.TemplateValidatorPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterNameProcessor @Inject constructor(
    private val dao: ChapterTemplateDao
) {
    fun observeTemplates(): Flow<List<ChapterTemplate>> = dao.observeAll()

    fun observeTemplatesAsDto(): Flow<List<ChapterTemplateDto>> = dao.observeAll()
        .map { list -> list.map { it.toDto() } }

    suspend fun getTemplates(): List<ChapterTemplate> = dao.getAll()

    suspend fun getTemplatesAsDto(): List<ChapterTemplateDto> = dao.getAll().map { it.toDto() }

    suspend fun addTemplate(label: String, pattern: String): Either<TemplateError, Unit> {
        val trimmed = pattern.trim()
        val extensionTag = "{${TemplateMacro.EXTENSION.tag}}"

        val transformedPattern = when {
            !trimmed.contains(extensionTag) -> {
                val connector = if (trimmed.endsWith("*") || trimmed.endsWith(".")) "" else "*"
                "$trimmed$connector$extensionTag"
            }
            else -> trimmed.substring(0, trimmed.indexOf(extensionTag) + extensionTag.length)
        }

        return TemplateValidatorPattern.validateCustomTemplate(transformedPattern)
            .flatMap {
                val entity = createCustomTemplate(label.trim(), transformedPattern)
                val insertedId = dao.insert(entity)
                if (insertedId == -1L) Either.Left(TemplateError.Duplicate)
                else Either.Right(Unit)
            }
    }

    private fun createCustomTemplate(label: String, pattern: String): ChapterTemplate {
        return ChapterTemplate(
            label = label,
            pattern = pattern,
            isDefault = false,
            priority = 1
        )
    }

    suspend fun removeTemplate(id: Long): Either<TemplateError, Unit> {
        val deleted = dao.deleteCustom(id)
        return if (deleted > 0) Either.Right(Unit)
        else Either.Left(TemplateError.SystemProtected)
    }
}
