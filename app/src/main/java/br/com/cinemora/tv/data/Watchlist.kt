package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Video

/** Títulos que você está esperando entrar no catálogo. */
object Watchlist {
    fun adicionar(atual: List<String>, titulo: String, limit: Int = 20): List<String> {
        val limpo = titulo.trim()
        if (limpo.isBlank()) return atual
        val jaTem = atual.any { it.equals(limpo, ignoreCase = true) }
        return if (jaTem) atual else (listOf(limpo) + atual).take(limit)
    }

    fun remover(atual: List<String>, titulo: String): List<String> =
        atual.filterNot { it.equals(titulo, ignoreCase = true) }

    /** Quais dos esperados já estão disponíveis agora. */
    fun chegaram(esperados: List<String>, catalog: Catalog): List<Pair<String, Video>> =
        esperados.mapNotNull { esperado ->
            CatalogMatcher.match(listOf(esperado), catalog).movies.firstOrNull()?.let { esperado to it }
        }
}
