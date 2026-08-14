package br.com.cinemora.tv

import br.com.cinemora.tv.data.FamilyFilter
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyFilterTest {
    private fun movie(id: String, categoria: String) = Video(id, "Filme $id", categoria, null, "u$id")

    private val catalog = Catalog(
        movieCategories = listOf(
            Category("k", "Filmes | Infantil"),
            Category("a", "Filmes | Ação"),
            Category("x", "[XXX] Adulto"),
        ),
        movies = listOf(movie("1", "k"), movie("2", "a"), movie("3", "x")),
    )

    @Test fun `modo familia mantem apenas categorias para criancas`() {
        val filtrado = FamilyFilter.apply(catalog)
        assertEquals(listOf("1"), filtrado.movies.map { it.id })
        assertEquals(listOf("Filmes | Infantil"), filtrado.movieCategories.map { it.name })
    }

    @Test fun `sem modo familia o catalogo fica intacto`() {
        assertEquals(3, catalog.movies.size)
    }

    @Test fun `categoria adulta nunca passa`() {
        assertTrue(FamilyFilter.apply(catalog).movieCategories.none { it.name.contains("XXX") })
    }
}
