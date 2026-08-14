package br.com.cinemora.tv

import br.com.cinemora.tv.data.Watchlist
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistTest {
    private val catalog = Catalog(movies = listOf(Video("1", "Duna: Parte 3 (2026)", "c", null, "u")))

    @Test fun `avisa quando o titulo esperado entra no catalogo`() {
        val chegou = Watchlist.chegaram(listOf("Duna Parte 3", "Avatar 4"), catalog)
        assertEquals(listOf("Duna Parte 3"), chegou.map { it.first })
        assertEquals("Duna: Parte 3 (2026)", chegou.first().second.title)
    }

    @Test fun `nao avisa o que ainda nao chegou`() {
        assertTrue(Watchlist.chegaram(listOf("Avatar 4"), catalog).isEmpty())
    }

    @Test fun `guarda sem repetir e coloca o novo no topo`() {
        assertEquals(listOf("Duna"), Watchlist.adicionar(listOf("Duna"), "  duna "))
        assertEquals(listOf("Avatar", "Duna"), Watchlist.adicionar(listOf("Duna"), "Avatar"))
    }
}
