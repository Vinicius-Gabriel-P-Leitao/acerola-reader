package br.acerola.comic.pattern

object VolumeTemplatePattern {
    val presets: Map<String, String> =
        mapOf(
            "Vol. 01" to "Vol. {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Volume 01" to "Volume {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "V01" to "V{${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edicao 01" to "Edicao {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
            "Edição 01" to "Edição {${TemplateMacro.VOLUME.tag}}{${TemplateMacro.DECIMAL.tag}}",
        )

    private const val DEFAULT = "Vol. {volume}{decimal}"

    fun getTemplate(userInput: String? = null): String = userInput?.let { presets[it] ?: it } ?: DEFAULT
}
