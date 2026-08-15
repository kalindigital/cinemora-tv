package br.com.cinemora.tv.data

/**
 * As capas do provedor vêm do TMDB em w600×h900 (~100 KB): pesado demais para uma fileira
 * inteira na TV, então pedimos o tamanho adequado a cada uso.
 *
 * Também servimos por HTTP: o certificado do TMDB é Let's Encrypt e Android 6/7 não tem a
 * raiz ISRG Root X1, então nesses aparelhos o HTTPS falha e nenhuma capa aparece.
 */
object ImageUrls {
    fun card(url: String?): String? = resize(url, "w342")

    fun detail(url: String?): String? = resize(url, "w500")

    /** Arte 16:9 do destaque: ocupa a largura da tela, então vem maior que as capas. */
    fun backdrop(url: String?): String? = resize(url, "w1280")

    private val TMDB_SIZE = Regex("(https?://image\\.tmdb\\.org/t/p/)[^/]+(/)")

    private fun resize(url: String?, size: String): String? {
        if (url.isNullOrBlank()) return url
        return TMDB_SIZE.replace(url) { match ->
            "${match.groupValues[1].replaceFirst("https://", "http://")}$size${match.groupValues[2]}"
        }
    }
}
