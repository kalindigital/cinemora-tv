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
        runCatching { tts?.stop() }
        runCatching { player?.stop(); player?.release() }
        player = null
    }

    fun release() {
        stop()
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
        val audio = openAi.speech(text) ?: return falarComGoogle(text)
        runCatching {
            val arquivo = File(context.cacheDir, "fala.mp3").apply { writeBytes(audio) }
            player = MediaPlayer().apply {
                setDataSource(arquivo.absolutePath)
                setOnCompletionListener { it.release(); player = null }
                prepare()
                start()
            }
        }.onFailure { falarComGoogle(text) }
    }

    private companion object { const val FALA_ID = "cinemora-fala" }
}
