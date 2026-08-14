package br.com.cinemora.tv

import br.com.cinemora.tv.data.OpenAiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiClientTest {
    @Test fun `extrai titulos do json aninhado da resposta`() {
        val response = """
            {"choices":[{"message":{"role":"assistant","content":"{\"titulos\":[\"A Origem\",\"Interestelar\"]}"}}]}
        """.trimIndent()
        assertEquals(listOf("A Origem", "Interestelar"), OpenAiClient.parseTitles(response))
    }

    @Test fun `le a conversa e as sugestoes da resposta com busca na web`() {
        val response = """
            {"status":"completed","output":[
              {"type":"web_search_call"},
              {"type":"message","content":[{"type":"output_text",
                "text":"{\"resposta\":\"Achei estes lançamentos.\",\"titulos\":[\"Pânico 7\"]}"}]}
            ]}
        """.trimIndent()
        val reply = OpenAiClient.parseChat(response)
        assertEquals("Achei estes lançamentos.", reply.text)
        assertEquals(listOf("Pânico 7"), reply.titles)
    }

    @Test fun `resposta sem escolhas devolve lista vazia`() {
        assertTrue(OpenAiClient.parseTitles("""{"choices":[]}""").isEmpty())
    }
}
