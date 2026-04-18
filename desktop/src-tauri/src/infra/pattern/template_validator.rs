use crate::infra::{
    error::messages::pattern_error::PatternError, pattern::chapter_template::TemplateMacro,
};

pub fn validate_template(
    input: &str, extract: impl Fn(&str) -> Result<Vec<String>, PatternError>,
) -> Result<(), PatternError> {
    let tags = extract(input)?;

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
        }
    }

    if let (Some(ch), Some(dec)) = (chapter_pos, decimal_pos) {
        if dec < ch {
            return Err(PatternError::DecimalBeforeChapter);
        }
    }

    if let (Some(ext), Some(ch)) = (extension_pos, chapter_pos) {
        if ext < ch {
            return Err(PatternError::ExtensionBeforeChapter);
        }
    }

    if let (Some(sub), Some(ext)) = (decimal_pos, extension_pos) {
        if ext < sub {
            return Err(PatternError::ExtensionBeforeDecimal);
        }
    }

    if chapter_count != 1 {
        return Err(PatternError::ChapterRequired);
    }

    if decimal_count > 1 {
        return Err(PatternError::DecimalDuplicate);
    }

    if extension_count != 1 {
        return Err(PatternError::ExtensionRequired);
    }

    if !input.trim_end().ends_with("{extension}") {
        return Err(PatternError::ExtensionNotAtEnd);
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
    use crate::infra::error::messages::pattern_error::PatternError;

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
    fn tag_vazia_passa_para_validador() {
        assert_eq!(
            extract_tags("{}{chapter}.*.{extension}").unwrap(),
            vec!["", "chapter", "extension"]
        );
    }

    // NOTE: validate_template — usando extract_tags real

    #[test]
    fn template_valido_completo() {
        assert!(validate_template("Ch. {chapter}{decimal}.*.{extension}", extract_tags).is_ok());
    }

    #[test]
    fn template_valido_sem_decimal() {
        assert!(validate_template("Ch. {chapter}.*.{extension}", extract_tags).is_ok());
    }

    #[test]
    fn erro_sem_chapter() {
        assert!(matches!(
            validate_template("{decimal}.*.{extension}", extract_tags),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn erro_chapter_duplicado() {
        assert!(matches!(
            validate_template("{chapter}{chapter}.*.{extension}", extract_tags),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn erro_decimal_duplicado() {
        assert!(matches!(
            validate_template("{chapter}{decimal}{decimal}.*.{extension}", extract_tags),
            Err(PatternError::DecimalDuplicate)
        ));
    }

    #[test]
    fn erro_sem_extension() {
        assert!(matches!(
            validate_template("{chapter}{decimal}.*", extract_tags),
            Err(PatternError::ExtensionRequired)
        ));
    }

    #[test]
    fn erro_decimal_antes_de_chapter() {
        assert!(matches!(
            validate_template("{decimal}{chapter}.*.{extension}", extract_tags),
            Err(PatternError::DecimalBeforeChapter)
        ));
    }

    #[test]
    fn erro_extension_antes_de_chapter() {
        assert!(matches!(
            validate_template("{extension}{chapter}.*.{extension}", extract_tags),
            Err(PatternError::ExtensionBeforeChapter)
        ));
    }

    #[test]
    fn erro_extension_antes_de_decimal() {
        assert!(matches!(
            validate_template("{chapter}{extension}{decimal}", extract_tags),
            Err(PatternError::ExtensionBeforeDecimal)
        ));
    }

    #[test]
    fn erro_extension_nao_encerra() {
        assert!(matches!(
            validate_template("{chapter}.*.{extension}.extra", extract_tags),
            Err(PatternError::ExtensionNotAtEnd)
        ));
    }

    // NOTE: validate_template — usando mock para isolar o validador

    #[test]
    fn mock_tag_desconhecida() {
        let result = validate_template("qualquer", |_| {
            Ok(vec!["titulo".into(), "chapter".into(), "extension".into()])
        });
        assert!(matches!(result, Err(PatternError::UnknownMacro(tag)) if tag == "titulo"));
    }

    #[test]
    fn mock_macro_malformada() {
        let result = validate_template("qualquer", |_| Err(PatternError::MalformedMacro));
        assert!(matches!(result, Err(PatternError::MalformedMacro)));
    }

    #[test]
    fn mock_tag_vazia_vira_unknown() {
        let result = validate_template("qualquer", |_| {
            Ok(vec!["".into(), "chapter".into(), "extension".into()])
        });
        assert!(matches!(result, Err(PatternError::UnknownMacro(tag)) if tag.is_empty()));
    }
}
