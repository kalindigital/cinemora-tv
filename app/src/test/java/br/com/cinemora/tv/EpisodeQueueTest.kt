package br.com.cinemora.tv

import br.com.cinemora.tv.data.EpisodeQueue
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeQueueTest {
    private fun ep(id: String, season: Int, number: Int) =
        Episode(id, "Ep $number", season, number, "http://x/$id.mp4")

    private val seasons = listOf(
        Season(1, listOf(ep("101", 1, 1), ep("102", 1, 2))),
        Season(2, listOf(ep("201", 2, 1))),
    )

    @Test fun `fila continua do episodio seguinte na mesma temporada`() {
        assertEquals(listOf("102", "201"), EpisodeQueue.upcoming(seasons, "101").map { it.id })
    }

    @Test fun `fila atravessa para a proxima temporada`() {
        assertEquals(listOf("201"), EpisodeQueue.upcoming(seasons, "102").map { it.id })
    }

    @Test fun `ultimo episodio da serie nao tem proximos`() {
        assertTrue(EpisodeQueue.upcoming(seasons, "201").isEmpty())
    }

    @Test fun `episodio desconhecido devolve fila vazia`() {
        assertTrue(EpisodeQueue.upcoming(seasons, "999").isEmpty())
    }

    @Test fun `continuar retoma o episodio em andamento`() {
        val alvo = EpisodeQueue.resumeTarget(
            seasons,
            watched = { false },
            position = { url -> if (url.endsWith("102.mp4")) 120_000L else 0L },
        )
        assertEquals("102", alvo?.id)
    }

    @Test fun `sem episodio em andamento continua no primeiro nao assistido`() {
        val alvo = EpisodeQueue.resumeTarget(
            seasons,
            watched = { url -> url.endsWith("101.mp4") },
            position = { 0L },
        )
        assertEquals("102", alvo?.id)
    }

    @Test fun `serie toda assistida volta para o primeiro episodio`() {
        val alvo = EpisodeQueue.resumeTarget(seasons, watched = { true }, position = { 0L })
        assertEquals("101", alvo?.id)
    }
}
