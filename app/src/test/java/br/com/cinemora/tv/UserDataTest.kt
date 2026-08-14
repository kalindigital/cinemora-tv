package br.com.cinemora.tv

import br.com.cinemora.tv.data.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataTest {
    @Test fun `favoritar adiciona e favoritar de novo remove`() {
        val favoritado = UserData().toggleFavorite("10")
        assertTrue("10" in favoritado.favorites)
        val desfavoritado = favoritado.toggleFavorite("10")
        assertFalse("10" in desfavoritado.favorites)
    }

    @Test fun `assistir move o titulo para o topo sem duplicar`() {
        val data = UserData(watched = listOf("3", "2", "1"))
        val updated = data.recordWatched("2")
        assertEquals(listOf("2", "3", "1"), updated.watched)
    }

    @Test fun `remover dos assistidos tira o titulo do historico`() {
        val data = UserData(watched = listOf("3", "2", "1"))
        assertEquals(listOf("3", "1"), data.removeWatched("2").watched)
    }

    @Test fun `remover titulo ausente nao altera o historico`() {
        val data = UserData(watched = listOf("3", "1"))
        assertEquals(listOf("3", "1"), data.removeWatched("9").watched)
    }

    @Test fun `historico respeita o limite descartando os mais antigos`() {
        val data = UserData(watched = (1..20).map { it.toString() })
        val updated = data.recordWatched("99", limit = 20)
        assertEquals(20, updated.watched.size)
        assertEquals("99", updated.watched.first())
        assertFalse(updated.watched.contains("20"))
    }
}
