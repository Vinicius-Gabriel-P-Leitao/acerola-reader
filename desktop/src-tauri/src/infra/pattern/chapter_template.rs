use regex::Regex;
use crate::infra::pattern::archive_format::ArchiveFormat;

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

    pub fn from_tag(tag: &str) -> Option<Self> {
        match tag {
            "chapter" => Some(Self::Chapter),
            "decimal" => Some(Self::Decimal),
            "extension" => Some(Self::Extension),
            _ => None,
        }
    }
}

pub const PRESETS: &[(&str, &str)] = &[
    ("01.*.", "{chapter}{decimal}.*.{extension}"),
    ("Ch. 01.*.", "Ch. {chapter}{decimal}.*.{extension}"),
    ("Cap. 01.*.", "Cap. {chapter}{decimal}.*.{extension}"),
    ("chapter 01.*.", "chapter {chapter}{decimal}.*.{extension}"),
];

pub const DEFAULT_TEMPLATE: &str = PRESETS[0].1;

// prettier-ignore
fn extensions_pattern() -> String {
    ArchiveFormat::all().iter().map(|it: &ArchiveFormat| it.extension()).collect::<Vec<_>>().join("|")
}

pub fn template_to_regex(template: &str) -> Result<Regex, regex::Error> {
    let extension = extensions_pattern();

    let pattern = template
        .replace('.', "\\.")
        .replace('(', "\\(")
        .replace(')', "\\)")
        .replace('[', "\\[")
        .replace(']', "\\]")
        .replace("{chapter}", "(\\d+)")
        .replace("{decimal}", "(?:[.,](\\d+))?")
        .replace("{extension}", &format!("\\.?({extension})"))
        .replace('*', ".*?")
        .replace(' ', "\\s*");

     Regex::new(&format!("(?i)^{pattern}$"))
}
