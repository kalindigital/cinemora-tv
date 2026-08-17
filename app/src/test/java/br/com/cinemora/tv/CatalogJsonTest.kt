package br.com.cinemora.tv

import br.com.cinemora.tv.data.CatalogJson
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Channel
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogJsonTest {
    @Test fun `round-trip preserva filmes canais series e campos nulos`() {
        val catalog = Catalog(
            movieCategories = listOf(Category("c1", "Ação")),
            movies = listOf(
                Video("1", "Filme A", "c1", "http://x/a.png", "http://x/movie/u/p/1.mp4", "2020", "6.7", "Sinopse A"),
                Video("2", "Filme B", "c1", null, "http://x/movie/u/p/2.mkv", null, null, null),
            ),
            liveCategories = listOf(Category("l1", "Notícias")),
            channels = listOf(Channel("9", "Canal X", "l1", "http://x/logo.png", "http://x/live/u/p/9.ts")),
            seriesCategories = listOf(Category("s1", "Drama")),
            series = listOf(Series("7", "Série Z", "s1", "http://x/z.png", "2019", "8.1", "Sinopse Z")),
        )
        val restored = CatalogJson.decode(CatalogJson.encode(catalog))
        assertEquals(catalog, restored)
    }

    @Test fun `decodificar json vazio devolve catalogo vazio`() {
        assertEquals(Catalog(), CatalogJson.decode("{}"))
    }

    @Test fun `cache legado com ano e qualidade no titulo e limpo ao carregar`() {
        val json = """{
            "movies":[{"id":"1","title":"Avatar (2009) [4K]","categoryId":"c","streamUrl":"http://x/1.mp4"}],
            "series":[{"id":"7","title":"Breaking Bad [1080p]","categoryId":"s"}]
        }"""
        val catalog = CatalogJson.decode(json)
        assertEquals("Avatar", catalog.movies[0].title)
        assertEquals("Breaking Bad", catalog.series[0].title)
    }
}
