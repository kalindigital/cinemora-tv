package br.com.cinemora.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.AppState
import br.com.cinemora.tv.DetailState
import br.com.cinemora.tv.UpdateState
import br.com.cinemora.tv.R
import br.com.cinemora.tv.AiState
import br.com.cinemora.tv.data.CacheTtl
import br.com.cinemora.tv.data.ChatSession
import br.com.cinemora.tv.data.VoiceMode
import br.com.cinemora.tv.data.VoiceSpeed
import br.com.cinemora.tv.data.Recommendations
import br.com.cinemora.tv.data.ResumeEntry
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

private val TopBarHeight = 64.dp

@Composable
fun CinemoraApp(
    state: AppState,
    seriesDetail: DetailState,
    moviePlot: String?,
    featuredPlot: String?,
    aiState: AiState,
    resumeMs: Long,
    resumeOf: (String) -> Long,
    watchedOf: (String) -> Boolean,
    recommendations: List<Video>,
    onCloseRecommendations: () -> Unit,
    sortOrder: SortOrder,
    cacheTtl: CacheTtl,
    account: Credentials?,
    onSignIn: (String, String, String) -> Unit,
    onRetry: () -> Unit,
    onPlay: (String, String, Boolean, String?) -> Unit,
    onPlayQueue: (String, String, Boolean, String?, List<Triple<String, String, String>>) -> Unit,
    onRecordWatched: (String) -> Unit,
    onRemoveWatched: (Video) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLoadSeriesDetail: (Series) -> Unit,
    onClearSeriesDetail: () -> Unit,
    onLoadMoviePlot: (Video) -> Unit,
    onClearMoviePlot: () -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onSetCacheTtl: (CacheTtl) -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onAskAi: (String) -> Unit,
    hasOpenAiKey: Boolean,
    onSaveOpenAiKey: (String) -> Unit,
    chatSession: ChatSession?,
    chatSessions: List<ChatSession>,
    chatThinking: Boolean,
    chatError: String?,
    speaking: Boolean,
    onStopSpeech: () -> Unit,
    typewriter: Boolean,
    onSetTypewriter: (Boolean) -> Unit,
    liveEnabled: Boolean,
    onSetLiveEnabled: (Boolean) -> Unit,
    liveActive: Boolean,
    liveStatus: String?,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onSendChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenChat: (ChatSession) -> Unit,
    onDeleteChat: (ChatSession) -> Unit,
    onSpeakAgain: (String) -> Unit,
    onContinueFrom: (Int) -> Unit,
    voiceMode: VoiceMode,
    onSetVoiceMode: (VoiceMode) -> Unit,
    openAiVoice: String,
    onSetOpenAiVoice: (String) -> Unit,
    voiceSpeed: VoiceSpeed,
    onSetVoiceSpeed: (VoiceSpeed) -> Unit,
    resumeEntry: ResumeEntry?,
    onResumeEntry: (ResumeEntry) -> Unit,
    onDismissResume: () -> Unit,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Coral, onPrimary = Color.White, secondary = Signal,
            background = Ink, surface = Panel, onBackground = Mist, onSurface = Mist,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            when (state) {
                AppState.Login -> LoginScreen(onSignIn)
                AppState.Loading -> LoadingScreen()
                is AppState.Home -> HomeShell(
                    home = state,
                    seriesDetail = seriesDetail,
                    moviePlot = moviePlot,
                    featuredPlot = featuredPlot,
                    aiState = aiState,
                    resumeMs = resumeMs,
                    resumeOf = resumeOf,
                    watchedOf = watchedOf,
                    recommendations = recommendations,
                    onCloseRecommendations = onCloseRecommendations,
                    sortOrder = sortOrder,
                    cacheTtl = cacheTtl,
                    account = account,
                    onPlay = onPlay,
                    onPlayQueue = onPlayQueue,
                    onRecordWatched = onRecordWatched,
                    onRemoveWatched = onRemoveWatched,
                    onToggleFavorite = onToggleFavorite,
                    onLoadSeriesDetail = onLoadSeriesDetail,
                    onClearSeriesDetail = onClearSeriesDetail,
                    onLoadMoviePlot = onLoadMoviePlot,
                    onClearMoviePlot = onClearMoviePlot,
                    onRefresh = onRefresh,
                    onClearCache = onClearCache,
                    onSetCacheTtl = onSetCacheTtl,
                    onSetSortOrder = onSetSortOrder,
                    onAskAi = onAskAi,
                    hasOpenAiKey = hasOpenAiKey,
                    onSaveOpenAiKey = onSaveOpenAiKey,
                    chatSession = chatSession,
                    chatSessions = chatSessions,
                    chatThinking = chatThinking,
                    chatError = chatError,
                    speaking = speaking,
                    onStopSpeech = onStopSpeech,
                    typewriter = typewriter,
                    onSetTypewriter = onSetTypewriter,
                    liveEnabled = liveEnabled,
                    onSetLiveEnabled = onSetLiveEnabled,
                    liveActive = liveActive,
                    liveStatus = liveStatus,
                    onStartLive = onStartLive,
                    onStopLive = onStopLive,
                    onSendChat = onSendChat,
                    onNewChat = onNewChat,
                    onOpenChat = onOpenChat,
                    onDeleteChat = onDeleteChat,
                    onSpeakAgain = onSpeakAgain,
                    onContinueFrom = onContinueFrom,
                    voiceMode = voiceMode,
                    onSetVoiceMode = onSetVoiceMode,
                    openAiVoice = openAiVoice,
                    onSetOpenAiVoice = onSetOpenAiVoice,
                    voiceSpeed = voiceSpeed,
                    onSetVoiceSpeed = onSetVoiceSpeed,
                    resumeEntry = resumeEntry,
                    onResumeEntry = onResumeEntry,
                    onDismissResume = onDismissResume,
                    updateState = updateState,
                    onCheckUpdate = onCheckUpdate,
                    onDownloadUpdate = onDownloadUpdate,
                    onDismissUpdate = onDismissUpdate,
                    onLogout = onLogout,
                )
                is AppState.Error -> ErrorScreen(state.message, onRetry)
            }
        }
    }
}

