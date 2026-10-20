package br.com.cinemora.tv.launcher

/**
 * Link que abre um filme ou uma série no detalhe do Cinemora, vindo de fora (o launcher):
 * cinemora://br.com.cinemora.tv/movie/{id} ou /series/{id}.
 */
data class OpenLink(val kind: String, val id: String) {
    companion object {
        const val MOVIE = "movie"
        const val SERIES = "series"

        /** Lê os segmentos do caminho do link; qualquer outra coisa (como /resume/) não é um OpenLink. */
        fun parse(segments: List<String>): OpenLink? {
            if (segments.size != 2) return null
            val (kind, id) = segments
            if (kind != MOVIE && kind != SERIES || id.isBlank()) return null
            return OpenLink(kind, id)
        }
    }
}
