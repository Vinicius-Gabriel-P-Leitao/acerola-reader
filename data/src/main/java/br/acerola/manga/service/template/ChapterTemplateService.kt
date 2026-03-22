package br.acerola.manga.service.template

import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.local.dao.archive.ChapterTemplateDao
import br.acerola.manga.local.entity.archive.ChapterTemplateEntity
import br.acerola.manga.pattern.TemplateMacro
import br.acerola.manga.pattern.TemplateValidator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterTemplateService @Inject constructor(
    private val dao: ChapterTemplateDao
) {
    fun observeTemplates(): Flow<List<ChapterTemplateEntity>> = dao.observeAll()

    suspend fun getTemplates(): List<ChapterTemplateEntity> = dao.getAll()

    suspend fun getTemplateById(id: Long): ChapterTemplateEntity? = dao.getById(id)

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

        return TemplateValidator.validateCustomTemplate(transformedPattern)
            .flatMap {
                val entity = createCustomTemplate(label.trim(), transformedPattern)
                val insertedId = dao.insert(entity)
                if (insertedId == -1L) Either.Left(TemplateError.Duplicate)
                else Either.Right(Unit)
            }
    }

    private fun createCustomTemplate(label: String, pattern: String): ChapterTemplateEntity {
        return ChapterTemplateEntity(
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