private enum class Section(val label: String, val icon: ImageVector) {
    FILMES("Filmes", Icons.Rounded.Movie),
    SERIES("Séries", Icons.Rounded.Tv),
    CANAIS("Canais", Icons.Rounded.LiveTv),
    CATEGORIAS("Categorias", Icons.Rounded.GridView),
    PESQUISA("Pesquisa", Icons.Rounded.Search),
    IA("IA", Icons.Rounded.AutoAwesome),
    DEFINICOES("Definições", Icons.Rounded.Settings),
    PERFIL("Perfil", Icons.Rounded.Person),
}

private val MainTabs = listOf(Section.FILMES, Section.SERIES, Section.CANAIS, Section.CATEGORIAS)

@Composable
private fun HomeShell(
    home: AppState.Home,
    seriesDetail: DetailState,
    moviePlot: String?,
    featuredPlot: String?,
    aiState: AiState,
    resumeMs: Long,
    resumeOf: (String) -> Long,
    watchedOf: (String) -> Boolean,
    recommendations: List<Video>,
    onCloseRecommendations: () -> Unit,
    sortOrder: SortOrder,
    cacheTtl: CacheTtl,
    account: Credentials?,
    onPlay: (String, String, Boolean, String?) -> Unit,
    onPlayQueue: (String, String, Boolean, String?, List<Triple<String, String, String>>) -> Unit,
    onRecordWatched: (String) -> Unit,
    onRemoveWatched: (Video) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLoadSeriesDetail: (Series) -> Unit,
    onClearSeriesDetail: () -> Unit,
    onLoadMoviePlot: (Video) -> Unit,
    onClearMoviePlot: () -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onSetCacheTtl: (CacheTtl) -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onAskAi: (String) -> Unit,
    hasOpenAiKey: Boolean,
    onSaveOpenAiKey: (String) -> Unit,
    chatSession: ChatSession?,
    chatSessions: List<ChatSession>,
    chatThinking: Boolean,
    chatError: String?,
    speaking: Boolean,
    onStopSpeech: () -> Unit,
    typewriter: Boolean,
    onSetTypewriter: (Boolean) -> Unit,
    liveEnabled: Boolean,
    onSetLiveEnabled: (Boolean) -> Unit,
    liveActive: Boolean,
    liveStatus: String?,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onSendChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenChat: (ChatSession) -> Unit,
    onDeleteChat: (ChatSession) -> Unit,
    onSpeakAgain: (String) -> Unit,
    onContinueFrom: (Int) -> Unit,
    voiceMode: VoiceMode,
    onSetVoiceMode: (VoiceMode) -> Unit,
    openAiVoice: String,
    onSetOpenAiVoice: (String) -> Unit,
    voiceSpeed: VoiceSpeed,
    onSetVoiceSpeed: (VoiceSpeed) -> Unit,
    resumeEntry: ResumeEntry?,
    onResumeEntry: (ResumeEntry) -> Unit,
    onDismissResume: () -> Unit,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    // Guardado como Saveable (aba + item aberto): a TV pode destruir esta Activity enquanto o
    // player está em cena, e sem isso o "voltar" recomeçaria o app do zero.
    var sectionName by rememberSaveable { mutableStateOf(Section.FILMES.name) }
    var openMovieId by rememberSaveable { mutableStateOf<String?>(null) }
    var openSeriesId by rememberSaveable { mutableStateOf<String?>(null) }
    var configurandoChave by rememberSaveable { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }

    val section = remember(sectionName) { runCatching { Section.valueOf(sectionName) }.getOrDefault(Section.FILMES) }
    val movie = openMovieId?.let { id -> home.catalog.movies.firstOrNull { it.id == id } }
    val series = openSeriesId?.let { id -> home.catalog.series.firstOrNull { it.id == id } }
    // O detalhe substitui o conteúdo (em vez de sobrepor): mantê-lo por cima deixava a
    // lista de trás ainda focável, e o D-pad/Enter continuavam agindo nela.
    when {
        configurandoChave -> KeySetupScreen(
            onKeyReceived = onSaveOpenAiKey,
            onClose = { configurandoChave = false },
        )
        recommendations.isNotEmpty() -> RecommendationsScreen(
            recommendations,
            onOpen = { openMovieId = it.id; onCloseRecommendations() },
            onClose = onCloseRecommendations,
        )
        movie != null -> {
            LaunchedEffect(movie.id) { onLoadMoviePlot(movie) }
            MovieDetail(
                video = movie,
                plot = moviePlot,
                isFavorite = movieKey(movie) in home.userData.favorites,
                isWatched = movie.id in home.userData.watched,
                resumeMs = resumeMs,
                onPlay = onPlay,
                onRecordWatched = onRecordWatched,
                onRemoveWatched = { onRemoveWatched(movie) },
                onToggleFavorite = { onToggleFavorite(movieKey(movie)) },
                onClose = { openMovieId = null; onClearMoviePlot() },
                related = remember(movie.id, home.catalog) {
                    Recommendations.related(home.catalog, movie, home.userData.watched.toSet())
                },
                onOpenRelated = { openMovieId = it.id },
            )
        }
        series != null -> SeriesDetailScreen(
            series = series,
            state = seriesDetail,
            isFavorite = seriesKey(series) in home.userData.favorites,
            resumeOf = resumeOf,
            watchedOf = watchedOf,
            onLoad = { onLoadSeriesDetail(series) },
            onPlayEpisode = { episode, reiniciar, proximos ->
                onRecordWatched(seriesKey(series))
                onPlayQueue(
                    episodeTitle(series, episode),
                    episode.streamUrl,
                    reiniciar,
                    series.coverUrl,
                    proximos.map { Triple(episodeTitle(series, it), it.streamUrl, "T${it.season} E${it.episode}") },
                )
            },
            onToggleFavorite = { onToggleFavorite(seriesKey(series)) },
            onClose = { openSeriesId = null; onClearSeriesDetail() },
            related = remember(series.id, home.catalog) {
                Recommendations.relatedSeries(home.catalog, series, home.userData.watched.toSet())
            },
            onOpenRelated = { openSeriesId = it.id },
        )
        else -> Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .padding(top = if (section == Section.FILMES) 0.dp else TopBarHeight)
                    .focusRequester(contentFocus)
                    .focusGroup(),
            ) {
                when (section) {
                    Section.FILMES -> MoviesSection(home.catalog, home.userData, home.featured, featuredPlot, sortOrder) { openMovieId = it.id }
                    Section.SERIES -> SeriesSection(home.catalog, home.userData, home.featuredSeries, sortOrder) { openSeriesId = it.id }
                    Section.CANAIS -> ChannelsSection(home.catalog) { onPlay(it.name, it.streamUrl, false, it.logoUrl) }
                    Section.CATEGORIAS -> CategoriesSection(home.catalog, sortOrder, { openMovieId = it.id }, { openSeriesId = it.id }) { onPlay(it.name, it.streamUrl, false, it.logoUrl) }
                    Section.PESQUISA -> SearchSection(home.catalog, { openMovieId = it.id }, { openSeriesId = it.id }) { onPlay(it.name, it.streamUrl, false, it.logoUrl) }
                    Section.IA -> ChatScreen(
                        catalog = home.catalog,
                        session = chatSession,
                        sessions = chatSessions,
                        thinking = chatThinking,
                        error = chatError,
                        speaking = speaking,
                        onStopSpeech = onStopSpeech,
                        typewriter = typewriter,
                        liveEnabled = liveEnabled,
                        liveActive = liveActive,
                        liveStatus = liveStatus,
                        onStartLive = onStartLive,
                        onStopLive = onStopLive,
                        hasKey = hasOpenAiKey,
                        onConfigureKey = { configurandoChave = true },
                        onSend = onSendChat,
                        onNewChat = onNewChat,
                        onOpenChat = onOpenChat,
                        onDeleteChat = onDeleteChat,
                        onSpeakAgain = onSpeakAgain,
                        onContinueFrom = onContinueFrom,
                        onOpenMovie = { openMovieId = it.id },
                        onOpenSeries = { openSeriesId = it.id },
                    )
                    Section.DEFINICOES -> SettingsSection(
                        updateState,
                        onCheckUpdate,
                        onDownloadUpdate,
                        hasOpenAiKey,
                        { configurandoChave = true },
                        cacheTtl,
                        onSetCacheTtl,
                        sortOrder,
                        onSetSortOrder,
                        onClearCache,
                        onRefresh,
                        voiceMode,
                        onSetVoiceMode,
                        openAiVoice,
                        onSetOpenAiVoice,
                        voiceSpeed,
                        onSetVoiceSpeed,
                        typewriter,
                        onSetTypewriter,
                        liveEnabled,
                        onSetLiveEnabled,
                    )
                    Section.PERFIL -> ProfileSection(account, onLogout)
                }
            }
            TopBar(section, contentFocus) { sectionName = it.name; openMovieId = null; openSeriesId = null }
            if (updateState is UpdateState.Idle) {
                ResumeBanner(
                    resumeEntry,
                    onResume = onResumeEntry,
                    onDismiss = onDismissResume,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp),
                )
            }
            UpdateBanner(
                updateState,
                onUpdate = onDownloadUpdate,
                onDismiss = onDismissUpdate,
                modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp),
            )
        }
    }
}

