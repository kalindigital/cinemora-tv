package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.DetailState
import br.com.cinemora.tv.data.EpisodeQueue
import br.com.cinemora.tv.data.ImageUrls
import br.com.cinemora.tv.data.MovieExtra
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
internal fun MovieDetail(
    video: Video,
    plot: String?,
    extra: MovieExtra?,
    isFavorite: Boolean,
    isWatched: Boolean,
    resumeMs: Long,
    onPlay: (String, String, Boolean, String?) -> Unit,
    onRecordWatched: (String) -> Unit,
    onRemoveWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    related: List<Video>,
    onOpenRelated: (Video) -> Unit,
    onAskVerdict: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(video.id) { runCatching { playFocus.requestFocus() } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val emAndamento = resumeMs > 0
    DetalheFundo(ImageUrls.backdrop(extra?.backdrop) ?: ImageUrls.detail(video.coverUrl), video.title) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(0.55f)
                        // Voltando dos relacionados, o Compose traz só o botão focado para a tela:
                        // aqui a página volta ao topo.
                        .onFocusChanged { if (it.hasFocus) scope.launch { listState.animateScrollToItem(0) } }
                        .padding(start = 44.dp, top = 44.dp, end = 20.dp),
                ) {
                    Text(
                        video.title, color = Mist, fontSize = 34.sp, lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold, maxLines = 3,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        linhaDeDados(video.year, extra?.genre, extra?.duration, video.rating?.let { "★ $it" }),
                        color = Color(0xFF9AA7B4), fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    val synopsis = when {
                        plot == null -> "Carregando sinopse…"
                        plot.isBlank() -> "Sem sinopse disponível."
                        else -> plot
                    }
                    Text(synopsis, color = Color(0xFFC4CED8), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4)
                    extra?.cast?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Elenco: $it", color = Muted, fontSize = 12.sp, maxLines = 2)
                    }
                    extra?.director?.let {
                        Spacer(Modifier.height(3.dp))
                        Text("Direção: $it", color = Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    if (emAndamento) {
                        Spacer(Modifier.height(10.dp))
                        Text("Você parou em ${formatPosition(resumeMs)}.", color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    BotaoPrincipal(
                        if (emAndamento) "Continuar de ${formatPosition(resumeMs)}" else "Assistir",
                        Icons.Rounded.PlayArrow,
                        Modifier.focusRequester(playFocus),
                    ) { onRecordWatched(video.id); onPlay(video.title, video.streamUrl, false, video.coverUrl) }
                    Spacer(Modifier.height(6.dp))
                    if (emAndamento) {
                        OpcaoMenu("Assistir do começo", Icons.Rounded.Refresh) {
                            onRecordWatched(video.id); onPlay(video.title, video.streamUrl, true, video.coverUrl)
                        }
                    }
                    OpcaoMenu(
                        if (isFavorite) "Nos favoritos" else "Favoritar",
                        if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        onClick = onToggleFavorite,
                    )
                    OpcaoMenu("Vale a pena?", Icons.Rounded.AutoAwesome, onClick = onAskVerdict)
                    if (isWatched) {
                        // Basta estar no histórico: nem todo assistido tem posição salva.
                        OpcaoMenu("Remover dos assistidos", Icons.Rounded.Delete, onClick = onRemoveWatched)
                    }
                    OpcaoMenu("Voltar", Icons.Rounded.ArrowBack, onClick = onClose)
                }
            }
            if (related.isNotEmpty()) {
                item { PosterRow("Títulos semelhantes", related, onOpenRelated, visual = CardVisual.COMPACTO) }
            }
        }
    }
}

@Composable
internal fun SeriesDetailScreen(
    series: Series,
    state: DetailState,
    isFavorite: Boolean,
    resumeOf: (String) -> Long,
    progressoDe: (String) -> Float?,
    watchedOf: (String) -> Boolean,
    onLoad: () -> Unit,
    onPlayEpisode: (Episode, Boolean, List<Episode>) -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    related: List<Series>,
    onOpenRelated: (Series) -> Unit,
    onAskRecap: (Int, Int) -> Unit,
) {
    LaunchedEffect(series.id) { onLoad() }
    var escolhendo by remember { mutableStateOf<Episode?>(null) }
    var vendoEpisodios by remember(series.id) { mutableStateOf(false) }
    val seasons = (state as? DetailState.Loaded)?.detail?.seasons.orEmpty()
    val extra = (state as? DetailState.Loaded)?.detail?.extra

    escolhendo?.let { episode ->
        val proximos = EpisodeQueue.upcoming(seasons, episode.id)
        ResumeChoice(
            titulo = "T${episode.season} E${episode.episode} · ${episode.title}",
            posicao = resumeOf(episode.streamUrl),
            onContinuar = { escolhendo = null; onPlayEpisode(episode, false, proximos) },
            onReiniciar = { escolhendo = null; onPlayEpisode(episode, true, proximos) },
            onCancelar = { escolhendo = null },
        )
        return
    }

    if (vendoEpisodios && seasons.isNotEmpty()) {
        EpisodesScreen(
            series = series,
            seasons = seasons,
            arte = extra?.backdrop,
            progressoDe = progressoDe,
            watchedOf = watchedOf,
            onPlayEpisode = { episode ->
                // Episódio já começado pergunta; os demais tocam direto.
                if (resumeOf(episode.streamUrl) > 0) {
                    escolhendo = episode
                } else {
                    onPlayEpisode(episode, false, EpisodeQueue.upcoming(seasons, episode.id))
                }
            },
            onClose = { vendoEpisodios = false },
        )
        return
    }

    BackHandler(onBack = onClose)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(series.id) { runCatching { playFocus.requestFocus() } }
    val continuarEm = EpisodeQueue.resumeTarget(seasons, watchedOf, resumeOf)
    DetalheFundo(ImageUrls.backdrop(extra?.backdrop) ?: ImageUrls.detail(series.coverUrl), series.title) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(0.55f)
                        .onFocusChanged { if (it.hasFocus) scope.launch { listState.animateScrollToItem(0) } }
                        .padding(start = 44.dp, top = 44.dp, end = 20.dp),
                ) {
                    Text(
                        series.title, color = Mist, fontSize = 34.sp, lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold, maxLines = 3,
                    )
                    Spacer(Modifier.height(10.dp))
                    val temporadas = seasons.size.takeIf { it > 0 }
                        ?.let { "$it temporada${if (it > 1) "s" else ""}" }
                    Text(
                        linhaDeDados(series.year, extra?.genre, temporadas, series.rating?.let { "★ $it" }),
                        color = Color(0xFF9AA7B4), fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        extra?.plot ?: series.synopsis ?: "Sem sinopse disponível.",
                        color = Color(0xFFC4CED8), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4,
                    )
                    extra?.cast?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Elenco: $it", color = Muted, fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(18.dp))
                    when (state) {
                        DetailState.Loading -> Notice("Carregando episódios…")
                        is DetailState.Failed -> Notice(state.message)
                        else -> Unit
                    }
                    if (continuarEm != null) {
                        val rotulo = if (resumeOf(continuarEm.streamUrl) > 0) {
                            "Continuar T${continuarEm.season}:E${continuarEm.episode}"
                        } else {
                            "Assistir T${continuarEm.season}:E${continuarEm.episode}"
                        }
                        BotaoPrincipal(rotulo, Icons.Rounded.PlayArrow, Modifier.focusRequester(playFocus)) {
                            onPlayEpisode(continuarEm, false, EpisodeQueue.upcoming(seasons, continuarEm.id))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (seasons.isNotEmpty()) {
                        OpcaoMenu("Ver episódios", Icons.Rounded.Menu) { vendoEpisodios = true }
                    }
                    OpcaoMenu(
                        if (isFavorite) "Nos favoritos" else "Favoritar",
                        if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        onClick = onToggleFavorite,
                    )
                    if (continuarEm != null && (continuarEm.episode > 1 || continuarEm.season > 1)) {
                        OpcaoMenu("Onde eu parei?", Icons.Rounded.AutoAwesome) {
                            onAskRecap(continuarEm.season, continuarEm.episode)
                        }
                    }
                    OpcaoMenu("Voltar", Icons.Rounded.ArrowBack, onClick = onClose)
                }
            }
            if (related.isNotEmpty()) {
                item { SeriesRow("Títulos semelhantes", related, onOpenRelated, visual = CardVisual.COMPACTO) }
            }
        }
    }
}

/** Escolha ao abrir um episódio já começado. */
@Composable
private fun ResumeChoice(
    titulo: String,
    posicao: Long,
    onContinuar: () -> Unit,
    onReiniciar: () -> Unit,
    onCancelar: () -> Unit,
) {
    BackHandler(onBack = onCancelar)
    val continuarFocus = remember { FocusRequester() }
    LaunchedEffect(titulo) { runCatching { continuarFocus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Você parou em ${formatPosition(posicao)}.", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("Continuar", modifier = Modifier.focusRequester(continuarFocus), icon = Icons.Rounded.PlayArrow, onClick = onContinuar)
                ActionButton("Reiniciar", onClick = onReiniciar)
                ActionButton("Cancelar", onClick = onCancelar)
            }
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) = ActionButton(
    if (isFavorite) "Nos favoritos" else "Favoritar",
    icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
    onClick = onToggle,
)

/** 1h05 / 12min — só para indicar onde o filme parou. */
private fun formatPosition(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "%dh%02d".format(hours, minutes) else "${minutes}min"
}

@Composable
private fun Notice(text: String) = Text(text, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 40.dp, top = 20.dp))
