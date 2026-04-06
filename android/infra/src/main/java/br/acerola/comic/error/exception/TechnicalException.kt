package br.acerola.comic.error.exception

/**
 * Exceções técnicas que indicam BUGS ou estados impossíveis.
 * Não devem ser capturadas para lógica de negócio.
 * São destinadas apenas a logging/Crashlytics.
 *
 * @param message Mensagem detalhada sobre a causa da exceção.
 * @param cause Causa raiz da exceção, se houver.
 */
sealed class TechnicalException(
    message: String, cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Indica um estado inválido que nunca deveria ocorrer em um código correto.
     * Ex: Um serviço não inicializado antes de ser usado, uma configuração interna inválida.
     *
     * @param message Mensagem detalhada sobre o estado inválido.
     * @param cause Causa raiz da exceção.
     */
    class InvalidStateException(
        message: String, cause: Throwable? = null
    ) : TechnicalException(message, cause)

    /**
     * Indica uma violação de contrato, geralmente relacionada a argumentos inválidos passados a um método.
     * Ex: Um ID negativo onde apenas IDs positivos são esperados, uma string vazia onde conteúdo é mandatório.
     *
     * @param message Mensagem detalhada sobre a violação do contrato.
     * @param cause Causa raiz da exceção.
     */
    class ContractViolationException(
        message: String, cause: Throwable? = null
    ) : TechnicalException(message, cause)

    /**
     * Sinaliza que uma operação não é suportada ou não é permitida no contexto atual.
     * Ex: Tentar realizar uma operação de escrita em um objeto que é somente leitura.
     *
     * @param message Mensagem detalhada sobre a operação não suportada.
     * @param cause Causa raiz da exceção.
     */
    class UnsupportedOperationException(
        message: String, cause: Throwable? = null
    ) : TechnicalException(message, cause)
}

/**
 * Ajuda a garantir pré-condições, lançando [ContractViolationException] se a condição for falsa.
 *
 * @param value Condição booleana que deve ser verdadeira.
 * @param lazyMessage Função que retorna a mensagem de erro se a condição for falsa.
 */
inline fun require(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        throw TechnicalException.ContractViolationException(lazyMessage())
    }
}

/**
 * Ajuda a garantir que um valor não seja nulo, lançando [InvalidStateException] se for nulo.
 *
 * @param value O valor a ser verificado.
 * @param lazyMessage Função que retorna a mensagem de erro se o valor for nulo.
 * @return O valor não nulo.
 */
inline fun <T : Any> requireNotNull(value: T?, lazyMessage: () -> String): T {
    return value ?: throw TechnicalException.InvalidStateException(lazyMessage())
}
