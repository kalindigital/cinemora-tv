package br.com.cinemora.tv

import br.com.cinemora.tv.player.BufferAviso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BufferAvisoTest {
    @Test fun `engasgo curto nao mostra nada`() {
        // Trocar de trecho engasga por instantes; avisar a cada engasgo faria a tela piscar.
        assertNull(BufferAviso.mensagem(0))
        assertNull(BufferAviso.mensagem(900))
    }

    @Test fun `espera normal mostra carregando`() {
        assertEquals("Carregando…", BufferAviso.mensagem(1_500))
        assertEquals("Carregando…", BufferAviso.mensagem(7_000))
    }

    @Test fun `espera longa avisa que a conexao esta lenta`() {
        assertEquals("Conexão lenta. Ainda tentando…", BufferAviso.mensagem(12_000))
    }

    @Test fun `retoma a conexao so depois de muito tempo parado`() {
        assertFalse(BufferAviso.deveTentarDeNovo(10_000))
        assertTrue(BufferAviso.deveTentarDeNovo(25_000))
    }
}
