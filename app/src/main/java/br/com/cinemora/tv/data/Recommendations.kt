package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

/** Títulos parecidos: mesma categoria, melhores notas primeiro. */
object Recommendations {
    fun related(catalog: Catalog, movie: Video, watched: Set<String>, limit: Int = 12): List<Video> =
        pick(catalog.movies, movie.id, movie.categoryId, watched, limit, { it.id }, { it.categoryId }, { it.rating })

    fun relatedSeries(catalog: Catalog, series: Series, watched: Set<String>, limit: Int = 12): List<Series> =
        pick(catalog.series, series.id, series.categoryId, watched, limit, { it.id }, { it.categoryId }, { it.rating })

    private fun <T> pick(
        items: List<T>,
        currentId: String,
        categoryId: String,
        watched: Set<String>,
        limit: Int,
        id: (T) -> String,
        category: (T) -> String,
        rating: (T) -> String?,
    ): List<T> {
        fun elegivel(outro: T) = id(outro) != currentId && id(outro) !in watched
        val nota = { outro: T -> rating(outro)?.toDoubleOrNull() ?: -1.0 }

        val mesmaCategoria = items.filter { category(it) == categoryId && elegivel(it) }.sortedByDescending(nota)
        if (mesmaCategoria.isNotEmpty()) return mesmaCategoria.take(limit)

        // Só quando a categoria não oferece nada: melhores do catálogo.
        return items.filter(::elegivel).sortedByDescending(nota).take(limit)
    }
}
