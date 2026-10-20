package br.com.cinemora.tv.player

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.data.ResumeEntry
import br.com.cinemora.tv.data.ResumeRegistry
import br.com.cinemora.tv.data.WatchNext

/**
 * Player em View pura (sem Compose): dentro de um AndroidView o D-pad não chegava de
 * forma confiável ao PlayerView, e os controles não respondiam depois que o vídeo iniciava.
 */
class PlayerActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var nextPanel: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var seekBadge: TextView
    private lateinit var bufferBadge: TextView
    private val store by lazy { LocalStore(this) }

    private var streamUrl = ""
    private var currentTitle = ""
    private var posterUrl: String? = null
    private var queueTitles: List<String> = emptyList()
    private var queueUrls: List<String> = emptyList()
    private var queueLabels: List<String> = emptyList()
    private var queueIndex = 0
    private var confirmandoSaida = false
    private var bufferandoDesde = 0L
    private var ultimaRetomada = 0L
    private var autoNextEm = 0
    private var autoNextCancelado = false
    private var lastSeekAt = 0L
    private var lastSeekDirection = 0
    private var seekStepMs = STEP_SMALL
    private val ui = Handler(Looper.getMainLooper())
    private val hideBadge = Runnable { seekBadge.visibility = View.GONE }
    private val limparConfirmacao = Runnable { confirmandoSaida = false }
    private val tick = object : Runnable {
        override fun run() {
            atualizarAvisoDeBuffer()
            updateNextPanel()
            ui.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        streamUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        require(streamUrl.isNotBlank()) { "Vídeo inválido." }
        currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        posterUrl = intent.getStringExtra(EXTRA_POSTER)
        queueTitles = intent.getStringArrayListExtra(EXTRA_QUEUE_TITLES).orEmpty()
        queueUrls = intent.getStringArrayListExtra(EXTRA_QUEUE_URLS).orEmpty()
        queueLabels = intent.getStringArrayListExtra(EXTRA_QUEUE_LABELS).orEmpty()
        // Sem isso a TV apaga a tela durante a reprodução.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        player = ExoPlayer.Builder(this).build()
        playerView = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            this.player = this@PlayerActivity.player
            useController = true
            controllerAutoShow = false
            controllerShowTimeoutMs = 4_000
            setShowNextButton(false)
            setShowPreviousButton(false)
            // Roda de carregamento do próprio player, inclusive com os controles escondidos.
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            keepScreenOn = true
        }
        setContentView(buildLayout())

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onPlaybackFinished()
                bufferandoDesde = if (state == Player.STATE_BUFFERING) {
                    if (bufferandoDesde == 0L) SystemClock.elapsedRealtime() else bufferandoDesde
                } else {
                    0L
                }
                if (bufferandoDesde == 0L) ultimaRetomada = 0L
                atualizarAvisoDeBuffer()
                updateNextPanel()
            }
        })

        // Voltar é progressivo: cancela a contagem, fecha os controles e só sai na confirmação.
        onBackPressedDispatcher.addCallback(this) {
            when {
                nextPanel.visibility == View.VISIBLE && !autoNextCancelado && autoNextEm > 0 -> {
                    autoNextCancelado = true
                    autoNextEm = 0
                    updateNextPanel()
                }
                playerView.isControllerFullyVisible -> playerView.hideController()
                !confirmandoSaida -> {
                    confirmandoSaida = true
                    mostrarAviso("Pressione Voltar novamente para sair")
                    ui.postDelayed(limparConfirmacao, CONFIRM_EXIT_MS)
                }
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        startPlayback(streamUrl, currentTitle, restart = intent.getBooleanExtra(EXTRA_RESTART, false))
        ui.post(tick)
    }

    /**
     * Mostra em que pé está o carregamento e, se a espera passar do razoável, refaz o pedido
     * ao servidor — travar sem explicação é o que mais confunde quem está assistindo.
     */
    private fun atualizarAvisoDeBuffer() {
        if (bufferandoDesde == 0L) {
            bufferBadge.visibility = View.GONE
            return
        }
        val parado = SystemClock.elapsedRealtime() - bufferandoDesde
        val mensagem = BufferAviso.mensagem(parado)
        bufferBadge.text = mensagem.orEmpty()
        bufferBadge.visibility = if (mensagem == null) View.GONE else View.VISIBLE
        if (BufferAviso.deveTentarDeNovo(parado) && SystemClock.elapsedRealtime() - ultimaRetomada > RETRY_MS) {
            ultimaRetomada = SystemClock.elapsedRealtime()
            player.prepare()
        }
    }

    /** PlayerView ao fundo, título no topo e o painel de "próximo" no canto inferior. */
    private fun buildLayout(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(playerView, FrameLayout.LayoutParams(MATCH, MATCH))

        titleView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
            setPadding(48, 30, 48, 0)
            visibility = View.GONE
        }
        root.addView(titleView, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP or Gravity.START))

        nextButton = Button(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(PANEL_GRAY)
            isAllCaps = false
            setOnClickListener { playNext() }
            // Sem isso não dá para saber se o botão está com foco.
            setOnFocusChangeListener { _, comFoco ->
                setBackgroundColor(if (comFoco) ACCENT else PANEL_GRAY)
            }
        }
        nextPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
            setPadding(48, 0, 48, 0)
            addView(nextButton)
        }
        // No topo, à direita: embaixo ficava sobre a barra de tempo e o botão de ajustes.
        root.addView(
            nextPanel,
            FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END).apply { topMargin = 24 },
        )

        seekBadge = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            setBackgroundColor(PANEL_GRAY)
            setPadding(36, 20, 36, 20)
            visibility = View.GONE
        }
        root.addView(seekBadge, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))

        bufferBadge = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            setBackgroundColor(PANEL_GRAY)
            setPadding(28, 14, 28, 14)
            visibility = View.GONE
        }
        root.addView(
            bufferBadge,
            FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                .apply { bottomMargin = 120 },
        )
        return root
    }

    private fun startPlayback(url: String, title: String, restart: Boolean) {
        streamUrl = url
        currentTitle = title
        titleView.text = title
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        if (restart) {
            store.clearPosition(url)
        } else {
            val saved = store.position(url)
            if (saved > 0) player.seekTo((saved - RESUME_REWIND_MS).coerceAtLeast(0))
        }
        player.playWhenReady = true
        playerView.requestFocus()
        nextPanel.visibility = View.GONE
        autoNextEm = 0
        autoNextCancelado = false
        // Registra assim que começa: gravar só na saída fazia o banner continuar
        // apontando para o título anterior quando este era deixado antes dos 15s.
        registrarNoHistorico(player.currentPosition, 0)
    }

    /** Toca o próximo da fila, marcando o atual como visto. */
    private fun playNext() {
        if (queueIndex >= queueUrls.size) return
        store.markStreamWatched(streamUrl)
        store.clearPosition(streamUrl)
        val url = queueUrls[queueIndex]
        val title = queueTitles.getOrElse(queueIndex) { "" }
        queueIndex++
        startPlayback(url, title, restart = true)
    }

    private fun onPlaybackFinished() {
        store.markStreamWatched(streamUrl)
        store.clearPosition(streamUrl)
        removerDaTelaInicial()
        if (queueIndex >= queueUrls.size) {
            // Filme (ou fim da série): a Home mostra as recomendações.
            store.saveFinishedStream(streamUrl)
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_FINISHED, true))
            finish()
        }
    }

    /**
     * O próximo episódio fica sempre à mão (aparece junto com os controles) e, no fim do
     * episódio, entra em contagem regressiva e avança sozinho — "Voltar" cancela.
     */
    private fun updateNextPanel() {
        // O título aparece junto com os controles, não o tempo todo sobre o vídeo.
        titleView.visibility = if (playerView.isControllerFullyVisible) View.VISIBLE else View.GONE

        val temProximo = queueIndex < queueUrls.size
        if (!temProximo) {
            nextPanel.visibility = View.GONE
            return
        }
        val duration = player.duration
        val restante = if (duration > 0) duration - player.currentPosition else Long.MAX_VALUE
        val fimDoEpisodio = (duration > 0 && restante in 0..NEXT_WINDOW_MS) ||
            player.playbackState == Player.STATE_ENDED
        val contagemAtiva = fimDoEpisodio && !autoNextCancelado
        val mostrar = fimDoEpisodio || playerView.isControllerFullyVisible

        if (!mostrar) {
            nextPanel.visibility = View.GONE
            return
        }

        val label = queueLabels.getOrElse(queueIndex) { "" }
        if (contagemAtiva) {
            if (autoNextEm == 0) autoNextEm = AUTO_NEXT_SECONDS
            nextButton.text = "Próximo em $autoNextEm  ·  $label"
            autoNextEm--
            if (autoNextEm <= 0) {
                playNext()
                return
            }
        } else {
            nextButton.text = if (label.isNotBlank()) "Próximo: $label" else "Próximo episódio"
        }

        if (nextPanel.visibility != View.VISIBLE) {
            nextPanel.visibility = View.VISIBLE
            if (fimDoEpisodio) nextButton.requestFocus()
        }
    }

    /**
     * Com os controles escondidos: botão central abre a barra e as setas avançam/retrocedem.
     * Com a barra aberta (ou o painel de próximo), as setas voltam a navegar entre os botões.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!playerView.isControllerFullyVisible && nextPanel.visibility != View.VISIBLE) {
            val central = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER
            if (central) {
                if (event.action == KeyEvent.ACTION_DOWN) playerView.showController()
                return true
            }
            val direction = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> 1
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> -1
                else -> 0
            }
            if (direction != 0) {
                if (event.action == KeyEvent.ACTION_DOWN) seekBy(direction)
                return true
            }
            if (event.keyCode in DirectionKeys) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Cliques seguidos (não segurar) aumentam o passo: 15s → 30s → 90s. Parar por 2s, ou
     * trocar de direção, volta para 15s. O retorno visual é o selo próprio: abrir o
     * controle do player faria as setas seguintes navegarem os botões e a escalada se perderia.
     */
    private fun seekBy(direction: Int) {
        val now = android.os.SystemClock.elapsedRealtime()
        val emSequencia = now - lastSeekAt <= CHAIN_WINDOW_MS && direction == lastSeekDirection
        seekStepMs = when {
            !emSequencia -> STEP_SMALL
            seekStepMs == STEP_SMALL -> STEP_MEDIUM
            else -> STEP_LARGE
        }
        lastSeekAt = now
        lastSeekDirection = direction

        val duration = player.duration
        var target = player.currentPosition + direction * seekStepMs
        if (target < 0) target = 0
        if (duration > 0 && target > duration) target = duration
        player.seekTo(target)
        showSeekBadge(direction, target)
    }

    private fun showSeekBadge(direction: Int, target: Long) {
        val sinal = if (direction > 0) "+" else "−"
        mostrarAviso("$sinal${seekStepMs / 1000}s   ${formatTime(target)}", 1_200)
    }

    private fun mostrarAviso(texto: String, duracaoMs: Long = CONFIRM_EXIT_MS) {
        seekBadge.text = texto
        seekBadge.visibility = View.VISIBLE
        ui.removeCallbacks(hideBadge)
        ui.removeCallbacks(limparConfirmacao)
        ui.postDelayed(hideBadge, duracaoMs)
    }

    private fun formatTime(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val sec = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    override fun onPause() {
        super.onPause()
        rememberPosition()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        ui.removeCallbacks(tick)
        ui.removeCallbacks(hideBadge)
        ui.removeCallbacks(limparConfirmacao)
        rememberPosition()
        player.release()
    }

    /** Guarda a posição só de conteúdo sob demanda e ainda não concluído (ao vivo não tem duração). */
    private fun rememberPosition() {
        val duration = player.duration
        val position = player.currentPosition
        val worthResuming = duration > 0 && position > RESUME_REWIND_MS && position < duration - FINISHED_MARGIN_MS
        if (worthResuming) {
            store.savePosition(streamUrl, position)
            atualizarTelaInicial(position, duration)
        } else {
            store.clearPosition(streamUrl)
            removerDaTelaInicial()
        }
        // Sair já no finzinho conta como assistido: quase ninguém vê os créditos até o fim.
        val quaseNoFim = duration > 0 && position >= duration * FINISHED_RATIO
        if (quaseNoFim && queueIndex >= queueUrls.size) {
            store.markStreamWatched(streamUrl)
            store.saveFinishedStream(streamUrl)
        }
    }

    /** Espelha o que está em andamento na linha "Continuar assistindo" do sistema. */
    private fun atualizarTelaInicial(position: Long, duration: Long) {
        WatchNext.update(this, registrarNoHistorico(position, duration))
    }

    private fun registrarNoHistorico(position: Long, duration: Long): ResumeEntry {
        val entrada = ResumeEntry(
            id = streamUrl.hashCode().toString(),
            title = currentTitle,
            streamUrl = streamUrl,
            posterUrl = posterUrl,
            positionMs = position.coerceAtLeast(0),
            durationMs = duration,
        )
        store.saveResumeEntries(ResumeRegistry.upsert(store.resumeEntries(), entrada))
        return entrada
    }

    private fun removerDaTelaInicial() {
        val id = streamUrl.hashCode().toString()
        store.saveResumeEntries(ResumeRegistry.remove(store.resumeEntries(), id))
        WatchNext.remove(this, id)
    }

    companion object {
        /** Intervalo mínimo entre duas tentativas de retomar o mesmo stream. */
        private const val RETRY_MS = 20_000L
        const val EXTRA_URL = "video_url"
        const val EXTRA_POSTER = "video_poster"
        const val EXTRA_TITLE = "video_title"
        const val EXTRA_RESTART = "video_restart"
        const val EXTRA_FINISHED = "video_finished"
        const val EXTRA_QUEUE_TITLES = "queue_titles"
        const val EXTRA_QUEUE_URLS = "queue_urls"
        const val EXTRA_QUEUE_LABELS = "queue_labels"
        private const val RESUME_REWIND_MS = 15_000L
        private const val FINISHED_MARGIN_MS = 30_000L
        private const val NEXT_WINDOW_MS = 60_000L
        private const val AUTO_NEXT_SECONDS = 10
        private const val FINISHED_RATIO = 0.9
        private const val STEP_SMALL = 15_000L
        private const val STEP_MEDIUM = 30_000L
        private const val STEP_LARGE = 90_000L
        private const val CHAIN_WINDOW_MS = 2_000L
        private const val CONFIRM_EXIT_MS = 3_000L
        private const val ACCENT = 0xFFE50914.toInt()
        private const val PANEL_GRAY = 0xB33A3A3A.toInt()
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        private val DirectionKeys = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        )
    }
}
