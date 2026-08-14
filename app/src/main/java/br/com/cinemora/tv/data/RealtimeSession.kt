package br.com.cinemora.tv.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** O que a conversa ao vivo informa para a tela. */
sealed interface RealtimeEvent {
    data object Conectado : RealtimeEvent
    data object Ouvindo : RealtimeEvent
    data object Respondendo : RealtimeEvent
    data class VocêDisse(val texto: String) : RealtimeEvent
    data class ElaDisse(val texto: String) : RealtimeEvent
    data class Erro(val mensagem: String) : RealtimeEvent
    data object Encerrado : RealtimeEvent
}

/**
 * Conversa ao vivo com a IA: o microfone fica aberto, o áudio vai em tempo real e a resposta
 * volta em áudio enquanto é gerada. A detecção de fim de fala é feita pelo servidor.
 */
class RealtimeSession(
    private val apiKey: String,
    private val instrucoes: String,
    private val voz: String,
    private val onEvent: (RealtimeEvent) -> Unit,
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private var socket: WebSocket? = null
    private var gravador: AudioRecord? = null
    private var alto: AudioTrack? = null
    @Volatile private var ativo = false
    @Volatile private var falandoAgora = false
    // Fila de reprodução: escrever direto no AudioTrack em modo não bloqueante descartava
    // pedaços quando o buffer enchia, e a fala saía acelerada e picotada.
    private val fila = java.util.concurrent.LinkedBlockingQueue<ByteArray>()

    fun start() {
        if (ativo) return
        ativo = true
        val request = Request.Builder()
            .url("wss://api.openai.com/v1/realtime?model=$MODELO")
            .header("Authorization", "Bearer $apiKey")
            .build()
        socket = client.newWebSocket(request, Ouvinte())
    }

    fun stop() {
        ativo = false
        fila.clear()
        falandoAgora = false
        runCatching { socket?.close(1000, "fim") }
        socket = null
        runCatching { gravador?.stop(); gravador?.release() }
        gravador = null
        runCatching { alto?.stop(); alto?.release() }
        alto = null
        onEvent(RealtimeEvent.Encerrado)
    }

    /** Configura a sessão: voz, transcrição da sua fala e detecção automática de turno. */
    private fun configurar(ws: WebSocket) {
        val sessao = JSONObject()
            .put("type", "realtime")
            .put("model", MODELO)
            .put("instructions", instrucoes)
            .put(
                "audio",
                JSONObject()
                    .put(
                        "input",
                        JSONObject()
                            .put("format", JSONObject().put("type", "audio/pcm").put("rate", TAXA))
                            .put("transcription", JSONObject().put("model", "whisper-1"))
                            .put("turn_detection", JSONObject().put("type", "server_vad")),
                    )
                    .put(
                        "output",
                        JSONObject()
                            .put("format", JSONObject().put("type", "audio/pcm").put("rate", TAXA))
                            .put("voice", voz),
                    ),
            )
        ws.send(JSONObject().put("type", "session.update").put("session", sessao).toString())
    }

    private fun abrirMicrofone(ws: WebSocket) {
        val tamanho = AudioRecord.getMinBufferSize(TAXA, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(BUFFER)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            TAXA,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            tamanho,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            onEvent(RealtimeEvent.Erro("Não consegui abrir o microfone."))
            return
        }
        gravador = record
        record.startRecording()
        onEvent(RealtimeEvent.Ouvindo)

        thread(isDaemon = true, name = "cinemora-mic") {
            val buffer = ByteArray(BUFFER)
            while (ativo) {
                val lidos = record.read(buffer, 0, buffer.size)
                if (lidos <= 0) continue
                // Enquanto ela fala, o microfone não envia: na TV o alto-falante volta
                // para o microfone e ela se interrompia sozinha.
                if (falandoAgora) continue
                val audio = Base64.encodeToString(buffer.copyOf(lidos), Base64.NO_WRAP)
                val enviado = ws.send(
                    JSONObject().put("type", "input_audio_buffer.append").put("audio", audio).toString(),
                )
                if (!enviado) {
                    onEvent(RealtimeEvent.Erro("o canal parou de aceitar áudio"))
                    break
                }
            }
        }
    }

    private fun tocar(pcm: ByteArray) {
        fila.offer(pcm)
    }

    private fun abrirSaida() {
        val track = alto ?: AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(TAXA)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(BUFFER * 8)
            .build()
            .also { alto = it; it.play() }

        thread(isDaemon = true, name = "cinemora-fala") {
            while (ativo) {
                val pedaco = runCatching { fila.poll(200, TimeUnit.MILLISECONDS) }.getOrNull()
                if (pedaco == null) {
                    // Fila vazia: a resposta terminou, o microfone volta a valer.
                    if (falandoAgora) {
                        falandoAgora = false
                        onEvent(RealtimeEvent.Ouvindo)
                    }
                    continue
                }
                falandoAgora = true
                var escrito = 0
                // Bloqueante e em laço: só sai daqui quando todo o pedaço tocou.
                while (escrito < pedaco.size && ativo) {
                    val n = runCatching {
                        track.write(pedaco, escrito, pedaco.size - escrito, AudioTrack.WRITE_BLOCKING)
                    }.getOrDefault(-1)
                    if (n <= 0) break
                    escrito += n
                }
            }
        }
    }

    private inner class Ouvinte : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onEvent(RealtimeEvent.Conectado)
            configurar(webSocket)
            abrirSaida()
            abrirMicrofone(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val evento = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (val tipo = evento.optString("type")) {
                "response.output_audio.delta", "response.audio.delta" -> {
                    val pcm = runCatching { Base64.decode(evento.optString("delta"), Base64.DEFAULT) }.getOrNull()
                    if (pcm != null) tocar(pcm)
                }
                "input_audio_buffer.speech_started" -> onEvent(RealtimeEvent.Ouvindo)
                "response.created" -> onEvent(RealtimeEvent.Respondendo)
                "conversation.item.input_audio_transcription.completed" ->
                    onEvent(RealtimeEvent.VocêDisse(evento.optString("transcript").trim()))
                "response.output_audio_transcript.done", "response.audio_transcript.done" ->
                    onEvent(RealtimeEvent.ElaDisse(evento.optString("transcript").trim()))
                "response.done", "response.output_audio.done", "response.audio.done" -> {
                    if (fila.isEmpty()) {
                        falandoAgora = false
                        onEvent(RealtimeEvent.Ouvindo)
                    }
                }
                "error" -> onEvent(
                    RealtimeEvent.Erro(evento.optJSONObject("error")?.optString("message") ?: tipo),
                )
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // Sem isto a tela ficava presa em "respondendo" quando a linha caía.
            if (ativo) onEvent(RealtimeEvent.Erro("a conexão foi encerrada ($code)"))
            stop()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onEvent(RealtimeEvent.Erro(t.message ?: "conexão perdida"))
            stop()
        }
    }

    private companion object {
        const val MODELO = "gpt-realtime-mini"
        const val TAXA = 24_000
        const val BUFFER = 3_200
    }
}

/** Volume do microfone é usado só para a animação; manter aqui evita expor o AudioManager. */
fun AudioManager.silenciado(): Boolean = getStreamVolume(AudioManager.STREAM_MUSIC) == 0
