package br.com.cinemora.tv

import br.com.cinemora.tv.data.Trailers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrailersTest {
    @Test fun `aceita o identificador solto`() {
        assertEquals("mOuTbZhHCnY", Trailers.videoId("mOuTbZhHCnY"))
    }

    @Test fun `extrai o identificador do endereco completo`() {
        assertEquals("l0ae8gNI9u4", Trailers.videoId("https://www.youtube.com/watch?v=l0ae8gNI9u4"))
        assertEquals("l0ae8gNI9u4", Trailers.videoId("https://youtu.be/l0ae8gNI9u4"))
        assertEquals("l0ae8gNI9u4", Trailers.videoId("http://www.youtube.com/embed/l0ae8gNI9u4?rel=0"))
    }

    // O seletor de apps recebe o endereço web: é o que YouTube, SmartTube e navegador abrem.
    @Test fun `endereco web aponta para o video no youtube`() {
        assertEquals("https://www.youtube.com/watch?v=l0ae8gNI9u4", Trailers.enderecoWeb("l0ae8gNI9u4"))
    }

    @Test fun `campo vazio ou estranho nao vira trailer`() {
        assertNull(Trailers.videoId(null))
        assertNull(Trailers.videoId(""))
        assertNull(Trailers.videoId("   "))
        assertNull(Trailers.videoId("sem trailer"))
    }
}
