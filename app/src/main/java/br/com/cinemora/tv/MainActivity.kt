package br.com.cinemora.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import br.com.cinemora.tv.player.PlayerActivity
import br.com.cinemora.tv.ui.CinemoraApp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var confirmandoSaida = false
    private val limparConfirmacao = Runnable { confirmandoSaida = false }
    private val ui = Handler(Looper.getMainLooper())
    private val playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val terminou = result.resultCode == Activity.RESULT_OK &&
            result.data?.getBooleanExtra(PlayerActivity.EXTRA_FINISHED, false) == true
        if (terminou) viewModel.onPlaybackFinished()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Sair pede confirmação: um Voltar sem querer não deve fechar o app.
        // As telas internas (detalhe, QR, recomendações) tratam o Voltar antes disto.
        onBackPressedDispatcher.addCallback(this) {
            if (!confirmandoSaida) {
                confirmandoSaida = true
                Toast.makeText(this@MainActivity, "Pressione Voltar novamente para sair", Toast.LENGTH_SHORT).show()
                ui.postDelayed(limparConfirmacao, CONFIRMACAO_MS)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        setContent {
            CinemoraApp(
                state = viewModel.state,
                seriesDetail = viewModel.seriesDetail,
                moviePlot = viewModel.moviePlot,
                movieDetailExtra = viewModel.movieDetailExtra,
                featuredPlot = viewModel.featuredPlot,
                movieFocus = viewModel.movieFocus,
                movieFocusExtra = viewModel.movieFocusExtra,
                movieArt = viewModel.movieArt,
                onFocusMovie = viewModel::onMovieFocused,
                onNeedArt = viewModel::onArtNeeded,
                progresso = viewModel.progresso,
                seriesFocus = viewModel.seriesFocus,
                seriesFocusExtra = viewModel.seriesFocusExtra,
                seriesArt = viewModel.seriesArt,
                onFocusSeries = viewModel::onSeriesFocused,
                aiState = viewModel.aiState,
                resumeMs = viewModel.resumeMs,
                resumeOf = viewModel::resumePositionOf,
                watchedOf = viewModel::isStreamWatched,
                recommendations = viewModel.recommendations,
                onCloseRecommendations = viewModel::clearRecommendations,
                sortOrder = viewModel.sortOrder,
                cacheTtl = viewModel.cacheTtl,
                account = viewModel.account(),
                onSignIn = viewModel::signIn,
                onRetry = viewModel::returnToLogin,
                onPlay = ::play,
                onPlayQueue = ::playQueue,
                onRecordWatched = viewModel::recordWatched,
                onRemoveWatched = viewModel::removeWatched,
                onToggleFavorite = viewModel::toggleFavorite,
                onLoadSeriesDetail = viewModel::loadSeriesDetail,
                onClearSeriesDetail = viewModel::clearSeriesDetail,
                onLoadMoviePlot = viewModel::loadMoviePlot,
                onClearMoviePlot = viewModel::clearMoviePlot,
                onRefresh = viewModel::refresh,
                onClearCache = viewModel::clearCache,
                onSetCacheTtl = viewModel::changeCacheTtl,
                onSetSortOrder = viewModel::changeSortOrder,
                onAskAi = viewModel::askAi,
                hasOpenAiKey = viewModel.hasOpenAiKey,
                onSaveOpenAiKey = viewModel::saveOpenAiKey,
                chatSession = viewModel.currentChat,
                chatSessions = viewModel.chatSessions,
                chatThinking = viewModel.chatThinking,
                chatError = viewModel.chatError,
                speaking = viewModel.speaking,
                onStopSpeech = viewModel::stopSpeech,
                typewriter = viewModel.typewriter,
                onSetTypewriter = viewModel::changeTypewriter,
                liveEnabled = viewModel.liveEnabled,
                onSetLiveEnabled = viewModel::changeLiveEnabled,
                familyMode = viewModel.familyMode,
                onSetFamilyMode = viewModel::changeFamilyMode,
                hideAdult = viewModel.hideAdult,
                onSetHideAdult = viewModel::changeHideAdult,
                novidades = viewModel.novidades,
                watchlist = viewModel.watchlist,
                chegaram = viewModel.chegaram,
                onAddWatchlist = viewModel::addToWatchlist,
                onRemoveWatchlist = viewModel::removeFromWatchlist,
                onDismissChegaram = viewModel::dismissChegaram,
                tasteProfile = viewModel.tasteProfile,
                onRefreshTaste = viewModel::refreshTasteProfile,
                insight = viewModel.insight,
                insightTitulo = viewModel.insightTitulo,
                onAskVerdict = viewModel::askVerdict,
                onAskRecap = { serie, t, e -> viewModel.askRecap(serie, t, e) },
                onCloseInsight = viewModel::clearInsight,
                liveActive = viewModel.liveActive,
                liveStatus = viewModel.liveStatus,
                onStartLive = viewModel::startLive,
                onStopLive = viewModel::stopLive,
                onSendChat = viewModel::sendChat,
                onNewChat = viewModel::newChat,
                onOpenChat = viewModel::openChat,
                onDeleteChat = viewModel::deleteChat,
                onSpeakAgain = viewModel::speakAgain,
                onContinueFrom = viewModel::continueFrom,
                voiceMode = viewModel.voiceMode,
                onSetVoiceMode = viewModel::changeVoiceMode,
                openAiVoice = viewModel.openAiVoice,
                onSetOpenAiVoice = viewModel::changeOpenAiVoice,
                voiceSpeed = viewModel.voiceSpeed,
                onSetVoiceSpeed = viewModel::changeVoiceSpeed,
                resumeEntry = viewModel.resumeEntry,
                onResumeEntry = { entrada ->
                    viewModel.dismissResume()
                    play(entrada.title, entrada.streamUrl, false, entrada.posterUrl)
                },
                onDismissResume = viewModel::dismissResume,
                updateState = viewModel.updateState,
                onCheckUpdate = { viewModel.checkForUpdate() },
                onDownloadUpdate = viewModel::downloadUpdate,
                onDismissUpdate = viewModel::dismissUpdate,
                onLogout = viewModel::logout,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Rede de segurança: se o player foi encerrado sem devolver resultado, o sinal
        // gravado em disco ainda é lido aqui.
        viewModel.onPlaybackFinished()
        viewModel.refreshResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        ui.removeCallbacks(limparConfirmacao)
    }

    private fun play(title: String, url: String, restart: Boolean, poster: String?) =
        playerLauncher.launch(playerIntent(title, url, restart, poster))

    /** Série: leva a fila dos próximos episódios para o player oferecer "próximo". */
    private fun playQueue(
        title: String,
        url: String,
        restart: Boolean,
        poster: String?,
        upcoming: List<Triple<String, String, String>>,
    ) {
        playerLauncher.launch(
            playerIntent(title, url, restart, poster).apply {
                putStringArrayListExtra(PlayerActivity.EXTRA_QUEUE_TITLES, ArrayList(upcoming.map { it.first }))
                putStringArrayListExtra(PlayerActivity.EXTRA_QUEUE_URLS, ArrayList(upcoming.map { it.second }))
                putStringArrayListExtra(PlayerActivity.EXTRA_QUEUE_LABELS, ArrayList(upcoming.map { it.third }))
            },
        )
    }

    private fun playerIntent(title: String, url: String, restart: Boolean, poster: String?) =
        Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_RESTART, restart)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
            putExtra(PlayerActivity.EXTRA_URL, url)
            putExtra(PlayerActivity.EXTRA_POSTER, poster)
        }

    private companion object {
        const val CONFIRMACAO_MS = 3_000L
    }
}
