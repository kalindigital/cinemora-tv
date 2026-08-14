package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog

/**
 * Modo família: o provedor não envia classificação indicativa, então usamos o nome das
 * categorias — é o dado confiável que existe.
 */
object FamilyFilter {
    private val PERMITIDAS = listOf("infantil", "kids", "crianca", "animacao", "desenho", "familia")
    private val PROIBIDAS = listOf("xxx", "adulto", "erotic", "+18", "18+")

    fun apply(catalog: Catalog): Catalog {
        val filmes = catalog.movieCategories.filter { permitida(it.name) }
        val series = catalog.seriesCategories.filter { permitida(it.name) }
        val canais = catalog.liveCategories.filter { permitida(it.name) }
        val idsFilmes = filmes.map { it.id }.toSet()
        val idsSeries = series.map { it.id }.toSet()
        val idsCanais = canais.map { it.id }.toSet()
        return catalog.copy(
            movieCategories = filmes,
            movies = catalog.movies.filter { it.categoryId in idsFilmes },
            seriesCategories = series,
            series = catalog.series.filter { it.categoryId in idsSeries },
            liveCategories = canais,
            channels = catalog.channels.filter { it.categoryId in idsCanais },
        )
    }

    private fun permitida(nome: String): Boolean {
        val limpo = normalize(nome)
        if (PROIBIDAS.any { limpo.contains(it) }) return false
        return PERMITIDAS.any { limpo.contains(it) }
    }

    private fun normalize(raw: String) = java.text.Normalizer
        .normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
}
