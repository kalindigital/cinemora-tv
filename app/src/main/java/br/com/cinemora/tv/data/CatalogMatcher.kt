package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

/** Títulos do catálogo que correspondem às sugestões da IA. */
data class AiMatches(val movies: List<Video>, val series: List<Series>)

object CatalogMatcher {
    private const val MIN_LENGTH = 3

    // Compilados uma vez: criá-los dentro de normalize() custava centenas de milhares
    // de compilações por busca e travava a TV por segundos.
    private val ACCENTS = Regex("\\p{Mn}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

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
            normalizedMovies.firstOrNull { (title, _) -> matches(title, term) }
                ?.let { (_, video) -> movies.putIfAbsent(video.id, video) }
            normalizedSeries.firstOrNull { (title, _) -> matches(title, term) }
                ?.let { (_, item) -> series.putIfAbsent(item.id, item) }
        }
        return AiMatches(movies.values.toList(), series.values.toList())
    }

    private fun matches(catalogTitle: String, term: String) = catalogTitle.contains(term) || term.contains(catalogTitle)

    /** Minúsculas, sem acentos e sem pontuação/ano ("A Origem (2010) [4K]" → "a origem 2010 4k"). */
    private fun normalize(raw: String): String = java.text.Normalizer
        .normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(ACCENTS, "")
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .removeSuffix(" 4k")
        .trim()
}
