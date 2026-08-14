package br.com.cinemora.tv

import br.com.cinemora.tv.data.ChatMessage
import br.com.cinemora.tv.data.ChatRole
import br.com.cinemora.tv.data.ChatSession
import br.com.cinemora.tv.data.ChatStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStoreTest {
    private val sessao = ChatSession(
        id = "s1",
        title = "Filmes de terror",
        updatedAt = 1_000L,
        messages = listOf(
            ChatMessage(ChatRole.USER, "quero terror", emptyList()),
            ChatMessage(ChatRole.ASSISTANT, "Que tal estes?", listOf("Hereditário", "A Bruxa")),
        ),
    )

    @Test fun `round-trip preserva mensagens e sugestoes`() {
        assertEquals(listOf(sessao), ChatStore.decode(ChatStore.encode(listOf(sessao))))
    }

    @Test fun `titulo vem da primeira pergunta encurtada`() {
        val longa = "me recomenda um filme de comédia romântica dos anos noventa para assistir hoje"
        assertEquals("me recomenda um filme de comédia…", ChatStore.titleFrom(longa, limit = 32))
        assertEquals("quero terror", ChatStore.titleFrom("quero terror", limit = 32))
    }

    @Test fun `conversa atualizada vai para o topo da lista`() {
        val outra = sessao.copy(id = "s2", title = "Outra", updatedAt = 500L)
        val lista = ChatStore.upsert(listOf(outra, sessao), sessao.copy(updatedAt = 2_000L))
        assertEquals(listOf("s1", "s2"), lista.map { it.id })
    }

    @Test fun `apenas as ultimas mensagens vao para o modelo`() {
        val muitas = (1..20).map { ChatMessage(ChatRole.USER, "m$it", emptyList()) }
        val enviadas = ChatStore.lastMessages(muitas, limit = 6)
        assertEquals(6, enviadas.size)
        assertEquals("m20", enviadas.last().text)
    }

    @Test fun `json invalido nao derruba o app`() {
        assertTrue(ChatStore.decode("quebrado").isEmpty())
    }
}
