package br.com.cinemora.tv

import br.com.cinemora.tv.data.XtreamParser
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeDetailTest {
    private val creds = Credentials("http://s", "u", "p")
    private val serie = Series("7", "O Mentalista", "s", null)

    private val json = """
        {"episodes":{"1":[
          {"id":"11","episode_num":"1","title":"O Mentalista - S01E01 - Piloto","container_extension":"mkv",
           "info":{"plot":"Patrick entra para a agência.","duration":"00:44:12",
                   "movie_image":"http://cdn/ep1.jpg"}},
          {"id":"12","episode_num":"2","title":"O Mentalista - S01E02 - Cabelo ruivo","container_extension":"mkv",
           "info":{}}
        ]}}
    """.trimIndent()

    @Test fun `episodio traz sinopse duracao e miniatura`() {
        val ep = XtreamParser.seriesDetail(json, serie, creds).seasons.first().episodes.first()
        assertEquals("Piloto", ep.title)
        assertEquals("Patrick entra para a agência.", ep.plot)
        assertEquals("44min", ep.duration)
        assertEquals("http://cdn/ep1.jpg", ep.thumbUrl)
    }

    @Test fun `episodio sem detalhe nao quebra`() {
        val ep = XtreamParser.seriesDetail(json, serie, creds).seasons.first().episodes[1]
        assertNull(ep.plot)
        assertNull(ep.duration)
        assertNull(ep.thumbUrl)
    }
}
