package br.com.cinemora.tv

import br.com.cinemora.tv.data.CatalogSearch
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchTest {
    private fun movie(id: String, title: String) = Video(id, title, "c", null, "http://x/$id.mp4")

    private val catalog = Catalog(
        movies = listOf(
            movie("1", "A Entidade (2023)"),
            movie("2", "A Entidade 2 (2024)"),
            movie("3", "Interestelar (2014)"),
        ),
        series = listOf(Series("9", "Round 6", "s", null)),
    )

    @Test fun `acha os titulos do catalogo citados na pergunta`() {
        val achados = CatalogSearch.candidates("me mostra o filme a entidade", catalog)
        assertEquals(listOf("A Entidade (2023)", "A Entidade 2 (2024)"), achados)
    }

    @Test fun `ignora palavras curtas e comuns da pergunta`() {
        val achados = CatalogSearch.candidates("qual o de que tem a com", catalog)
        assertTrue(achados.isEmpty())
    }

    @Test fun `inclui series quando o titulo bate`() {
        assertEquals(listOf("Round 6"), CatalogSearch.candidates("quero ver round 6 hoje", catalog))
    }

    @Test fun `acha mesmo quando a fala junta as palavras do titulo`() {
        val comHifen = Catalog(movies = listOf(movie("5", "Homem-Aranha: De Volta ao Lar (2017)")))
        assertEquals(
            listOf("Homem-Aranha: De Volta ao Lar (2017)"),
            CatalogSearch.candidates("me mostra homemaranha", comHifen),
        )
    }

    @Test fun `acha quando o titulo do catalogo e que vem junto`() {
        val junto = Catalog(movies = listOf(movie("6", "SpiderMan (2002)")))
        assertEquals(listOf("SpiderMan (2002)"), CatalogSearch.candidates("quero spider man", junto))
    }

    @Test fun `limita a quantidade enviada ao modelo`() {
        val muitos = Catalog(movies = (1..40).map { movie("$it", "Vingadores $it") })
        assertTrue(CatalogSearch.candidates("quero vingadores", muitos, limit = 8).size <= 8)
    }
}
