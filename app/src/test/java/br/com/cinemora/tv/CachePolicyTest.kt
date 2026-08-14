package br.com.cinemora.tv

import br.com.cinemora.tv.data.CachePolicy
import br.com.cinemora.tv.data.CacheTtl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    @Test fun `cache dentro da validade esta fresco`() {
        val savedAt = 1_000_000L
        val umaHoraDepois = savedAt + 60 * 60 * 1000L
        assertTrue(CachePolicy.isFresh(savedAt, umaHoraDepois, CacheTtl.SIX_HOURS))
    }

    @Test fun `cache no limite exato da validade esta vencido`() {
        val savedAt = 1_000_000L
        val seisHorasDepois = savedAt + CacheTtl.SIX_HOURS.millis
        assertFalse(CachePolicy.isFresh(savedAt, seisHorasDepois, CacheTtl.SIX_HOURS))
    }

    @Test fun `timestamp no futuro nao conta como fresco`() {
        val savedAt = 5_000_000L
        val agoraAntes = savedAt - 10_000L
        assertFalse(CachePolicy.isFresh(savedAt, agoraAntes, CacheTtl.TWELVE_HOURS))
    }
}
