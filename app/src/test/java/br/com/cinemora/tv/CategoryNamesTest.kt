package br.com.cinemora.tv

import br.com.cinemora.tv.data.CategoryNames
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryNamesTest {
    @Test fun `remove o prefixo do tipo dentro da aba`() {
        assertEquals("Aventura", CategoryNames.short("Filmes | Aventura"))
        assertEquals("Legendados", CategoryNames.short("Filmes | Legendados"))
        assertEquals("Netflix", CategoryNames.short("Séries | Netflix"))
    }

    @Test fun `mantem o nome quando nao ha prefixo`() {
        assertEquals("Oscar 2026", CategoryNames.short("Oscar 2026"))
    }

    @Test fun `preserva barras que fazem parte do nome`() {
        assertEquals("Cinema (CAM) | Extra", CategoryNames.short("Filmes | Cinema (CAM) | Extra"))
    }
}
