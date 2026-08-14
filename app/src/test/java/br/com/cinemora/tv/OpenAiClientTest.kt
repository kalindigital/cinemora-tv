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

    @Test fun `resposta sem escolhas devolve lista vazia`() {
        assertTrue(OpenAiClient.parseTitles("""{"choices":[]}""").isEmpty())
    }
}
