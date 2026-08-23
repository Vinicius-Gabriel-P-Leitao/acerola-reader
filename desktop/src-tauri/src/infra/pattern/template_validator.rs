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
    fn test_extracts_valid_tags() {
        let result = extract_tags("{chapter}{decimal}.*.{extension}");
        assert_eq!(result.unwrap(), vec!["chapter", "decimal", "extension"]);
    }

    #[test]
    fn test_extracts_without_macros() {
        assert_eq!(extract_tags("arquivo.cbz").unwrap(), Vec::<String>::new());
    }

    #[test]
    fn test_error_unclosed_brace() {
        assert!(matches!(extract_tags("{chapter"), Err(PatternError::MalformedMacro)));
    }

    #[test]
    fn test_error_double_opening_brace() {
        assert!(matches!(extract_tags("{{chapter}"), Err(PatternError::MalformedMacro)));
    }

    #[test]
    fn test_empty_tag_is_unknown_macro() {
        assert!(matches!(
            validate_chapter_template("{}.{extension}"),
            Err(PatternError::UnknownMacro(tag)) if tag.is_empty()
        ));
    }

    // NOTE: validate_chapter_template

    #[test]
    fn test_full_valid_template() {
        assert!(validate_chapter_template("Ch. {chapter}{decimal}.*.{extension}").is_ok());
    }

    #[test]
    fn test_valid_template_without_decimal() {
        assert!(validate_chapter_template("Ch. {chapter}.*.{extension}").is_ok());
    }

    #[test]
    fn test_error_missing_chapter() {
        assert!(matches!(
            validate_chapter_template("{decimal}.*.{extension}"),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn test_error_duplicate_chapter() {
        assert!(matches!(
            validate_chapter_template("{chapter}{chapter}.*.{extension}"),
            Err(PatternError::ChapterRequired)
        ));
    }

    #[test]
    fn test_error_duplicate_decimal() {
        assert!(matches!(
            validate_chapter_template("{chapter}{decimal}{decimal}.*.{extension}"),
            Err(PatternError::DecimalDuplicate)
        ));
    }

    #[test]
    fn test_error_missing_extension() {
        assert!(matches!(
            validate_chapter_template("{chapter}{decimal}.*"),
            Err(PatternError::ExtensionRequired)
        ));
    }

    #[test]
    fn test_error_decimal_before_chapter() {
        assert!(matches!(
            validate_chapter_template("{decimal}{chapter}.*.{extension}"),
            Err(PatternError::DecimalBeforeChapter)
        ));
    }

    #[test]
    fn test_error_extension_before_chapter() {
        // Só uma extension: ordem é verificada depois da contagem
        assert!(matches!(
            validate_chapter_template("{extension}{chapter}.*"),
            Err(PatternError::ExtensionBeforeChapter)
        ));
    }

    #[test]
    fn test_error_extension_before_decimal() {
        assert!(matches!(
            validate_chapter_template("{chapter}{extension}{decimal}"),
            Err(PatternError::ExtensionBeforeDecimal)
        ));
    }

    #[test]
    fn test_error_extension_not_at_end() {
        assert!(matches!(
            validate_chapter_template("{chapter}.*.{extension}.extra"),
            Err(PatternError::ExtensionNotAtEnd)
        ));
    }

    #[test]
    fn test_error_unknown_macro_chapter() {
        assert!(matches!(
            validate_chapter_template("{titulo}.*.{extension}"),
            Err(PatternError::UnknownMacro(tag)) if tag == "titulo"
        ));
    }

    #[test]
    fn test_error_malformed_macro_chapter() {
        assert!(matches!(validate_chapter_template("{chapter"), Err(PatternError::MalformedMacro)));
    }

    // NOTE: validate_volume_template

    #[test]
    fn test_valid_volume_template() {
        assert!(validate_volume_template("Vol. {volume}{decimal}").is_ok());
    }

    #[test]
    fn test_valid_volume_template_without_decimal() {
        assert!(validate_volume_template("Vol. {volume}").is_ok());
    }

    #[test]
    fn test_error_missing_volume() {
        assert!(matches!(
            validate_volume_template("Vol. {decimal}"),
            Err(PatternError::VolumeRequired)
        ));
    }

    #[test]
    fn test_error_duplicate_volume() {
        assert!(matches!(
            validate_volume_template("{volume}{volume}"),
            Err(PatternError::VolumeRequired)
        ));
    }

    #[test]
    fn test_error_duplicate_decimal_volume() {
        assert!(matches!(
            validate_volume_template("{volume}{decimal}{decimal}"),
            Err(PatternError::DecimalDuplicate)
        ));
    }

    #[test]
    fn test_error_unknown_macro_volume() {
        assert!(matches!(
            validate_volume_template("{titulo}"),
            Err(PatternError::UnknownMacro(tag)) if tag == "titulo"
        ));
    }
}
