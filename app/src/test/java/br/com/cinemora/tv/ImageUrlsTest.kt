package br.com.cinemora.tv

import br.com.cinemora.tv.data.ImageUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlsTest {
    @Test fun `capa do tmdb pede tamanho de cartao`() {
        val original = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/abc.jpg"
        assertEquals("http://image.tmdb.org/t/p/w342/abc.jpg", ImageUrls.card(original))
    }

    @Test fun `capa grande do tmdb usa tamanho maior no detalhe`() {
        val original = "https://image.tmdb.org/t/p/original/abc.jpg"
        assertEquals("http://image.tmdb.org/t/p/w500/abc.jpg", ImageUrls.detail(original))
    }

    @Test fun `capa do tmdb usa http porque TV antiga nao valida Lets Encrypt`() {
        val original = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/abc.jpg"
        assertEquals("http://image.tmdb.org/t/p/w342/abc.jpg", ImageUrls.card(original))
    }

    @Test fun `url de outro servidor fica intacta`() {
        val outra = "http://meuservidor.com/capas/filme.png"
        assertEquals(outra, ImageUrls.card(outra))
    }

    @Test fun `sem url continua sem url`() {
        assertNull(ImageUrls.card(null))
    }

    @Test fun `backdrop pede a arte larga em http`() {
        assertEquals(
            "http://image.tmdb.org/t/p/w1280/abc.jpg",
            ImageUrls.backdrop("https://image.tmdb.org/t/p/original/abc.jpg"),
        )
    }

    @Test fun `backdrop ausente continua nulo`() {
        assertEquals(null, ImageUrls.backdrop(null))
    }
}
