package br.com.cinemora.tv

import br.com.cinemora.tv.data.Recommendations
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsTest {
    private fun movie(id: String, categoria: String, nota: String?) =
        Video(id, "Filme $id", categoria, null, "http://x/$id.mp4", "2020", nota)

    private val catalog = Catalog(
        movies = listOf(
            movie("1", "acao", "5.0"),
            movie("2", "acao", "9.0"),
            movie("3", "acao", "7.0"),
            movie("4", "comedia", "8.0"),
        ),
    )

    @Test fun `recomenda da mesma categoria com as melhores notas primeiro`() {
        val result = Recommendations.related(catalog, catalog.movies[0], watched = emptySet())
        assertEquals(listOf("2", "3"), result.map { it.id })
    }

    @Test fun `nao recomenda o proprio filme nem os ja assistidos`() {
        val result = Recommendations.related(catalog, catalog.movies[0], watched = setOf("2"))
        assertEquals(listOf("3"), result.map { it.id })
    }

    @Test fun `series relacionadas seguem a mesma regra dos filmes`() {
        val comSeries = catalog.copy(
            series = listOf(
                br.com.cinemora.tv.model.Series("s1", "Serie A", "drama", null, rating = "6.0"),
                br.com.cinemora.tv.model.Series("s2", "Serie B", "drama", null, rating = "9.0"),
                br.com.cinemora.tv.model.Series("s3", "Serie C", "comedia", null, rating = "8.0"),
            ),
        )
        val result = Recommendations.relatedSeries(comSeries, comSeries.series[0], watched = emptySet())
        assertEquals(listOf("s2"), result.map { it.id })
    }

    @Test fun `categoria sem outros titulos completa com destaques do catalogo`() {
        val result = Recommendations.related(catalog, catalog.movies[3], watched = emptySet())
        assertTrue(result.isNotEmpty())
        assertTrue(result.none { it.id == "4" })
    }
}
