package br.com.cinemora.tv.data

import android.content.Context
import br.com.cinemora.tv.R
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * TVs antigas (Android 6/7) têm um conjunto de certificados raiz desatualizado: a raiz da
 * Let's Encrypt (TMDB) e a da Sectigo (imgur) não estão lá, então as capas simplesmente não
 * carregam. Aqui embutimos as raízes atuais e validamos contra elas quando o sistema falha.
 */
object TvTrust {
    fun socketFactory(context: Context): Pair<SSLSocketFactory, X509TrustManager> {
        val manager = CompositeTrustManager(listOf(systemTrustManager(), bundledTrustManager(context)))
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(manager), null) }
        return ssl.socketFactory to manager
    }

    private fun systemTrustManager(): X509TrustManager = trustManagerFrom(null)

    private fun bundledTrustManager(context: Context): X509TrustManager {
        val certificates = context.resources.openRawResource(R.raw.trusted_roots).use { input ->
            CertificateFactory.getInstance("X.509").generateCertificates(input)
        }
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            certificates.forEachIndexed { index, certificate -> setCertificateEntry("raiz-$index", certificate) }
        }
        return trustManagerFrom(store)
    }

    private fun trustManagerFrom(store: KeyStore?): X509TrustManager =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(store) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

    /** Aceita o certificado se qualquer um dos conjuntos (sistema ou embutido) o validar. */
    private class CompositeTrustManager(private val managers: List<X509TrustManager>) : X509TrustManager {
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            var ultimaFalha: CertificateException? = null
            managers.forEach { manager ->
                try {
                    manager.checkServerTrusted(chain, authType)
                    return
                } catch (falha: CertificateException) {
                    ultimaFalha = falha
                }
            }
            throw ultimaFalha ?: CertificateException("Nenhuma âncora de confiança disponível.")
        }

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
            managers.first().checkClientTrusted(chain, authType)

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            managers.flatMap { it.acceptedIssuers.asList() }.toTypedArray()
    }
}
