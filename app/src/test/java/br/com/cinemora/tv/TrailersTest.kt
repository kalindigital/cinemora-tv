package br.com.cinemora.tv

import br.com.cinemora.tv.data.Trailers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // O esquema vnd.youtube: é aceito pelo aplicativo do YouTube da TV (não dá
    // erro), mas ele abre na tela inicial em vez de tocar o vídeo. Quem toca é o
    // endereço https — por isso ele precisa ser a primeira tentativa.
    @Test fun `o endereco https vem primeiro porque o esquema vnd nao toca o video`() {
        val enderecos = Trailers.enderecos("l0ae8gNI9u4")
        assertEquals("https://www.youtube.com/watch?v=l0ae8gNI9u4", enderecos.first())
        assertTrue(enderecos.contains("vnd.youtube:l0ae8gNI9u4"))
    }

    @Test fun `campo vazio ou estranho nao vira trailer`() {
        assertNull(Trailers.videoId(null))
        assertNull(Trailers.videoId(""))
        assertNull(Trailers.videoId("   "))
        assertNull(Trailers.videoId("sem trailer"))
    }
}
