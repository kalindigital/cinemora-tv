package br.com.cinemora.tv

import br.com.cinemora.tv.data.CatalogNews
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogNewsTest {
    private fun movie(id: String) = Video(id, "Filme $id", "c", null, "http://x/$id.mp4", rating = id)

    @Test fun `mostra o que entrou desde a ultima vez`() {
        val antes = setOf("1", "2")
        val agora = Catalog(movies = listOf(movie("1"), movie("2"), movie("3"), movie("4")))
        assertEquals(listOf("4", "3"), CatalogNews.novidades(agora, antes).map { it.id })
    }

    @Test fun `primeira carga nao inventa novidades`() {
        val agora = Catalog(movies = listOf(movie("1"), movie("2")))
        assertTrue(CatalogNews.novidades(agora, emptySet()).isEmpty())
    }

    @Test fun `sem nada novo devolve lista vazia`() {
        val agora = Catalog(movies = listOf(movie("1")))
        assertTrue(CatalogNews.novidades(agora, setOf("1")).isEmpty())
    }
}
