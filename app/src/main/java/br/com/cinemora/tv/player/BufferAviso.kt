package br.com.cinemora.tv.player

/**
 * O que dizer enquanto o vídeo enche o buffer.
 *
 * Sem isso não dá para saber se o filme está carregando ou se travou de vez — foi a dúvida
 * que motivou este aviso. O silêncio no começo é de propósito: engasgos de um segundo são
 * normais ao trocar de trecho, e avisar em todos faria a tela piscar.
 */
object BufferAviso {
    private const val SILENCIO_MS = 1_200L
    private const val LENTO_MS = 10_000L
    private const val RETOMAR_MS = 20_000L

    fun mensagem(bufferandoMs: Long): String? = when {
        bufferandoMs < SILENCIO_MS -> null
        bufferandoMs < LENTO_MS -> "Carregando…"
        else -> "Conexão lenta. Ainda tentando…"
    }

    /** Parado tempo demais é sinal de conexão perdida: vale refazer o pedido ao servidor. */
    fun deveTentarDeNovo(bufferandoMs: Long): Boolean = bufferandoMs >= RETOMAR_MS
}
