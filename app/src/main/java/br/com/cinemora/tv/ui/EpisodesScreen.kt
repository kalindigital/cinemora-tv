package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.data.ImageUrls
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season
import br.com.cinemora.tv.model.Series
import coil.compose.AsyncImage

/**
 * Temporadas de um lado, episódios do outro. A sinopse e a duração de cada episódio vêm do
 * provedor; quando ele não manda (acontece em alguns painéis), fica só o nome e a miniatura.
 */
@Composable
internal fun EpisodesScreen(
    series: Series,
    seasons: List<Season>,
    arte: String?,
    progressoDe: (String) -> Float?,
    watchedOf: (String) -> Boolean,
    onPlayEpisode: (Episode) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var temporada by remember(seasons) { mutableStateOf(seasons.firstOrNull()?.number) }
    val atual = seasons.firstOrNull { it.number == temporada } ?: seasons.firstOrNull()
    val primeira = remember { FocusRequester() }
    LaunchedEffect(seasons) { runCatching { primeira.requestFocus() } }
    DetalheFundo(ImageUrls.backdrop(arte) ?: ImageUrls.detail(series.coverUrl), series.title) {
        Row(Modifier.fillMaxSize().padding(start = 44.dp, top = 40.dp, end = 36.dp, bottom = 24.dp)) {
            Column(Modifier.width(300.dp).fillMaxHeight().focusGroup()) {
                Text(
                    series.title, color = Mist, fontSize = 26.sp, lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    linhaDeDados(series.year, "${seasons.size} temporada${if (seasons.size > 1) "s" else ""}"),
                    color = Color(0xFF9AA7B4), fontSize = 13.sp,
                )
                Spacer(Modifier.height(18.dp))
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(seasons) { index, season ->
                        val vistos = season.episodes.count { watchedOf(it.streamUrl) }
                        LinhaTemporada(
                            numero = season.number,
                            episodios = season.episodes.size,
                            vistos = vistos,
                            selecionada = season.number == temporada,
                            modifier = if (index == 0) Modifier.focusRequester(primeira) else Modifier,
                        ) { temporada = season.number }
                    }
                }
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    "Temporada ${atual?.number ?: 1}", color = Mist, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.focusGroup(),
                    contentPadding = PaddingValues(bottom = 40.dp),
                ) {
                    items(atual?.episodes.orEmpty(), key = { it.id }) { episode ->
                        CartaoEpisodio(
                            episode = episode,
                            progresso = progressoDe(episode.streamUrl),
                            visto = watchedOf(episode.streamUrl),
                        ) { onPlayEpisode(episode) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaTemporada(
    numero: Int,
    episodios: Int,
    vistos: Int,
    selecionada: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Color(0x33FFFFFF) else if (selecionada) Color(0x1AFFFFFF) else Color.Transparent)
            // Andar pelas temporadas já troca a lista de episódios, sem precisar confirmar.
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onSelect() }
            .clickable { onSelect() }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Temporada $numero", color = if (focused || selecionada) Mist else Color(0xFFB9C2CC),
            fontSize = 15.sp, fontWeight = if (selecionada) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (vistos > 0) "$vistos/$episodios" else "$episodios episódios",
            color = Muted, fontSize = 12.sp,
        )
    }
}

@Composable
private fun CartaoEpisodio(
    episode: Episode,
    progresso: Float?,
    visto: Boolean,
    onPlay: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0x26FFFFFF) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onPlay() }
            .focusable()
            .padding(10.dp),
    ) {
        Box(
            Modifier.width(168.dp).height(94.dp).clip(RoundedCornerShape(8.dp)).background(Panel),
        ) {
            AsyncImage(
                model = ImageUrls.card(episode.thumbUrl), contentDescription = episode.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            if (visto) {
                Icon(
                    Icons.Rounded.Check, contentDescription = "Assistido", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                )
            }
            if (progresso != null && progresso > 0f) {
                BarraProgresso(progresso, Modifier.align(Alignment.BottomStart))
            }
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(
                episode.title, color = Mist, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            episode.plot?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(5.dp))
                Text(
                    it, color = Color(0xFFB9C2CC), fontSize = 13.sp, lineHeight = 18.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                linhaDeDados("T${episode.season}:E${episode.episode}", episode.duration),
                color = Muted, fontSize = 12.sp,
            )
        }
    }
}
