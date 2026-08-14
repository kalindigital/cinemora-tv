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

    @Test fun `titulo curto do catalogo nao casa dentro de uma sugestao longa`() {
        // Era assim que 007 e outros entravam em pedidos da Marvel.
        val comCurtos = catalog.copy(movies = catalog.movies + movie("9", "Guerra"))
        val result = CatalogMatcher.match(listOf("Capitão América: Guerra Civil"), comCurtos)
        assertTrue(result.movies.isEmpty())
    }

    @Test fun `casa quando o catalogo usa o nome mais curto que a sugestao`() {
        val curtos = catalog.copy(movies = listOf(movie("20", "Náufrago (2000)")))
        val result = CatalogMatcher.match(listOf("O Náufrago"), curtos)
        assertEquals(listOf("20"), result.movies.map { it.id })
    }

    @Test fun `nao troca o filme por um homonimo curto`() {
        // "Os Caçadores da Arca Perdida" não pode virar "Caçadores (2022)".
        val homonimo = catalog.copy(movies = listOf(movie("30", "Caçadores (2022)")))
        val result = CatalogMatcher.match(listOf("Os Caçadores da Arca Perdida"), homonimo)
        assertTrue(result.movies.isEmpty())
    }

    @Test fun `prefere o titulo exato ao parecido`() {
        val comParecidos = catalog.copy(
            movies = listOf(movie("10", "A Origem do Mal (2022)"), movie("11", "A Origem (2010) [4K]")),
        )
        val result = CatalogMatcher.match(listOf("A Origem"), comParecidos)
        assertEquals(listOf("11"), result.movies.map { it.id })
    }

    @Test fun `casa pelo comeco do titulo quando o catalogo traz subtitulo`() {
        val comSubtitulo = catalog.copy(movies = listOf(movie("12", "Harry Potter e o Enigma do Príncipe (2009)")))
        val result = CatalogMatcher.match(listOf("Harry Potter e o Enigma do Príncipe"), comSubtitulo)
        assertEquals(listOf("12"), result.movies.map { it.id })
    }

    @Test fun `ignora sugestoes curtas demais para evitar falsos positivos`() {
        val result = CatalogMatcher.match(listOf("a", "o"), catalog)
        assertTrue(result.movies.isEmpty())
    }
}
