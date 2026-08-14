package br.com.cinemora.tv

import br.com.cinemora.tv.data.PairingForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingFormTest {
    @Test fun `extrai a chave do formulario`() {
        assertEquals("sk-proj-abc123", PairingForm.readKey("key=sk-proj-abc123"))
    }

    @Test fun `decodifica caracteres escapados e espacos`() {
        assertEquals("sk-a+b/c", PairingForm.readKey("key=sk-a%2Bb%2Fc&outro=1"))
    }

    @Test fun `ignora espacos em volta`() {
        assertEquals("sk-abc", PairingForm.readKey("key=++sk-abc++"))
    }

    @Test fun `corpo sem chave devolve nulo`() {
        assertNull(PairingForm.readKey("outro=1"))
        assertNull(PairingForm.readKey("key="))
    }
}
