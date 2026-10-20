package br.com.cinemora.tv

import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.launcher.LauncherRows
import br.com.cinemora.tv.launcher.OpenLink
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherRowsTest {
    private fun movie(id: String, title: String, cat: String, year: String? = null) =
        Video(id, title, cat, "http://img/$id.jpg", "http://user:senha@x/$id.mp4", year)

    private val catalog = Catalog(
        movieCategories = listOf(Category("a", "Filmes | Ação"), Category("v", "Filmes | Vazia"), Category("x", "Filmes | XXX Adultos"),
            Category("k", "Filmes | Infantil")),
        movies = listOf(movie("1", "Zorro", "a", "2001"), movie("2", "Alfa", "a", "2020"), movie("3", "[XXX] Coisa", "a"),
            movie("4", "Proibido", "x"), movie("5", "Desenho", "k")),
        seriesCategories = listOf(Category("s", "Séries | Drama")),
        series = listOf(Series("9", "Uma Série", "s", "http://img/9.jpg")),
    )

    @Test fun `fileiras seguem as categorias com nome curto e pulam as vazias`() {
        val rows = LauncherRows.movies(catalog, hideAdult = false, family = false, order = SortOrder.PADRAO)
        assertEquals(listOf("Ação", "XXX Adultos", "Infantil"), rows.map { it.title })
    }

    @Test fun `itens na ordem escolhida`() {
        val rows = LauncherRows.movies(catalog, false, false, SortOrder.LANCAMENTO)
        assertEquals(listOf("2", "1", "3"), rows[0].items.map { it.id })
    }

    @Test fun `conteudo adulto escondido some das fileiras`() {
        val rows = LauncherRows.movies(catalog, hideAdult = true, family = false, order = SortOrder.PADRAO)
        assertEquals(listOf("Ação", "Infantil"), rows.map { it.title })
        assertEquals(listOf("1", "2"), rows[0].items.map { it.id })
    }

    @Test fun `modo familia deixa so as categorias infantis`() {
        val rows = LauncherRows.movies(catalog, hideAdult = false, family = true, order = SortOrder.PADRAO)
        assertEquals(listOf("Infantil"), rows.map { it.title })
    }

    @Test fun `series e capa`() {
        assertEquals(listOf("Drama"), LauncherRows.series(catalog, false, false, SortOrder.PADRAO).map { it.title })
        assertEquals("http://img/9.jpg", LauncherRows.coverOf(catalog, "series", "9"))
        assertNull(LauncherRows.coverOf(catalog, "movie", "nao-existe"))
    }

    @Test fun `limite por fileira`() {
        val muitos = Catalog(movieCategories = listOf(Category("a", "A")), movies = (1..50).map { movie("$it", "T$it", "a") })
        assertEquals(LauncherRows.MAX_ITEMS, LauncherRows.movies(muitos, false, false, SortOrder.PADRAO)[0].items.size)
    }

    @Test fun `link de abertura`() {
        assertEquals(OpenLink("movie", "42"), OpenLink.parse(listOf("movie", "42")))
        assertEquals(OpenLink("series", "7"), OpenLink.parse(listOf("series", "7")))
        assertNull(OpenLink.parse(listOf("resume", "abc")))
        assertNull(OpenLink.parse(listOf("movie")))
        assertNull(OpenLink.parse(listOf("movie", " ")))
    }

    @Test fun `busca por palavras sem acento e com os que comecam pela busca primeiro`() {
        val c = Catalog(
            movieCategories = listOf(Category("a", "A")),
            movies = listOf(movie("1", "O Espetacular Homem-Aranha", "a"), movie("2", "Homem-Aranha: Longe de Casa", "a"),
                movie("3", "Ação Total", "a"), movie("4", "[XXX] Homem Aranha", "a")),
        )
        assertEquals(listOf("2", "1", "4"), LauncherRows.searchMovies(c, "homem aranha", false, false).map { it.id })
        assertEquals(listOf("2", "1"), LauncherRows.searchMovies(c, "Homem-Aranha", true, false).map { it.id })
        assertEquals(listOf("3"), LauncherRows.searchMovies(c, "acao", false, false).map { it.id })
        assertEquals(emptyList<String>(), LauncherRows.searchMovies(c, "  ", false, false).map { it.id })
        assertEquals(listOf("9"), LauncherRows.searchSeries(catalog, "serie", false, false).map { it.id })
    }
}
