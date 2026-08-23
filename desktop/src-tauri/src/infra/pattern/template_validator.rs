use crate::infra::{error::PatternError, pattern::template::TemplateMacro};

pub fn validate_chapter_template(input: &str) -> Result<(), PatternError> {
    let tags = extract_tags(input)?;

    let mut chapter_count = 0usize;
    let mut decimal_count = 0usize;
    let mut extension_count = 0usize;

    let mut chapter_pos: Option<usize> = None;
    let mut decimal_pos: Option<usize> = None;
    let mut extension_pos: Option<usize> = None;

    for (index, tag) in tags.iter().enumerate() {
        match TemplateMacro::from_tag(tag)? {
            TemplateMacro::Chapter => {
                chapter_count += 1;
                if chapter_pos.is_none() {
                    chapter_pos = Some(index);
                }
            },
            TemplateMacro::Decimal => {
                decimal_count += 1;
                if decimal_pos.is_none() {
                    decimal_pos = Some(index);
                }
            },
            TemplateMacro::Extension => {
                extension_count += 1;
                if extension_pos.is_none() {
                    extension_pos = Some(index);
                }
            },
            TemplateMacro::Volume => {},
        }
    }

    if chapter_count != 1 {
        return Err(PatternError::ChapterRequired);
    }
    if extension_count != 1 {
        return Err(PatternError::ExtensionRequired);
    }
    if decimal_count > 1 {
        return Err(PatternError::DecimalDuplicate);
    }

    if let (Some(ch), Some(dec)) = (chapter_pos, decimal_pos) {
        if dec < ch {
            return Err(PatternError::DecimalBeforeChapter);
        }
    }
    if let (Some(ch), Some(ext)) = (chapter_pos, extension_pos) {
        if ext < ch {
            return Err(PatternError::ExtensionBeforeChapter);
        }
    }
    if let (Some(dec), Some(ext)) = (decimal_pos, extension_pos) {
        if ext < dec {
            return Err(PatternError::ExtensionBeforeDecimal);
        }
    }
    if !input.trim_end().ends_with("{extension}") {
        return Err(PatternError::ExtensionNotAtEnd);
    }

    Ok(())
}

pub fn validate_volume_template(input: &str) -> Result<(), PatternError> {
    let tags = extract_tags(input)?;

    let mut volume_count = 0usize;
    let mut decimal_count = 0usize;
    let mut volume_pos: Option<usize> = None;
    let mut extension_pos: Option<usize> = None;

    for (index, tag) in tags.iter().enumerate() {
        match TemplateMacro::from_tag(tag)? {
            TemplateMacro::Volume => {
                volume_count += 1;
                if volume_pos.is_none() {
                    volume_pos = Some(index);
                }
            },
            TemplateMacro::Decimal => {
                decimal_count += 1;
            },
            TemplateMacro::Extension => {
                if extension_pos.is_none() {
                    extension_pos = Some(index);
                }
            },
            TemplateMacro::Chapter => {},
        }
    }

    if volume_count != 1 {
        return Err(PatternError::VolumeRequired);
    }
    if decimal_count > 1 {
        return Err(PatternError::DecimalDuplicate);
    }
    if let (Some(vol), Some(ext)) = (volume_pos, extension_pos) {
        if ext < vol {
            return Err(PatternError::ExtensionBeforeChapter);
        }
    }

    Ok(())
}

