package br.com.cinemora.tv

import br.com.cinemora.tv.data.ResumeEntry
import br.com.cinemora.tv.data.ResumeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumeEntryTest {
    private val entrada = ResumeEntry(
        id = "abc",
        title = "Solo Leveling — T1 E6",
        streamUrl = "http://x/series/u/p/9.mp4",
        posterUrl = "http://x/capa.jpg",
        positionMs = 120_000,
        durationMs = 1_400_000,
    )

    @Test fun `guarda e recupera a entrada pelo id`() {
        val json = ResumeRegistry.encode(listOf(entrada))
        assertEquals(entrada, ResumeRegistry.decode(json).first())
    }

    @Test fun `entrada mais recente fica no topo e nao duplica`() {
        val outra = entrada.copy(id = "def", title = "Outro")
        val atualizada = entrada.copy(positionMs = 300_000)
        val lista = ResumeRegistry.upsert(listOf(entrada, outra), atualizada)
        assertEquals(listOf("abc", "def"), lista.map { it.id })
        assertEquals(300_000L, lista.first().positionMs)
    }

    @Test fun `remover tira a entrada da lista`() {
        assertNull(ResumeRegistry.remove(listOf(entrada), "abc").firstOrNull())
    }

    @Test fun `json invalido devolve lista vazia`() {
        assertEquals(emptyList<ResumeEntry>(), ResumeRegistry.decode("nao é json"))
    }
}
