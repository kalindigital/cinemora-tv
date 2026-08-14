package br.com.cinemora.tv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.cinemora.tv.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Atualização pelo GitHub Releases: o app compara a versão instalada com a do último
 * release, baixa o APK anexado e entrega ao instalador do sistema.
 */
class UpdateService(
    private val context: Context,
    private val repo: String = BuildConfig.GITHUB_REPO,
    private val versaoAtual: String = BuildConfig.VERSION_NAME,
) {
    private val client by lazy {
        val (socketFactory, trustManager) = TvTrust.socketFactory(context)
        OkHttpClient.Builder().sslSocketFactory(socketFactory, trustManager).build()
    }

    fun isConfigured(): Boolean = repo.isNotBlank()

    fun check(): UpdateInfo? {
        if (!isConfigured()) return null
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        val corpo = runCatching {
            client.newCall(request).execute().use { resposta ->
                if (resposta.isSuccessful) resposta.body?.string() else null
            }
        }.getOrNull() ?: return null
        return GithubUpdates.parse(corpo, versaoAtual)
    }

    /** Baixa o APK para a área do próprio app; devolve nulo se a rede falhar. */
    fun download(update: UpdateInfo, onProgress: (Int) -> Unit): File? {
        val destino = File(context.cacheDir, "cinemora-${update.version}.apk")
        val request = Request.Builder().url(update.apkUrl).build()
        return runCatching {
            client.newCall(request).execute().use { resposta ->
                val corpo = resposta.body ?: return null
                if (!resposta.isSuccessful) return null
                val total = corpo.contentLength()
                var baixado = 0L
                corpo.byteStream().use { entrada ->
                    destino.outputStream().use { saida ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val lidos = entrada.read(buffer)
                            if (lidos <= 0) break
                            saida.write(buffer, 0, lidos)
                            baixado += lidos
                            if (total > 0) onProgress((baixado * 100 / total).toInt())
                        }
                    }
                }
            }
            destino
        }.getOrNull()
    }

    /** Abre o instalador do sistema; o usuário confirma na tela da TV. */
    fun install(apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
