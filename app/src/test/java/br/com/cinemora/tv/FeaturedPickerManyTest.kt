package br.com.cinemora.tv

import br.com.cinemora.tv.data.FeaturedPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedPickerManyTest {
    private val catalogo = (1..100).map { "filme $it" }

    @Test fun `escolhe a quantidade pedida sem repetir`() {
        val escolhidos = FeaturedPicker.pickMany(catalogo, seed = 7, quantidade = 10)
        assertEquals(10, escolhidos.size)
        assertEquals(10, escolhidos.toSet().size)
    }

    @Test fun `mesma semente devolve a mesma seleção`() {
        assertEquals(
            FeaturedPicker.pickMany(catalogo, seed = 42, quantidade = 10),
            FeaturedPicker.pickMany(catalogo, seed = 42, quantidade = 10),
        )
    }

    @Test fun `sementes diferentes mudam a seleção`() {
        assertTrue(
            FeaturedPicker.pickMany(catalogo, seed = 1, quantidade = 10) !=
                FeaturedPicker.pickMany(catalogo, seed = 2, quantidade = 10),
        )
    }

    @Test fun `catalogo menor que o pedido devolve o que existe`() {
        assertEquals(3, FeaturedPicker.pickMany(listOf("a", "b", "c"), seed = 1, quantidade = 10).size)
        assertTrue(FeaturedPicker.pickMany(emptyList<String>(), seed = 1, quantidade = 10).isEmpty())
    }
}
