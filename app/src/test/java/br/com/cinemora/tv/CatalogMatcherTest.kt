package br.com.cinemora.tv

import br.com.cinemora.tv.data.CatalogMatcher
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMatcherTest {
    private fun movie(id: String, title: String) = Video(id, title, "c", null, "http://x/$id.mp4")

    private val catalog = Catalog(
        movies = listOf(
            movie("1", "A Origem (2010) [4K]"),
            movie("2", "Interestelar (2014)"),
            movie("3", "O Poderoso Chefão"),
        ),
        series = listOf(Series("9", "Round 6", "s", null)),
    )

    @Test fun `casa titulo ignorando acentos maiusculas e sufixos`() {
        val result = CatalogMatcher.match(listOf("a origem", "INTERESTELAR", "o poderoso chefao"), catalog)
        assertEquals(listOf("1", "2", "3"), result.movies.map { it.id })
    }

    @Test fun `casa series e ignora sugestoes sem correspondencia`() {
        val result = CatalogMatcher.match(listOf("Round 6", "Filme Que Não Existe"), catalog)
        assertEquals(listOf("9"), result.series.map { it.id })
        assertTrue(result.movies.isEmpty())
    }

    @Test fun `nao repete o mesmo titulo sugerido duas vezes`() {
        val result = CatalogMatcher.match(listOf("A Origem", "a origem (2010)"), catalog)
        assertEquals(1, result.movies.size)
    }

    @Test fun `ignora sugestoes curtas demais para evitar falsos positivos`() {
        val result = CatalogMatcher.match(listOf("a", "o"), catalog)
        assertTrue(result.movies.isEmpty())
    }
}
