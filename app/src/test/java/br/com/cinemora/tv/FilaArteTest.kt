package br.com.cinemora.tv

import br.com.cinemora.tv.data.FilaArte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilaArteTest {
    @Test fun `atende primeiro o pedido mais recente`() {
        val fila = FilaArte()
        fila.pedir("1"); fila.pedir("2"); fila.pedir("3")
        // Quem está na tela agora é o último pedido: buscar em ordem de chegada
        // entregaria arte para cartões que já saíram de vista.
        assertEquals("3", fila.proximo())
        assertEquals("2", fila.proximo())
    }

    @Test fun `nao pede duas vezes o mesmo filme`() {
        val fila = FilaArte()
        assertTrue(fila.pedir("7"))
        assertFalse(fila.pedir("7"))
        assertEquals("7", fila.proximo())
        assertNull(fila.proximo())
    }

    @Test fun `fila cheia descarta os pedidos antigos`() {
        val fila = FilaArte(limite = 2)
        fila.pedir("1"); fila.pedir("2"); fila.pedir("3")
        assertEquals("3", fila.proximo())
        assertEquals("2", fila.proximo())
        assertNull(fila.proximo())
    }

    @Test fun `id ja resolvido nao volta para a fila`() {
        val fila = FilaArte()
        fila.resolvidos(setOf("9"))
        assertFalse(fila.pedir("9"))
        assertNull(fila.proximo())
    }
}
