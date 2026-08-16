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

    /**
     * Endereços a tentar, na ordem. O https vem primeiro de propósito: o
     * aplicativo do YouTube da TV aceita o esquema `vnd.youtube:` sem reclamar,
     * mas abre na tela inicial em vez de tocar o trailer. Como o
     * `startActivity` não falha nesse caso, a tentativa seguinte nunca
     * aconteceria — o vídeo simplesmente não tocava. O `vnd.youtube:` fica como
     * reserva para aparelhos antigos que só entendem esse esquema.
     */
    fun enderecos(videoId: String): List<String> = listOf(
        "https://www.youtube.com/watch?v=$videoId",
        "vnd.youtube:$videoId",
    )

    /** Abre no aplicativo do YouTube; sem ele, tenta o navegador. Devolve false se nada abrir. */
    fun abrir(context: Context, videoId: String): Boolean =
        enderecos(videoId).any { endereco ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(endereco))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.isSuccess
        }
}
