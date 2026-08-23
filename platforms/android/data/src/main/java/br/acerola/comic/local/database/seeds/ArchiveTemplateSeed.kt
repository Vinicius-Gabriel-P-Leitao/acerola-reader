package br.acerola.comic.local.database.seeds

import androidx.sqlite.db.SupportSQLiteDatabase
import br.acerola.comic.pattern.template.TemplateMacro
import br.acerola.comic.util.sort.SortType

fun seedArchiveTemplates(db: SupportSQLiteDatabase) {
    val chapterPresets =
        mapOf(
            "01.*." to
                "{${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                ".*.{${TemplateMacro.EXTENSION.tag}}",
            "Ch. 01.*." to
                "Ch. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                ".*.{${TemplateMacro.EXTENSION.tag}}",
            "Cap. 01.*." to
                "Cap. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                ".*.{${TemplateMacro.EXTENSION.tag}}",
            "chapter 01.*." to
                "chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}" +
                ".*.{${TemplateMacro.EXTENSION.tag}}",
        )

    val volumePresets =
        mapOf(
            "Vol. 01" to "Vol. {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Volume 01" to "Volume {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "V01" to "V{${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edicao 01" to "Edicao {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edição 01" to "Edição {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
        )

    var index = 1L
    chapterPresets.forEach { (label, pattern) ->
        db.execSQL(
            "INSERT OR IGNORE INTO archive_template (id, label, pattern, type, is_default, priority) VALUES (?, ?, ?, ?, 1, 0)",
            arrayOf<Any>(-index, label, pattern, SortType.CHAPTER.name),
        )
        index++
    }

    volumePresets.forEach { (label, pattern) ->
        db.execSQL(
            "INSERT OR IGNORE INTO archive_template (id, label, pattern, type, is_default, priority) VALUES (?, ?, ?, ?, 1, 0)",
            arrayOf<Any>(-index, label, pattern, SortType.VOLUME.name),
        )
        index++
    }
}
