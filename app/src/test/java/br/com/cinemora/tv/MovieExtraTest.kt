package br.com.cinemora.tv

import br.com.cinemora.tv.data.XtreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieExtraTest {
    @Test fun `le backdrop genero e duracao do detalhe`() {
        val json = """
            {"info":{"plot":"Uma história.","genre":"Ficção científica",
             "duration":"2:04:00","backdrop_path":["https://image.tmdb.org/t/p/w1280/abc.jpg"]}}
        """.trimIndent()
        val extra = XtreamParser.movieExtra(json)
        assertEquals("Uma história.", extra.plot)
        assertEquals("Ficção científica", extra.genre)
        assertEquals("2h04", extra.duration)
        assertEquals("https://image.tmdb.org/t/p/w1280/abc.jpg", extra.backdrop)
    }

    @Test fun `aceita backdrop como texto simples`() {
        val json = """{"info":{"backdrop_path":"http://x/b.jpg"}}"""
        assertEquals("http://x/b.jpg", XtreamParser.movieExtra(json).backdrop)
    }

    @Test fun `detalhe vazio nao quebra`() {
        val extra = XtreamParser.movieExtra("{}")
        assertNull(extra.backdrop)
        assertNull(extra.plot)
    }

    @Test fun `duracao em minutos vira formato curto`() {
        assertEquals("1h30", XtreamParser.movieExtra("""{"info":{"duration":"1:30:00"}}""").duration)
    }

    @Test fun `serie usa o tempo de episodio como duracao`() {
        val json = """
            {"info":{"plot":"Uma série.","genre":"Drama","episode_run_time":"44",
             "backdrop_path":["https://image.tmdb.org/t/p/w1280/s.jpg"]}}
        """.trimIndent()
        val extra = XtreamParser.seriesExtra(json)
        assertEquals("Uma série.", extra.plot)
        assertEquals("Drama", extra.genre)
        assertEquals("44min", extra.duration)
        assertEquals("https://image.tmdb.org/t/p/w1280/s.jpg", extra.backdrop)
    }

    @Test fun `serie sem tempo de episodio fica sem duracao`() {
        assertNull(XtreamParser.seriesExtra("""{"info":{"plot":"x"}}""").duration)
    }
}
