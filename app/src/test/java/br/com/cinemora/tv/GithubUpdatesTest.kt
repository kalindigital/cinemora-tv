package br.com.cinemora.tv

import br.com.cinemora.tv.data.GithubUpdates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdatesTest {
    private val release = """
        {
          "tag_name": "v1.2.0",
          "name": "Cinemora 1.2.0",
          "body": "- Avanço automático de episódio\n- Correção das capas",
          "assets": [
            {"name": "mapping.txt", "browser_download_url": "http://x/mapping.txt"},
            {"name": "cinemora-tv.apk", "browser_download_url": "http://x/cinemora-tv.apk"}
          ]
        }
    """.trimIndent()

    @Test fun `le versao changelog e apk do release`() {
        val info = GithubUpdates.parse(release, versaoAtual = "1.0.0")!!
        assertEquals("1.2.0", info.version)
        assertEquals("http://x/cinemora-tv.apk", info.apkUrl)
        assertTrue(info.changelog.contains("Avanço automático"))
    }

    @Test fun `versao igual ou anterior nao vira atualizacao`() {
        assertNull(GithubUpdates.parse(release, versaoAtual = "1.2.0"))
        assertNull(GithubUpdates.parse(release, versaoAtual = "1.3.0"))
    }

    @Test fun `release sem apk e ignorado`() {
        val semApk = """{"tag_name":"v9.9.9","body":"x","assets":[]}"""
        assertNull(GithubUpdates.parse(semApk, versaoAtual = "1.0.0"))
    }

    @Test fun `compara versoes por numero e nao por texto`() {
        assertTrue(GithubUpdates.isNewer("1.10.0", "1.9.0"))
        assertFalse(GithubUpdates.isNewer("1.9.0", "1.10.0"))
        assertTrue(GithubUpdates.isNewer("v2.0", "1.9.9"))
        assertFalse(GithubUpdates.isNewer("1.0.0", "1.0.0"))
    }
}
