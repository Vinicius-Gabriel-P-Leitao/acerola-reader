use regex::Regex;

use crate::infra::{
    error::translations::pattern_error::PatternError, pattern::archive_format::ArchiveFormat,
};

#[derive(Debug, PartialEq, Eq)]
pub enum TemplateMacro {
    Chapter,
    Decimal,
    Extension,
}

impl TemplateMacro {
    pub fn tag(&self) -> &'static str {
        match self {
            Self::Chapter => "chapter",
            Self::Decimal => "decimal",
            Self::Extension => "extension",
        }
    }

    pub fn from_tag(tag: &str) -> Result<Self, PatternError> {
        match tag {
            "chapter" => Ok(Self::Chapter),
            "decimal" => Ok(Self::Decimal),
            "extension" => Ok(Self::Extension),
            _ => Err(PatternError::UnknownMacro(tag.to_string())),
        }
    }
}

#[derive(Debug, PartialEq, Eq)]
pub enum PresetTemplate {
    Numeric,
    Ch,
    Cap,
    Chapter,
}

impl PresetTemplate {
    pub fn value(&self) -> (&'static str, &'static str) {
        match self {
            Self::Numeric => ("01.*.", "{chapter}{decimal}.*.{extension}"),
            Self::Ch => ("Ch. 01.*.", "Ch. {chapter}{decimal}.*.{extension}"),
            Self::Cap => ("Cap. 01.*.", "Cap. {chapter}{decimal}.*.{extension}"),
            Self::Chapter => ("chapter 01.*.", "chapter {chapter}{decimal}.*.{extension}"),
        }
    }

    pub fn all() -> &'static [PresetTemplate] {
        &[Self::Numeric, Self::Ch, Self::Cap, Self::Chapter]
    }
}

#[rustfmt::skip] 
pub fn template_to_regex(
    template: &str,
    validate: impl Fn(&str) -> Result<(), PatternError>,
) -> Result<Regex, PatternError> {
    validate(template)
        .map(|_| {
            template
                .replace(".+", "*")
                .replace(".*", "*")
                .replace('.', "\\.")
                .replace('(', "\\(")
                .replace(')', "\\)")
                .replace('[', "\\[")
                .replace(']', "\\]")
                .replace("{chapter}", "(\\d+)")
                .replace("{decimal}", "(?:[.,](\\d+))?")
                .replace( "{extension}", &format!("\\.?({})", ArchiveFormat::extensions_pattern()),)
                .replace('*', ".*?")
                .replace(' ', "\\s*")
        })
        .and_then(|pattern| {
            Regex::new(&format!("(?i)^{pattern}$")).map_err(|err| PatternError::InvalidRegex(err.to_string()))
        })
}

pub fn detect_template<'a>(
    file_name: &str,
    templates: &[&'a str],
    validate: impl Fn(&str) -> Result<(), PatternError>,
) -> Option<&'a str> {
    templates.iter().copied().find_map(|template| {
        template_to_regex(template, &validate)
            .ok()
            .filter(|regex| regex.is_match(file_name))
            .map(|_| template)
    })
}

