package br.com.cinemora.tv

import br.com.cinemora.tv.data.CatalogSorter
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSorterTest {
    private fun movie(id: String, title: String, year: String?, rating: String?) =
        Video(id, title, "c", null, "http://x/$id.mp4", year, rating)

    private val movies = listOf(
        movie("1", "Zumbi", "2001", "5.0"),
        movie("2", "Ábaco", "2020", null),
        movie("3", "Meio", null, "9.1"),
    )

    @Test fun `ordem alfabetica ignora acentos`() {
        assertEquals(listOf("2", "3", "1"), CatalogSorter.movies(movies, SortOrder.ALFABETICA).map { it.id })
    }

    @Test fun `lancamento traz os mais novos primeiro e sem ano por ultimo`() {
        assertEquals(listOf("2", "1", "3"), CatalogSorter.movies(movies, SortOrder.LANCAMENTO).map { it.id })
    }

    @Test fun `nota traz as maiores primeiro e sem nota por ultimo`() {
        assertEquals(listOf("3", "1", "2"), CatalogSorter.movies(movies, SortOrder.NOTA).map { it.id })
    }

    @Test fun `ordem padrao mantem a sequencia do provedor`() {
        assertEquals(listOf("1", "2", "3"), CatalogSorter.movies(movies, SortOrder.PADRAO).map { it.id })
    }
}