private fun episodeTitle(series: Series, episode: Episode) = "${series.title} — T${episode.season} E${episode.episode}"

@Composable
private fun TopBar(selected: Section, contentFocus: FocusRequester, onSelect: (Section) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            // Do menu, ↓ entra no conteúdo da seção.
            .onPreviewKeyEvent { event ->
                val descendo = event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                if (descendo) runCatching { contentFocus.requestFocus() }.isSuccess else false
            }
            .background(Brush.verticalGradient(listOf(Color(0xE6050307), Color(0x00050307))))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo), contentDescription = "Cinemora", modifier = Modifier.size(40.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Text("CINEMORA", color = Mist, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(26.dp))
        Row(
            Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0x59000000)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MainTabs.forEach { tab -> NavTab(tab.label, selected == tab) { onSelect(tab) } }
        }
        Spacer(Modifier.weight(1f))
        TopIcon(Section.IA.icon, "IA", selected == Section.IA) { onSelect(Section.IA) }
        Spacer(Modifier.width(8.dp))
        TopIcon(Section.PESQUISA.icon, "Pesquisa", selected == Section.PESQUISA) { onSelect(Section.PESQUISA) }
        Spacer(Modifier.width(8.dp))
        TopIcon(Section.DEFINICOES.icon, "Definições", selected == Section.DEFINICOES) { onSelect(Section.DEFINICOES) }
        Spacer(Modifier.width(8.dp))
        TopIcon(Section.PERFIL.icon, "Perfil", selected == Section.PERFIL) { onSelect(Section.PERFIL) }
    }
}

@Composable
private fun NavTab(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Coral else Color.Transparent)
            .then(if (focused && !selected) Modifier.border(2.dp, Coral, RoundedCornerShape(18.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else if (focused) Mist else Muted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun TopIcon(icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val active = focused || selected
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) Coral else Color(0x40000000))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) Color.White else Mist, modifier = Modifier.size(20.dp))
    }
}
