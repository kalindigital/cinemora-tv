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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
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
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import coil.compose.AsyncImage

@Composable
internal fun MovieDetail(
    video: Video,
    plot: String?,
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
) {
    BackHandler(onBack = onClose)
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(video.id) { runCatching { playFocus.requestFocus() } }
    LazyColumn(Modifier.fillMaxSize().background(Ink), contentPadding = PaddingValues(bottom = 40.dp)) {
      item {
        Row(Modifier.fillMaxWidth().padding(40.dp)) {
            AsyncImage(
                model = ImageUrls.detail(video.coverUrl), contentDescription = video.title, contentScale = ContentScale.Crop,
                modifier = Modifier.width(240.dp).height(360.dp).clip(RoundedCornerShape(10.dp)).background(Panel),
            )
            Column(Modifier.padding(start = 32.dp)) {
                Text(video.title, color = Mist, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
                val meta = listOfNotNull(video.year, video.rating?.let { "★ $it" }).joinToString("   ·   ")
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(meta, color = Signal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                val synopsis = when {
                    plot == null -> "Carregando sinopse…"
                    plot.isBlank() -> "Sem sinopse disponível."
                    else -> plot
                }
                Text(synopsis, color = Color(0xFFC9C2CB), fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(26.dp))
                val emAndamento = resumeMs > 0
                if (emAndamento) {
                    Text(
                        "Você parou em ${formatPosition(resumeMs)}.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        if (emAndamento) "Continuar" else "Assistir",
                        modifier = Modifier.focusRequester(playFocus),
                        icon = Icons.Rounded.PlayArrow,
                    ) { onRecordWatched(video.id); onPlay(video.title, video.streamUrl, false, video.coverUrl) }
                    if (emAndamento) {
                        ActionButton("Reiniciar") { onRecordWatched(video.id); onPlay(video.title, video.streamUrl, true, video.coverUrl) }
                    }
                    FavoriteButton(isFavorite, onToggleFavorite)
                    ActionButton("Voltar", onClick = onClose)
                }
                // Basta estar no histórico: nem todo assistido tem posição salva.
                if (isWatched) {
                    Spacer(Modifier.height(12.dp))
                    ActionButton("Remover dos assistidos", onClick = onRemoveWatched)
                }
            }
        }
      }
      if (related.isNotEmpty()) item { PosterRow("Relacionados", related, onOpenRelated) }
    }
}

@Composable
internal fun SeriesDetailScreen(
    series: Series,
    state: DetailState,
    isFavorite: Boolean,
    resumeOf: (String) -> Long,
    watchedOf: (String) -> Boolean,
    onLoad: () -> Unit,
    onPlayEpisode: (Episode, Boolean, List<Episode>) -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    related: List<Series>,
    onOpenRelated: (Series) -> Unit,
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(series.id) { onLoad() }
    var escolhendo by remember { mutableStateOf<Episode?>(null) }
    val seasons = (state as? DetailState.Loaded)?.detail?.seasons.orEmpty()

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

    // Só a página rola: temporadas e episódios são colunas comuns. Listas roláveis
    // aninhadas brigavam com a rolagem da página e faziam o foco pular sozinho.
    LazyColumn(Modifier.fillMaxSize().background(Ink), contentPadding = PaddingValues(bottom = 32.dp)) {
      item {
        Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 28.dp, end = 40.dp)) {
            AsyncImage(
                model = ImageUrls.detail(series.coverUrl), contentDescription = series.title, contentScale = ContentScale.Crop,
                modifier = Modifier.width(126.dp).height(186.dp).clip(RoundedCornerShape(10.dp)).background(Panel),
            )
            Column(Modifier.padding(start = 26.dp)) {
                Text(series.title, color = Mist, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                val meta = listOfNotNull(series.year, series.rating?.let { "★ $it" }).joinToString("   ·   ")
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(meta, color = Signal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(series.synopsis ?: "Sem sinopse disponível.", color = Color(0xFFC9C2CB), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3)
                Spacer(Modifier.height(16.dp))
                val continuarEm = EpisodeQueue.resumeTarget(seasons, watchedOf, resumeOf)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (continuarEm != null) {
                        ActionButton(
                            "Continuar T${continuarEm.season} E${continuarEm.episode}",
                            icon = Icons.Rounded.PlayArrow,
                        ) {
                            onPlayEpisode(continuarEm, false, EpisodeQueue.upcoming(seasons, continuarEm.id))
                        }
                    }
                    FavoriteButton(isFavorite, onToggleFavorite)
                    ActionButton("Voltar", onClick = onClose)
                }
            }
        }
      }
      item {
          when (state) {
            DetailState.Loading -> Notice("Carregando episódios…")
            is DetailState.Failed -> Notice(state.message)
            DetailState.Idle -> Unit
            is DetailState.Loaded -> SeasonsAndEpisodes(state.detail.seasons, resumeOf, watchedOf) { episode ->
                // Episódio já começado pergunta; os demais tocam direto.
                if (resumeOf(episode.streamUrl) > 0) {
                    escolhendo = episode
                } else {
                    onPlayEpisode(episode, false, EpisodeQueue.upcoming(seasons, episode.id))
                }
            }
          }
      }
      if (related.isNotEmpty()) item { SeriesRow("Séries relacionadas", related, onOpenRelated) }
    }
}

@Composable
private fun SeasonsAndEpisodes(
    seasons: List<Season>,
    resumeOf: (String) -> Long,
    watchedOf: (String) -> Boolean,
    onPlayEpisode: (Episode) -> Unit,
) {
    var selectedNumber by remember(seasons) { mutableStateOf(seasons.firstOrNull()?.number) }
    val current = seasons.firstOrNull { it.number == selectedNumber } ?: seasons.firstOrNull()
    val firstSeason = remember { FocusRequester() }
    LaunchedEffect(seasons) { runCatching { firstSeason.requestFocus() } }
    Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 10.dp, end = 40.dp)) {
        Column(Modifier.width(210.dp).padding(end = 10.dp)) {
            SectionLabel("TEMPORADAS")
            seasons.forEachIndexed { index, season ->
                val vistos = season.episodes.count { watchedOf(it.streamUrl) }
                SeasonRow(
                    "Temporada ${season.number}",
                    if (vistos > 0) "$vistos/${season.episodes.size} vistos" else null,
                    season.number == selectedNumber,
                    if (index == 0) Modifier.focusRequester(firstSeason) else Modifier,
                ) { selectedNumber = season.number }
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            SectionLabel("EPISÓDIOS")
            current?.episodes.orEmpty().forEach { episode ->
                EpisodeRow(
                    episode,
                    iniciado = resumeOf(episode.streamUrl) > 0,
                    visto = watchedOf(episode.streamUrl),
                ) { onPlayEpisode(episode) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun SeasonRow(
    label: String,
    detalhe: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Coral else if (focused) Color(0xFF241019) else Panel)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onSelect() }
            .clickable { onSelect() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Text(label, color = if (selected) Color.White else Mist, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, fontSize = 15.sp)
            if (detalhe != null) {
                Text(detalhe, color = if (selected) Color.White else Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, iniciado: Boolean, visto: Boolean, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val corTitulo = if (visto && !focused) Muted else Mist
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0xFF241019) else Panel)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onPlay() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text("E${episode.episode}", color = Coral, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
        Text(episode.title, color = Mist, fontSize = 15.sp, maxLines = 1)
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
            Text("Você parou em ${'$'}{formatPosition(posicao)}.", color = Muted, fontSize = 14.sp)
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
