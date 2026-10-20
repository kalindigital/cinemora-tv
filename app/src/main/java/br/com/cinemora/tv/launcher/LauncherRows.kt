package br.com.cinemora.tv.launcher

import br.com.cinemora.tv.data.CatalogSorter
import br.com.cinemora.tv.data.CategoryNames
import br.com.cinemora.tv.data.ConteudoAdulto
import br.com.cinemora.tv.data.FamilyFilter
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.model.Catalog

/** Um título como o launcher vê: só o que serve para mostrar o cartão, nunca o stream. */
data class LauncherItem(val id: String, val title: String, val year: String?, val rating: String?)

data class LauncherRow(val title: String, val items: List<LauncherItem>)

/**
 * As fileiras de Filmes e Séries do jeito que o Cinemora mostra: as categorias na ordem do
 * provedor, com o nome curto, os itens na ordenação escolhida em Definições e os mesmos filtros
 * (conteúdo adulto escondido, modo família).
 */
object LauncherRows {
    const val MAX_ITEMS = 30

    fun movies(catalog: Catalog, hideAdult: Boolean, family: Boolean, order: SortOrder): List<LauncherRow> {
        val c = filtrado(catalog, hideAdult, family)
        val porCategoria = c.movies.groupBy { it.categoryId }
        return c.movieCategories.mapNotNull { categoria ->
            val itens = porCategoria[categoria.id].orEmpty()
            if (itens.isEmpty()) return@mapNotNull null
            LauncherRow(
                CategoryNames.short(categoria.name),
                CatalogSorter.movies(itens, order).take(MAX_ITEMS).map { LauncherItem(it.id, it.title, it.year, it.rating) },
            )
        }
    }

    fun series(catalog: Catalog, hideAdult: Boolean, family: Boolean, order: SortOrder): List<LauncherRow> {
        val c = filtrado(catalog, hideAdult, family)
        val porCategoria = c.series.groupBy { it.categoryId }
        return c.seriesCategories.mapNotNull { categoria ->
            val itens = porCategoria[categoria.id].orEmpty()
            if (itens.isEmpty()) return@mapNotNull null
            LauncherRow(
                CategoryNames.short(categoria.name),
                CatalogSorter.series(itens, order).take(MAX_ITEMS).map { LauncherItem(it.id, it.title, it.year, it.rating) },
            )
        }
    }

    /**
     * Busca pelo nome, como na Pesquisa do app: cada palavra digitada tem de aparecer no título (sem acento, sem
     * caixa; "homem aranha" também acha "Homem-Aranha"). Os que começam pela busca vêm primeiro.
     */
    fun searchMovies(catalog: Catalog, query: String, hideAdult: Boolean, family: Boolean): List<LauncherItem> {
        val c = filtrado(catalog, hideAdult, family)
        return buscar(query, c.movies, { it.title }) { LauncherItem(it.id, it.title, it.year, it.rating) }
    }

    fun searchSeries(catalog: Catalog, query: String, hideAdult: Boolean, family: Boolean): List<LauncherItem> {
        val c = filtrado(catalog, hideAdult, family)
        return buscar(query, c.series, { it.title }) { LauncherItem(it.id, it.title, it.year, it.rating) }
    }

    private fun <T> buscar(query: String, itens: List<T>, titulo: (T) -> String, item: (T) -> LauncherItem): List<LauncherItem> {
        val q = normalizar(query)
        val palavras = q.split(" ").filter { it.isNotEmpty() }
        if (palavras.isEmpty()) return emptyList()
        val achados = itens.filter { t ->
            val n = normalizar(titulo(t))
            val compacto = n.replace(" ", "")
            palavras.all { n.contains(it) || compacto.contains(it) }
        }
        return achados.sortedBy { if (normalizar(titulo(it)).startsWith(q)) 0 else 1 }.take(MAX_ITEMS).map(item)
    }

    private val ACENTOS = Regex("\\p{Mn}+")
    private val SEPARADORES = Regex("[^a-z0-9]+")

    private fun normalizar(raw: String) = java.text.Normalizer.normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(ACENTOS, "").replace(SEPARADORES, " ").trim()

    /** A capa de um título (para o provedor buscar e entregar a imagem). */
    fun coverOf(catalog: Catalog, kind: String, id: String): String? = when (kind) {
        OpenLink.MOVIE -> catalog.movies.firstOrNull { it.id == id }?.coverUrl
        OpenLink.SERIES -> catalog.series.firstOrNull { it.id == id }?.coverUrl
        else -> null
    }

    private fun filtrado(catalog: Catalog, hideAdult: Boolean, family: Boolean): Catalog {
        val semAdulto = if (hideAdult) ConteudoAdulto.filtrar(catalog) else catalog
        return if (family) FamilyFilter.apply(semAdulto) else semAdulto
    }
}
