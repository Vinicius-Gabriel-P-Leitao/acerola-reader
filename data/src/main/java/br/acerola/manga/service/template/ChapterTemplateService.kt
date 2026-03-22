package br.acerola.manga.service.template

import arrow.core.Either
import br.acerola.manga.error.message.TemplateError
import br.acerola.manga.local.dao.archive.ChapterTemplateDao
import br.acerola.manga.local.entity.archive.ChapterTemplateEntity
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
        val extensionMacro = "{extension}"
        
        val transformedPattern = if (!trimmed.contains(extensionMacro)) {
            // Apenas anexa o curinga e a extensão de forma limpa
            if (trimmed.endsWith("*") || trimmed.endsWith(".")) {
                "$trimmed$extensionMacro"
            } else {
                "$trimmed*$extensionMacro"
            }
        } else {
            // Garante que nada exista após a macro {extension}
            val index = trimmed.indexOf(extensionMacro)
            trimmed.substring(0, index + extensionMacro.length)
        }

        val validation = TemplateValidator.validateCustomTemplate(transformedPattern)
        if (validation.isLeft()) {
            return Either.Left(validation.leftOrNull()!!)
        }

        // TODO: Fazer toModel
        val entity = ChapterTemplateEntity(
            label = label.trim(),
            pattern = transformedPattern,
            isDefault = false,
            priority = 1
        )

        val insertedId = dao.insert(entity)
        if (insertedId == -1L) {
            return Either.Left(TemplateError.Duplicate)
        }

        return Either.Right(Unit)
    }

    suspend fun removeTemplate(id: Long): Either<TemplateError, Unit> {
        val deleted = dao.deleteCustom(id)
        return if (deleted > 0) Either.Right(Unit)
        else Either.Left(TemplateError.SystemProtected)
    }
}