pub fn extract_chapter_parts(
    file_name: &str,
    template: &str,
    validate: impl Fn(&str) -> Result<(), PatternError>,
) -> Option<(u64, Option<String>)> {
    template_to_regex(template, validate)
        .ok()
        .and_then(|regex| regex.captures(file_name))
        .and_then(|caps| {
            caps.get(1)
                .and_then(|it| it.as_str().parse::<u64>().ok())
                .map(|chapter| {
                    let decimal = caps.get(2).map(|it| it.as_str().to_string());
                    (chapter, decimal)
                })
        })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::infra::error::translations::pattern_error::PatternError;

    fn setup_true_validate(_: &str) -> Result<(), PatternError> {
        Ok(())
    }

    // NOTE: TemplateMacro

    #[test]
    fn macro_tag_retorna_string_correta() {
        assert_eq!(TemplateMacro::Chapter.tag(), "chapter");
        assert_eq!(TemplateMacro::Decimal.tag(), "decimal");
        assert_eq!(TemplateMacro::Extension.tag(), "extension");
    }

    #[test]
    fn macro_from_tag_valido() {
        assert!(matches!(
            TemplateMacro::from_tag("chapter"),
            Ok(TemplateMacro::Chapter)
        ));
        assert!(matches!(
            TemplateMacro::from_tag("decimal"),
            Ok(TemplateMacro::Decimal)
        ));
        assert!(matches!(
            TemplateMacro::from_tag("extension"),
            Ok(TemplateMacro::Extension)
        ));
    }

    #[test]
    fn macro_from_tag_desconhecido() {
        assert!(matches!(
            TemplateMacro::from_tag("titulo"),
            Err(PatternError::UnknownMacro(tag)) if tag == "titulo"
        ));
    }

    // NOTE: PresetTemplate

    #[test]
    fn preset_all_retorna_quatro() {
        assert_eq!(PresetTemplate::all().len(), 4);
    }

    #[test]
    fn preset_ch_value() {
        let (label, pattern) = PresetTemplate::Ch.value();
        assert_eq!(label, "Ch. 01.*.");
        assert_eq!(pattern, "Ch. {chapter}{decimal}.*.{extension}");
    }

    #[test]
    fn preset_numeric_value() {
        let (label, pattern) = PresetTemplate::Numeric.value();
        assert_eq!(label, "01.*.");
        assert_eq!(pattern, "{chapter}{decimal}.*.{extension}");
    }

    // NOTE: template_to_regex

    #[test]
    fn regex_template_valido_compila() {
        assert!(
            template_to_regex("Ch. {chapter}{decimal}.*.{extension}", setup_true_validate).is_ok()
        );
    }

    #[test]
    fn regex_validator_rejeitado_propaga_erro() {
        let result = template_to_regex("{chapter}.*.{extension}", |_| {
            Err(PatternError::ChapterRequired)
        });
        assert!(matches!(result, Err(PatternError::ChapterRequired)));
    }

    #[test]
    fn regex_bate_arquivo_ch() {
        let re =
            template_to_regex("Ch. {chapter}{decimal}.*.{extension}", setup_true_validate).unwrap();
        assert!(re.is_match("Ch. 1.cbz"));
        assert!(re.is_match("Ch. 10.5.cbz"));
        assert!(!re.is_match("Oneshot.cbz"));
    }

    // NOTE: detect_template

    fn presets_patterns() -> Vec<&'static str> {
        PresetTemplate::all().iter().map(|p| p.value().1).collect()
    }

    #[test]
    fn detecta_preset_ch() {
        let result = detect_template("Ch. 1.cbz", &presets_patterns(), setup_true_validate);
        assert_eq!(result, Some("Ch. {chapter}{decimal}.*.{extension}"));
    }

    #[test]
    fn detecta_preset_numerico() {
        let result = detect_template("001.cbz", &presets_patterns(), setup_true_validate);
        assert_eq!(result, Some("{chapter}{decimal}.*.{extension}"));
    }

    #[test]
    fn nao_detecta_oneshot() {
        assert!(detect_template("Oneshot.cbz", &presets_patterns(), setup_true_validate).is_none());
    }

    #[test]
    fn lista_vazia_retorna_none() {
        assert!(detect_template("Ch. 1.cbz", &[], setup_true_validate).is_none());
    }

    #[test]
    fn template_invalido_na_lista_e_ignorado() {
        let templates = &["invalido", "Ch. {chapter}{decimal}.*.{extension}"];
        let result = detect_template("Ch. 1.cbz", templates, |t| {
            if *t == *"invalido" {
                Err(PatternError::ChapterRequired)
            } else {
                Ok(())
            }
        });
        assert_eq!(result, Some("Ch. {chapter}{decimal}.*.{extension}"));
    }

    // NOTE: extract_chapter_parts

    #[test]
    fn extrai_chapter_inteiro() {
        let template = "Ch. {chapter}{decimal}.*.{extension}";
        assert_eq!(
            extract_chapter_parts("Ch. 5.cbz", template, setup_true_validate),
            Some((5, None))
        );
    }

    #[test]
    fn extrai_chapter_com_decimal() {
        let template = "Ch. {chapter}{decimal}.*.{extension}";
        assert_eq!(
            extract_chapter_parts("Ch. 1.5.cbz", template, setup_true_validate),
            Some((1, Some("5".to_string())))
        );
    }

    #[test]
    fn extrai_chapter_numerico() {
        let template = "{chapter}{decimal}.*.{extension}";
        assert_eq!(
            extract_chapter_parts("001.cbz", template, setup_true_validate),
            Some((1, None))
        );
    }

    #[test]
    fn nao_extrai_oneshot() {
        let template = "Ch. {chapter}{decimal}.*.{extension}";
        assert!(extract_chapter_parts("Oneshot.cbz", template, setup_true_validate).is_none());
    }

    #[test]
    fn nao_extrai_com_validator_rejeitando() {
        let template = "Ch. {chapter}{decimal}.*.{extension}";
        assert!(extract_chapter_parts("Ch. 1.cbz", template, |_| {
            Err(PatternError::ChapterRequired)
        })
        .is_none());
    }
}
