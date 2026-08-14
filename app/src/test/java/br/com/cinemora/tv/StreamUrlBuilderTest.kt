package br.com.cinemora.tv

import br.com.cinemora.tv.data.StreamUrlBuilder
import br.com.cinemora.tv.model.Credentials
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUrlBuilderTest {
    @Test fun `remove barra final e preserva extensao do conteudo`() {
        val result = StreamUrlBuilder.movie(Credentials("https://catalogo.exemplo/", "conta", "segredo",), "42", "mkv")
        assertEquals("https://catalogo.exemplo/movie/conta/segredo/42.mkv", result)
    }

    @Test fun `usa mp4 quando provedor nao informa extensao`() {
        val result = StreamUrlBuilder.movie(Credentials("https://catalogo.exemplo", "conta", "segredo"), "42", "")
        assertEquals("https://catalogo.exemplo/movie/conta/segredo/42.mp4", result)
    }
}
