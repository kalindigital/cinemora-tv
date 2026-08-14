package br.com.cinemora.tv.data

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

enum class VoiceMode { GOOGLE, OPENAI, MUDO }

/**
 * Fala as respostas do chat. A voz do Google é instantânea e sem custo; a da OpenAI é mais
 * natural, porém depende de rede e cobra por caractere — daí a escolha ficar com o usuário.
 */
class Speaker(private val context: Context, private val openAi: OpenAiClient) {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    // A voz da OpenAI é uma chamada de rede: na thread principal o Android a bloqueia,
    // o erro era engolido e a fala caía silenciosamente para o Google.
    private val rede = java.util.concurrent.Executors.newSingleThreadExecutor()
    @Volatile private var pedidoAtual = 0L

    fun speak(text: String, mode: VoiceMode) {
        if (text.isBlank() || mode == VoiceMode.MUDO) return
        stop()
        when (mode) {
            VoiceMode.GOOGLE -> falarComGoogle(text)
            VoiceMode.OPENAI -> falarComOpenAi(text)
            VoiceMode.MUDO -> Unit
        }
    }

    fun stop() {
        pedidoAtual = 0L
        runCatching { tts?.stop() }
        runCatching { player?.stop(); player?.release() }
        player = null
    }

    fun release() {
        stop()
        rede.shutdownNow()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun falarComGoogle(text: String) {
        val existente = tts
        if (existente != null) {
            existente.speak(text, TextToSpeech.QUEUE_FLUSH, null, FALA_ID)
            return
        }
        // A engine leva um instante para iniciar; a fala sai no callback.
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, FALA_ID)
            }
        }
    }

    private fun falarComOpenAi(text: String) {
        val pedido = System.currentTimeMillis()
        pedidoAtual = pedido
        rede.execute {
            val audio = openAi.speech(text)
            // Se outra fala começou nesse meio-tempo, esta é descartada.
            if (pedido != pedidoAtual) return@execute
            if (audio == null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { falarComGoogle(text) }
                return@execute
            }
            runCatching {
                val arquivo = File(context.cacheDir, "fala-$pedido.mp3").apply { writeBytes(audio) }
                val novo = MediaPlayer().apply {
                    setDataSource(arquivo.absolutePath)
                    setOnCompletionListener { it.release(); arquivo.delete() }
                    prepare()
                }
                if (pedido != pedidoAtual) {
                    novo.release(); arquivo.delete()
                } else {
                    player = novo
                    novo.start()
                }
            }.onFailure {
                android.os.Handler(android.os.Looper.getMainLooper()).post { falarComGoogle(text) }
            }
        }
    }

    private companion object { const val FALA_ID = "cinemora-fala" }
}
