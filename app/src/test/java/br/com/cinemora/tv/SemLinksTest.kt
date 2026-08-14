package br.com.cinemora.tv

import br.com.cinemora.tv.data.SemLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SemLinksTest {
    @Test fun `remove endereco solto no meio do texto`() {
        val texto = "Vale a pena. Veja em https://www.adorocinema.com/filmes/123 para mais."
        assertEquals("Vale a pena. Veja em para mais.", SemLinks.limpar(texto))
    }

    @Test fun `remove link em markdown mantendo o texto`() {
        val texto = "Segundo a [Rotten Tomatoes](https://rottentomatoes.com/m/x), é ótimo."
        assertEquals("Segundo a Rotten Tomatoes, é ótimo.", SemLinks.limpar(texto))
    }

    @Test fun `remove citacao entre parenteses`() {
        val texto = "O filme é elogiado (fonte: www.imdb.com/title/tt123)."
        assertFalse(SemLinks.limpar(texto).contains("imdb.com"))
    }

    @Test fun `texto sem link fica igual`() {
        val texto = "Suspense bem construído, indicado para quem gosta de ritmo lento."
        assertEquals(texto, SemLinks.limpar(texto))
    }
}
