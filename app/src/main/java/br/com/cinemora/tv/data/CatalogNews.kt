package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Video

/** O que entrou no catálogo desde a última atualização. */
object CatalogNews {
    fun novidades(catalog: Catalog, conhecidos: Set<String>, limit: Int = 30): List<Video> {
        // Sem histórico anterior, tudo seria "novo" — melhor não mostrar nada.
        if (conhecidos.isEmpty()) return emptyList()
        return catalog.movies
            .filterNot { it.id in conhecidos }
            .sortedByDescending { it.id.toLongOrNull() ?: 0L }
            .take(limit)
    }

    fun idsConhecidos(catalog: Catalog): Set<String> = catalog.movies.mapTo(mutableSetOf()) { it.id }
}
