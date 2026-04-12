use thiserror::Error;

/// Erros de validação de um template de nomenclatura de capítulos.                                                                                                                                                                                                                         ///
/// Retornado por `validate_template` quando o template fornecido
/// viola alguma regra estrutural. Nunca ocorre durante o scan
/// automático — apenas quando o usuário cria ou edita um template.
#[derive(Debug, Error)]
pub enum PatternError {
    /// O template contém um `{` sem `}` correspondente.
    #[error("Macro mal formada: falta o fechamento '}}'.")]
    MalformedMacro,

    /// O template contém uma macro `{tag}` não reconhecida.
    ///
    /// As macros válidas são: `{chapter}`, `{decimal}` e `{extension}`.
    #[error("Macro desconhecida: '{0}'.")]
    UnknownMacro(String),

    /// `{chapter}` é obrigatório e deve aparecer exatamente uma vez.
    #[error("O template deve conter exatamente um {{chapter}}.")]
    ChapterRequired,

    /// `{decimal}` apareceu mais de uma vez no template.
    #[error("O template deve conter no máximo um {{decimal}}.")]
    DecimalDuplicate,

    /// `{extension}` é obrigatório e deve aparecer exatamente uma vez.
    #[error("O template deve conter exatamente um {{extension}}.")]
    ExtensionRequired,

    /// `{decimal}` foi declarado antes de `{chapter}` no template.
    #[error("{{decimal}} deve vir depois de {{chapter}}.")]
    DecimalBeforeChapter,

    /// `{extension}` foi declarado antes de `{chapter}` no template.
    #[error("{{extension}} deve vir depois de {{chapter}}.")]
    ExtensionBeforeChapter,

    /// `{extension}` foi declarado antes de `{decimal}` no template.
    #[error("{{extension}} deve vir depois de {{decimal}}.")]
    ExtensionBeforeDecimal,

    /// O template não encerra com `{extension}`.
    ///
    /// A extensão deve ser sempre o último elemento do template.
    #[error("O template deve terminar com {{extension}}.")]
    ExtensionNotAtEnd,

    /// O template gerou um padrão de regex inválido.
    ///                                                                                                                                                                                                                                                                                         /// Indica que a string resultante após a substituição das macros
    /// não é um regex válido. Isso normalmente aponta para caracteres
    /// especiais no template que não foram escapados corretamente.
    ///
    /// A mensagem interna descreve o erro reportado pelo motor de regex.
    #[error("Padrão de regex inválido gerado pelo template: {0}")]
    InvalidRegex(String),
}
