package br.com.cinemora.tv

import br.com.cinemora.tv.data.XtreamParser
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamParserTest {
    private val creds = Credentials("http://host", "user", "pass")

    @Test fun `autenticacao valida quando auth e 1`() {
        assertTrue(XtreamParser.isAuthenticated("""{"user_info":{"auth":1}}"""))
        assertFalse(XtreamParser.isAuthenticated("""{"user_info":{"auth":0}}"""))
        assertFalse(XtreamParser.isAuthenticated("{}"))
    }

    @Test fun `filmes normalizam nota e ignoram itens sem id`() {
        val json = """[
            {"stream_id":"1","name":"Filme","category_id":"c","stream_icon":"http://i/1.png","container_extension":"mkv","rating":"6.666","year":"2020","plot":"Sinopse"},
            {"name":"sem id"}
        ]"""
        val movies = XtreamParser.movies(json, creds)
        assertEquals(1, movies.size)
        assertEquals("6.7", movies[0].rating)
        assertEquals("http://host/movie/user/pass/1.mkv", movies[0].streamUrl)
    }

    @Test fun `ano vem do titulo quando o provedor nao envia o campo year`() {
        val json = """[{"stream_id":"1","name":"...E o Vento Levou (1939)","category_id":"c","container_extension":"mp4"}]"""
        assertEquals("1939", XtreamParser.movies(json, creds)[0].year)
    }

    @Test fun `titulo sem ano nao inventa ano`() {
        val json = """[{"stream_id":"1","name":"Filme Sem Ano","category_id":"c"}]"""
        assertNull(XtreamParser.movies(json, creds)[0].year)
    }

    @Test fun `campo year do provedor tem prioridade sobre o titulo`() {
        val json = """[{"stream_id":"1","name":"Filme (1999)","year":"2005","category_id":"c"}]"""
        assertEquals("2005", XtreamParser.movies(json, creds)[0].year)
    }

    @Test fun `canais montam url de live`() {
        val json = """[{"stream_id":"9","name":"Canal","category_id":"l","stream_icon":"http://i/9.png"}]"""
        val channels = XtreamParser.channels(json, creds)
        assertEquals(1, channels.size)
        assertEquals("http://host/live/user/pass/9.ts", channels[0].streamUrl)
    }

    @Test fun `series leem campos principais`() {
        val json = """[{"series_id":"7","name":"Serie","category_id":"s","cover":"http://i/7.png","rating":"8","plot":"Enredo"}]"""
        val series = XtreamParser.series(json)
        assertEquals(1, series.size)
        assertEquals("8.0", series[0].rating)
        assertEquals("Serie", series[0].title)
    }

    @Test fun `detalhe da serie agrupa episodios por temporada ordenados`() {
        val json = """{"episodes":{
            "1":[{"id":"101","title":"Ep2","episode_num":"2","container_extension":"mkv"},
                 {"id":"100","title":"Ep1","episode_num":"1","container_extension":"mp4"}],
            "2":[{"id":"200","episode_num":"1","container_extension":"mp4"}]
        }}"""
        val detail = XtreamParser.seriesDetail(json, Series("7", "Serie", "s", null), creds)
        assertEquals(listOf(1, 2), detail.seasons.map { it.number })
        assertEquals(listOf(1, 2), detail.seasons[0].episodes.map { it.episode })
        assertEquals("http://host/series/user/pass/100.mp4", detail.seasons[0].episodes[0].streamUrl)
    }

    @Test fun `nota zero ou ausente vira nula`() {
        assertNull(XtreamParser.formatRating("0"))
        assertNull(XtreamParser.formatRating(""))
    }

    @Test fun `listagem de series traz a arte 16 por 9 quando o provedor manda`() {
        val json = """
            [{"series_id":"5","name":"Peaky Blinders","category_id":"2","cover":"http://c/p.jpg",
              "backdrop_path":["https://image.tmdb.org/t/p/w1280/wiE.jpg"]},
             {"series_id":"6","name":"Sem arte","category_id":"2","backdrop_path":[]}]
        """.trimIndent()
        val series = XtreamParser.series(json)
        assertEquals("https://image.tmdb.org/t/p/w1280/wiE.jpg", series[0].backdropUrl)
        assertNull(series[1].backdropUrl)
    }
}
