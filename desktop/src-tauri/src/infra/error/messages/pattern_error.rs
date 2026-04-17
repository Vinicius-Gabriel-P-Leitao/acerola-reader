use thiserror::Error;

// FIXME: Deve ser em ingles
#[derive(Debug, Error)]
pub enum PatternError {
    #[error("Macro mal formada: falta o fechamento '}}'.")]
    MalformedMacro,

    #[error("Macro desconhecida: '{0}'.")]
    UnknownMacro(String),

    #[error("O template deve conter exatamente um {{chapter}}.")]
    ChapterRequired,

    #[error("O template deve conter no máximo um {{decimal}}.")]
    DecimalDuplicate,

    #[error("O template deve conter exatamente um {{extension}}.")]
    ExtensionRequired,

    #[error("{{decimal}} deve vir depois de {{chapter}}.")]
    DecimalBeforeChapter,

    #[error("{{extension}} deve vir depois de {{chapter}}.")]
    ExtensionBeforeChapter,

    #[error("{{extension}} deve vir depois de {{decimal}}.")]
    ExtensionBeforeDecimal,

    #[error("O template deve terminar com {{extension}}.")]
    ExtensionNotAtEnd,

    #[error("Padrão de regex inválido gerado pelo template: {0}")]
    InvalidRegex(String),
}
