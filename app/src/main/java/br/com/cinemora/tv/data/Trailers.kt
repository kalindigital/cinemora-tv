package br.com.cinemora.tv.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * O provedor manda o trailer como campo youtube_trailer, às vezes só o identificador do
 * vídeo, às vezes o endereço inteiro. O trailer toca no aplicativo do YouTube da TV: o
 * vídeo é deles, e reproduzi-lo por fora do player oficial fica fora das regras deles.
 */
object Trailers {
    private val ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val NO_ENDERECO = Regex("(?:v=|youtu\\.be/|/embed/|/v/)([A-Za-z0-9_-]{11})")

    fun videoId(bruto: String?): String? {
        val texto = bruto?.trim().orEmpty()
        if (texto.isEmpty()) return null
        if (ID.matches(texto)) return texto
        return NO_ENDERECO.find(texto)?.groupValues?.get(1)
    }

    /** Abre no aplicativo do YouTube; sem ele, tenta o navegador. Devolve false se nada abrir. */
    fun abrir(context: Context, videoId: String): Boolean {
        val tentativas = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")),
        )
        return tentativas.any { intent ->
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }
}
