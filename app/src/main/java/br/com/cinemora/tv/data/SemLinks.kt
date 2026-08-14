package br.com.cinemora.tv.data

/**
 * A busca na web faz o modelo citar fontes com endereço, mesmo instruído a não fazer.
 * Numa TV o link não serve para nada, então tiramos do texto antes de exibir e falar.
 */
object SemLinks {
    // [texto](endereço) -> texto
    private val MARKDOWN = Regex("\\[([^\\]]+)]\\((?:https?://|www\\.)[^)]*\\)")
    // (fonte: endereço) e parecidos
    private val PARENTESES = Regex("\\s*\\([^)]*(?:https?://|www\\.)[^)]*\\)")
    private val ENDERECO = Regex("(?:https?://|www\\.)\\S+")
    private val ESPACOS = Regex(" {2,}")

    fun limpar(texto: String): String = texto
        .replace(MARKDOWN, "$1")
        .replace(PARENTESES, "")
        .replace(ENDERECO, "")
        .replace(ESPACOS, " ")
        .replace(" .", ".")
        .replace(" ,", ",")
        .trim()
}
