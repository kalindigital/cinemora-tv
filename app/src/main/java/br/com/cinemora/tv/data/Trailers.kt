package br.com.cinemora.tv.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * O provedor manda o trailer como campo youtube_trailer, às vezes só o identificador do
 * vídeo, às vezes o endereço inteiro. O trailer abre no seletor de apps da TV: o usuário
 * escolhe onde ver (app do YouTube, SmartTube, navegador…).
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

    /** Endereço web do vídeo — o que o YouTube, o SmartTube e o navegador sabem abrir. */
    fun enderecoWeb(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    /**
     * Abre o trailer pelo endereço web, sem forçar seletor: quando há mais de um app
     * capaz (YouTube, SmartTube…) e nenhum definido como padrão, o próprio Android mostra
     * o "Abrir com", onde o usuário escolhe o app e se vale só uma vez ou sempre. Assim
     * dá para fixar o app preferido. Devolve false só se nada conseguir abrir.
     */
    fun abrir(context: Context, videoId: String): Boolean {
        val ver = Intent(Intent.ACTION_VIEW, Uri.parse(enderecoWeb(videoId)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(ver) }.isSuccess
    }
}
