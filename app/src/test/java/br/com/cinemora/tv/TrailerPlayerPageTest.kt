package br.com.cinemora.tv

import br.com.cinemora.tv.data.TrailerPlayerPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailerPlayerPageTest {
    private val html = TrailerPlayerPage.html("l0ae8gNI9u4")

    @Test fun `usa o player oficial do YouTube`() {
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertTrue(html.contains("YT.Player"))
    }

    @Test fun `carrega o video pedido`() {
        assertTrue(html.contains("videoId: 'l0ae8gNI9u4'"))
    }

    @Test fun `comeca sozinho e sem videos de outros canais no fim`() {
        assertTrue(html.contains("autoplay: 1"))
        assertTrue(html.contains("rel: 0"))
    }

    // A API do IFrame recusa o embed quando a origem não bate com a base da página.
    @Test fun `declara a origem esperada pelo embed`() {
        assertTrue(html.contains("origin: 'https://www.youtube.com'"))
        assertEquals("https://www.youtube.com", TrailerPlayerPage.BASE_URL)
    }

    @Test fun `avisa o aplicativo quando o video acaba ou falha`() {
        assertTrue(html.contains("onError"))
        assertTrue(html.contains("onStateChange"))
        assertTrue(html.contains("CinemoraTrailer"))
    }

    @Test fun `o identificador vai escapado para nao injetar script`() {
        val perigoso = TrailerPlayerPage.html("abc'</script><script>x=1//")
        assertFalse(perigoso.contains("</script><script>x=1"))
    }

    @Test fun `erros de embed desativado sao tratados como impossivel tocar aqui`() {
        // 101 e 150 são os códigos do YouTube para "o dono não permite embed"
        assertTrue(TrailerPlayerPage.ehEmbedProibido(101))
        assertTrue(TrailerPlayerPage.ehEmbedProibido(150))
        assertFalse(TrailerPlayerPage.ehEmbedProibido(2))
        assertFalse(TrailerPlayerPage.ehEmbedProibido(5))
    }
}
