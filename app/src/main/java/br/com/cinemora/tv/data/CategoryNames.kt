package br.com.cinemora.tv.data

/**
 * O provedor nomeia as categorias como "Filmes | Aventura". Dentro da aba do tipo
 * o prefixo é redundante; em Categorias (que mistura tudo) ele é mantido.
 */
object CategoryNames {
    fun short(name: String): String =
        name.substringAfter(" | ", missingDelimiterValue = name).trim()
}
