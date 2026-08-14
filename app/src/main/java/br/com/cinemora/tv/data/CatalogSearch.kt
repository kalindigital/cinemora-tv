package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog

/** Títulos do catálogo relacionados à pergunta, enviados à IA para ela não recomendar fora. */
object CatalogSearch {
    // Palavras que aparecem em quase toda pergunta e casariam com meio catálogo.
    private val VAZIAS = setOf(
        "filme", "filmes", "serie", "series", "quero", "queria", "assistir", "mostra", "mostrar",
        "tem", "temos", "hoje", "agora", "meu", "minha", "catalogo", "sobre", "algum", "alguma",
        "para", "pra", "com", "sem", "que", "qual", "quais", "esse", "essa", "isso", "bom", "boa",
        "novo", "nova", "melhor", "melhores", "recomenda", "recomende", "indica", "indique", "ver",
    )

    fun candidates(question: String, catalog: Catalog, limit: Int = 12): List<String> {
        val palavras = normalize(question)
            .split(" ")
            .filter { it.length >= 4 && it !in VAZIAS }
        if (palavras.isEmpty()) return emptyList()

        val filmes = catalog.movies.asSequence()
            .filter { video -> palavras.any { normalize(video.title).contains(it) } }
            .map { it.title }
        val series = catalog.series.asSequence()
            .filter { serie -> palavras.any { normalize(serie.title).contains(it) } }
            .map { it.title }
        return (filmes + series).distinct().take(limit).toList()
    }

    private val ACENTOS = Regex("\\p{Mn}+")
    private val NAO_ALFANUMERICO = Regex("[^a-z0-9]+")

    private fun normalize(raw: String): String = java.text.Normalizer
        .normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(ACENTOS, "")
        .replace(NAO_ALFANUMERICO, " ")
        .trim()
}
