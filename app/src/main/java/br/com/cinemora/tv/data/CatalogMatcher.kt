package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

/** Títulos do catálogo que correspondem às sugestões da IA. */
data class AiMatches(val movies: List<Video>, val series: List<Series>)

object CatalogMatcher {
    private const val MIN_LENGTH = 4

    // Compilados uma vez: criá-los dentro de normalize() custava centenas de milhares
    // de compilações por busca e travava a TV por segundos.
    private val ACCENTS = Regex("\\p{Mn}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
    private val RUIDO = Regex("\\b(19|20)\\d{2}\\b|\\b(4k|hd|fhd|dublado|legendado|cam)\\b")

    private var indexedCatalog: Catalog? = null
    private var normalizedMovies: List<Pair<String, Video>> = emptyList()
    private var normalizedSeries: List<Pair<String, Series>> = emptyList()

    fun match(suggestions: List<String>, catalog: Catalog): AiMatches {
        val terms = suggestions.map(::normalize).filter { it.length >= MIN_LENGTH }.distinct()
        if (terms.isEmpty()) return AiMatches(emptyList(), emptyList())

        // Normaliza o catálogo uma vez e reaproveita nas buscas seguintes.
        if (indexedCatalog !== catalog) {
            normalizedMovies = catalog.movies.map { normalize(it.title) to it }
            normalizedSeries = catalog.series.map { normalize(it.title) to it }
            indexedCatalog = catalog
        }

        val movies = LinkedHashMap<String, Video>()
        val series = LinkedHashMap<String, Series>()
        terms.forEach { term ->
            melhor(normalizedMovies, term)?.let { movies.putIfAbsent(it.id, it) }
            melhor(normalizedSeries, term)?.let { series.putIfAbsent(it.id, it) }
        }
        return AiMatches(movies.values.toList(), series.values.toList())
    }

    /** Escolhe a melhor correspondência: título igual vence começo, que vence trecho. */
    private fun <T> melhor(itens: List<Pair<String, T>>, term: String): T? =
        itens.asSequence()
            .map { (titulo, item) -> pontuar(titulo, term) to item }
            .filter { it.first > 0 }
            .maxByOrNull { it.first }
            ?.second

    private fun pontuar(catalogTitle: String, term: String): Int = when {
        catalogTitle == term -> 3
        catalogTitle.startsWith("$term ") -> 2
        // Só trechos longos: um título curto do catálogo ("Guerra") casava dentro de
        // qualquer sugestão parecida e trazia filmes sem relação com o pedido.
        term.length >= 10 && catalogTitle.contains(term) -> 1
        else -> 0
    }

    /** Minúsculas, sem acentos, sem ano nem marcações de qualidade. */
    private fun normalize(raw: String): String = java.text.Normalizer
        .normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(ACCENTS, "")
        .replace(NON_ALPHANUMERIC, " ")
        .replace(RUIDO, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
