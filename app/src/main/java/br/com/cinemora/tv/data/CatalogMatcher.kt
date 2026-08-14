package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

/** Títulos do catálogo que correspondem às sugestões da IA. */
data class AiMatches(val movies: List<Video>, val series: List<Series>)

object CatalogMatcher {
    private const val MIN_LENGTH = 4
    private const val NOME_PROPRIO = 8
    // O nome do catálogo precisa cobrir a maior parte da sugestão: senão "Caçadores"
    // roubaria "Os Caçadores da Arca Perdida".
    private const val COBERTURA = 60

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
        catalogTitle == term -> 4
        catalogTitle.startsWith("$term ") -> 3
        // O catálogo às vezes usa o nome curto ("Náufrago") e a sugestão vem completa
        // ("O Náufrago"): aceitamos, desde que o título do catálogo seja longo o bastante
        // para não ser genérico — foi assim que "Guerra" entrava em pedidos da Marvel.
        catalogTitle.length >= NOME_PROPRIO &&
            catalogTitle.length * 100 >= term.length * COBERTURA &&
            contemPalavras(term, catalogTitle) -> 2
        term.length >= 10 && catalogTitle.contains(term) -> 1
        else -> 0
    }

    /** Só aceita se o título aparecer inteiro, entre limites de palavra. */
    private fun contemPalavras(texto: String, alvo: String): Boolean {
        val posicao = texto.indexOf(alvo)
        if (posicao < 0) return false
        val antes = posicao == 0 || texto[posicao - 1] == ' '
        val fim = posicao + alvo.length
        val depois = fim == texto.length || texto[fim] == ' '
        return antes && depois
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