pub fn extract_tags(input: &str) -> Result<Vec<String>, PatternError> {
    let mut inside = false;
    let mut buffer = String::new();
    let mut result = Vec::new();

    for it in input.chars() {
        match it {
            '{' => {
                if inside {
                    return Err(PatternError::MalformedMacro);
                }

                inside = true;
                buffer.clear();
            },
            '}' => {
                if inside {
                    result.push(buffer.clone());
                    inside = false;
                }
            },
            _ => {
                if inside {
                    buffer.push(it);
                }
            },
        }
    }

    if inside {
        return Err(PatternError::MalformedMacro);
    }

    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::infra::error::PatternError;

    // NOTE: extract_tags

    #[test]
    fn extrai_tags_validas() {
        let result = extract_tags("{chapter}{decimal}.*.{extension}");
        assert_eq!(result.unwrap(), vec!["chapter", "decimal", "extension"]);
    }

    #[test]
    fn extrai_sem_macros() {
        assert_eq!(extract_tags("arquivo.cbz").unwrap(), Vec::<String>::new());
    }

    #[test]
    fn erro_chave_sem_fechamento() {
        assert!(matches!(extract_tags("{chapter"), Err(PatternError::MalformedMacro)));
    }

    #[test]
    fn erro_chave_dupla_abertura() {
        assert!(matches!(extract_tags("{{chapter}"), Err(PatternError::MalformedMacro)));
    }

    #[test]
    fn tag_vazia_e_unknown_macro() {
        assert!(matches!(
            validate_chapter_template("{}.{extension}"),
            Err(PatternError::UnknownMacro(tag)) if tag.is_empty()
        ));
    }

    // NOTE: validate_chapter_template

    #[test]
    fn template_valido_completo() {
        assert!(validate_chapter_template("Ch. {chapter}{decimal}.*.{extension}").is_ok());
    }

    #[test]
    fn template_valido_sem_decimal() {
        assert!(validate_chapter_template("Ch. {chapter}.*.{extension}").is_ok());
    }

    #[test]
    fn erro_sem_chapter() {
        assert!(matches!(
            validate_chapter_template("{decimal}.*.{extension}"),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn erro_chapter_duplicado() {
        assert!(matches!(
            validate_chapter_template("{chapter}{chapter}.*.{extension}"),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn erro_decimal_duplicado() {
        assert!(matches!(
            validate_chapter_template("{chapter}{decimal}{decimal}.*.{extension}"),
            Err(PatternError::DecimalDuplicate)
        ));
    }

    #[test]
    fn erro_sem_extension() {
        assert!(matches!(
            validate_chapter_template("{chapter}{decimal}.*"),
            Err(PatternError::ExtensionRequired)
        ));
    }

    #[test]
    fn erro_decimal_antes_de_chapter() {
        assert!(matches!(
            validate_chapter_template("{decimal}{chapter}.*.{extension}"),
            Err(PatternError::DecimalBeforeChapter)
        ));
    }

    #[test]
    fn erro_extension_antes_de_chapter() {
        // Só uma extension: ordem é verificada depois da contagem
        assert!(matches!(
            validate_chapter_template("{extension}{chapter}.*"),
            Err(PatternError::ExtensionBeforeChapter)
        ));
    }

    #[test]
    fn erro_extension_antes_de_decimal() {
        assert!(matches!(
            validate_chapter_template("{chapter}{extension}{decimal}"),
            Err(PatternError::ExtensionBeforeDecimal)
        ));
    }

    #[test]
    fn erro_extension_nao_encerra() {
        assert!(matches!(
            validate_chapter_template("{chapter}.*.{extension}.extra"),
            Err(PatternError::ExtensionNotAtEnd)
        ));
    }

    #[test]
    fn erro_macro_desconhecida_chapter() {
        assert!(matches!(
            validate_chapter_template("{titulo}.*.{extension}"),
            Err(PatternError::UnknownMacro(tag)) if tag == "titulo"
        ));
    }

    #[test]
    fn erro_macro_malformada_chapter() {
        assert!(matches!(validate_chapter_template("{chapter"), Err(PatternError::MalformedMacro)));
    }

    // NOTE: validate_volume_template

    #[test]
    fn volume_template_valido() {
        assert!(validate_volume_template("Vol. {volume}{decimal}").is_ok());
    }

    #[test]
    fn volume_template_valido_sem_decimal() {
        assert!(validate_volume_template("Vol. {volume}").is_ok());
    }

    #[test]
    fn erro_volume_ausente() {
        assert!(matches!(
            validate_volume_template("Vol. {decimal}"),
            Err(PatternError::VolumeRequired)
        ));
    }

    #[test]
    fn erro_volume_duplicado() {
        assert!(matches!(
            validate_volume_template("{volume}{volume}"),
            Err(PatternError::VolumeRequired)
        ));
    }

    #[test]
    fn erro_decimal_duplicado_volume() {
        assert!(matches!(
            validate_volume_template("{volume}{decimal}{decimal}"),
            Err(PatternError::DecimalDuplicate)
        ));
    }

    #[test]
    fn erro_macro_desconhecida_volume() {
        assert!(matches!(
            validate_volume_template("{titulo}"),
            Err(PatternError::UnknownMacro(tag)) if tag == "titulo"
        ));
    }
}
