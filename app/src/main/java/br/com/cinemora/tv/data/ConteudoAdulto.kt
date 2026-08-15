package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog

/**
 * Esconde o conteúdo adulto do catálogo inteiro.
 *
 * O provedor marca esses títulos de duas formas: categorias com nome explícito e o prefixo
 * [XXX] no nome do título. Não basta procurar "xxx" solto: isso derrubaria "xXx: Reativado"
 * e "MaXXXine", que são filmes comuns.
 */
object ConteudoAdulto {
    private val MARCA = Regex("""[\[(]\s*x{2,}\s*[\])]""", RegexOption.IGNORE_CASE)
    private val CATEGORIA = listOf("xxx", "adult", "erot", "porn", "hentai", "onlyfans", "+18", "18+", "sexy")

    fun ehAdulto(titulo: String): Boolean = MARCA.containsMatchIn(titulo)

    fun categoriaAdulta(nome: String): Boolean {
        val limpo = java.text.Normalizer.normalize(nome.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return CATEGORIA.any { limpo.contains(it) }
    }

    fun filtrar(catalog: Catalog): Catalog {
        val filmes = catalog.movieCategories.filterNot { categoriaAdulta(it.name) }
        val series = catalog.seriesCategories.filterNot { categoriaAdulta(it.name) }
        val canais = catalog.liveCategories.filterNot { categoriaAdulta(it.name) }
        val idsFilmes = filmes.map { it.id }.toSet()
        val idsSeries = series.map { it.id }.toSet()
        val idsCanais = canais.map { it.id }.toSet()
        return catalog.copy(
            movieCategories = filmes,
            movies = catalog.movies.filter { it.categoryId in idsFilmes && !ehAdulto(it.title) },
            seriesCategories = series,
            series = catalog.series.filter { it.categoryId in idsSeries && !ehAdulto(it.title) },
            liveCategories = canais,
            channels = catalog.channels.filter { it.categoryId in idsCanais && !ehAdulto(it.name) },
        )
    }
}
