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
        // A chave é calculada uma vez por item: em sortedBy o seletor roda a cada
        // comparação, e normalizar 16 mil títulos assim travava a TV por segundos.
        SortOrder.ALFABETICA -> items.ordenarPor(crescente = true) { normalizadoParaOrdem(title(it)) }
        // Sem ano/nota vai para o fim: -1 fica abaixo de qualquer valor real na ordem decrescente.
        SortOrder.LANCAMENTO -> items.ordenarPor(crescente = false) { (year(it)?.take(4)?.toIntOrNull() ?: -1).toDouble() }
        SortOrder.NOTA -> items.ordenarPor(crescente = false) { rating(it)?.toDoubleOrNull() ?: -1.0 }
    }

    private inline fun <T, K : Comparable<K>> List<T>.ordenarPor(crescente: Boolean, chave: (T) -> K): List<T> {
        val decorado = map { chave(it) to it }
        val ordenado = if (crescente) decorado.sortedBy { it.first } else decorado.sortedByDescending { it.first }
        return ordenado.map { it.second }
    }

    private val ACENTOS = Regex("\\p{Mn}+")

    private fun normalizadoParaOrdem(raw: String): String = java.text.Normalizer
        .normalize(raw.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(ACENTOS, "")
}
