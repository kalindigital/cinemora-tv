package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

enum class SortOrder { PADRAO, ALFABETICA, LANCAMENTO, NOTA }

/** Ordenação das fileiras, escolhida em Definições. */
object CatalogSorter {
    fun movies(items: List<Video>, order: SortOrder): List<Video> =
        sort(items, order, { it.title }, { it.year }, { it.rating })

    fun series(items: List<Series>, order: SortOrder): List<Series> =
        sort(items, order, { it.title }, { it.year }, { it.rating })

    private fun <T> sort(
        items: List<T>,
        order: SortOrder,
        title: (T) -> String,
        year: (T) -> String?,
        rating: (T) -> String?,
    ): List<T> = when (order) {
        SortOrder.PADRAO -> items
        SortOrder.ALFABETICA -> items.sortedBy { normalize(title(it)) }
        // Sem ano/nota vai para o fim: -1 fica abaixo de qualquer valor real na ordem decrescente.
        SortOrder.LANCAMENTO -> items.sortedByDescending { year(it)?.take(4)?.toIntOrNull() ?: -1 }
        SortOrder.NOTA -> items.sortedByDescending { rating(it)?.toDoubleOrNull() ?: -1.0 }
    }

    private fun normalize(raw: String): String = java.text.Normalizer
        .normalize(raw.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
}
