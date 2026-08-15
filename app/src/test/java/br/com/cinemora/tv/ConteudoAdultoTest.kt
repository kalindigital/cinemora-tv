package br.com.cinemora.tv

import br.com.cinemora.tv.data.ConteudoAdulto
import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConteudoAdultoTest {
    @Test fun `marca entre colchetes no nome e adulto`() {
        assertTrue(ConteudoAdulto.ehAdulto("[XXX]  Hentai 11"))
        assertTrue(ConteudoAdulto.ehAdulto("(xxx) Alguma coisa"))
    }

    @Test fun `filme de acao com xis no nome continua liberado`() {
        // Sem a marca entre colchetes, "xxx" no meio do nome é só o título.
        assertFalse(ConteudoAdulto.ehAdulto("xXx: Reativado (2017)"))
        assertFalse(ConteudoAdulto.ehAdulto("MaXXXine (2024)"))
    }

    @Test fun `categoria adulta e reconhecida pelo nome`() {
        listOf("Filmes Adultos", "Onlyfans [adultos]", "Hentai [adultos]", "Canais | XXX +18 LGBT")
            .forEach { assertTrue(it, ConteudoAdulto.categoriaAdulta(it)) }
        assertFalse(ConteudoAdulto.categoriaAdulta("Filmes | Ação"))
    }

    @Test fun `filtrar tira categoria adulta e titulo marcado`() {
        val catalogo = Catalog(
            movieCategories = listOf(Category("1", "Filmes | Ação"), Category("9", "Filmes Adultos")),
            movies = listOf(
                Video("a", "Duro de Matar", "1", null, "u", null, null, null),
                Video("b", "[XXX]  Hentai 11", "1", null, "u", null, null, null),
                Video("c", "Qualquer um", "9", null, "u", null, null, null),
            ),
            seriesCategories = listOf(Category("2", "Séries | Drama")),
            series = listOf(Series("s1", "Uma série", "2", null)),
            liveCategories = emptyList(),
            channels = emptyList(),
        )
        val limpo = ConteudoAdulto.filtrar(catalogo)
        assertEquals(listOf("Duro de Matar"), limpo.movies.map { it.title })
        assertEquals(1, limpo.movieCategories.size)
        assertEquals(1, limpo.series.size)
    }
}
