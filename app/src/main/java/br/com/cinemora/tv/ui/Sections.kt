package br.com.cinemora.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.data.CatalogSorter
import br.com.cinemora.tv.data.CategoryNames
import br.com.cinemora.tv.data.MovieExtra
import br.com.cinemora.tv.data.ImageUrls
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.data.UserData
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Channel
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

internal fun movieKey(video: Video) = "m:${video.id}"
internal fun seriesKey(series: Series) = "s:${series.id}"

private fun subtitle(year: String?, rating: String?): String? =
    listOfNotNull(year, rating).joinToString("  ·  ").ifBlank { null }

@Composable
internal fun MoviesSection(
    catalog: Catalog,
    userData: UserData,
    featured: Video?,
    featuredPlot: String?,
    sortOrder: SortOrder,
    novidades: List<Video>,
    listState: LazyListState,
    focado: Video?,
    focadoExtra: MovieExtra?,
    arte: Map<String, String>,
    onFocusMovie: (Video?) -> Unit,
    onNeedArt: (Video) -> Unit,
    onOpenMovie: (Video) -> Unit,
) {
    val byId = remember(catalog) { catalog.movies.associateBy { it.id } }
    val byCategory = remember(catalog) { catalog.movies.groupBy { it.categoryId } }
    val continuar = userData.watched.mapNotNull { byId[it] }
    val favoritos = catalog.movies.filter { movieKey(it) in userData.favorites }
    val rows = remember(catalog, userData, sortOrder, novidades) {
        buildList {
            // "Continuar assistindo" fica em ordem de exibição, não na ordenação escolhida.
            if (continuar.isNotEmpty()) add("Continuar assistindo" to continuar)
            if (novidades.isNotEmpty()) add("Novidades no catálogo" to novidades)
            if (favoritos.isNotEmpty()) add("Favoritos" to CatalogSorter.movies(favoritos, sortOrder))
            catalog.movieCategories.forEach { category ->
                byCategory[category.id]?.takeIf { it.isNotEmpty() }
                    ?.let { add(CategoryNames.short(category.name) to CatalogSorter.movies(it, sortOrder)) }
            }
        }
    }
    // O destaque mostra o filme em foco; sem foco (ao entrar na aba), a seleção do dia.
    val alvo = focado ?: featured
    val sinopse = if (focado != null) focadoExtra?.plot ?: focado.synopsis else featuredPlot ?: featured?.synopsis
    // Sai da aba de filmes sem deixar o destaque preso no último item focado.
    DisposableEffect(Unit) { onDispose { onFocusMovie(null) } }
    val scope = rememberCoroutineScope()
    // Rolar por fileira inteira: sem isto a fileira de cima ficava cortada ao meio.
    var fileiraAtiva by remember { mutableStateOf(-1) }
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageUrls.backdrop(focadoExtra?.backdrop) ?: ImageUrls.detail(alvo?.coverUrl),
            contentDescription = alvo?.title, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Ink),
        )
        // A arte ocupa a tela inteira; os degradês devolvem contraste ao texto e às fileiras.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x99050307), 0.28f to Color(0x33050307),
                    0.50f to Color(0xCC070A0D), 0.66f to Color(0xF2070A0D), 1f to Ink,
                ),
            ),
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Ink, Color(0x00070A0D)))))
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.44f)) {
                InfoDestaque(
                    eyebrow = if (focado == null) "SELEÇÃO DO DIA" else focadoExtra?.genre?.uppercase() ?: "EM DESTAQUE",
                    title = alvo?.title,
                    meta = metaDoFilme(alvo, focadoExtra),
                    synopsis = sinopse,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                // Folga no fim para a última fileira também conseguir subir até o topo.
                contentPadding = PaddingValues(bottom = 220.dp),
            ) {
                itemsIndexed(rows) { index, (title, videos) ->
                    PosterRow(
                        title, videos, onOpenMovie, visual = CardVisual.LARGO, arte = arte,
                        onFocus = { video ->
                            onFocusMovie(video)
                            if (index != fileiraAtiva) {
                                fileiraAtiva = index
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        onNeedArt = onNeedArt,
                    )
                }
            }
        }
    }
}

/** Linha de dados do destaque: ano · duração · nota. */
private fun metaDoFilme(video: Video?, extra: MovieExtra?): String? {
    if (video == null) return null
    return listOfNotNull(video.year, extra?.duration, video.rating?.let { "★ $it" })
        .joinToString("   ·   ").ifBlank { null }
}

@Composable
internal fun SeriesSection(
    catalog: Catalog,
    userData: UserData,
    featured: Series?,
    sortOrder: SortOrder,
    listState: LazyListState,
    onOpenSeries: (Series) -> Unit,
) {
    val byCategory = remember(catalog) { catalog.series.groupBy { it.categoryId } }
    val byId = remember(catalog) { catalog.series.associateBy { seriesKey(it) } }
    val continuar = userData.watched.mapNotNull { byId[it] }
    val favoritos = catalog.series.filter { seriesKey(it) in userData.favorites }
    val rows = remember(catalog, userData, sortOrder) {
        buildList {
            if (continuar.isNotEmpty()) add("Continuar assistindo" to continuar)
            if (favoritos.isNotEmpty()) add("Favoritos" to CatalogSorter.series(favoritos, sortOrder))
            catalog.seriesCategories.forEach { category ->
                byCategory[category.id]?.takeIf { it.isNotEmpty() }
                    ?.let { add(CategoryNames.short(category.name) to CatalogSorter.series(it, sortOrder)) }
            }
        }
    }
    val firstRow = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            Hero(
                title = featured?.title,
                synopsis = featured?.synopsis,
                imageUrl = ImageUrls.detail(featured?.coverUrl),
                actionLabel = "Ver episódios",
                onAction = featured?.let { { onOpenSeries(it) } },
                modifier = Modifier.fillParentMaxHeight(0.68f)
                    .onFocusChanged { if (it.hasFocus) scope.launch { listState.animateScrollToItem(0) } },
                hasRows = rows.isNotEmpty(),
                downTarget = firstRow,
            )
        }
        itemsIndexed(rows) { index, (title, series) ->
            SeriesRow(title, series, onOpenSeries, if (index == 0) Modifier.focusRequester(firstRow) else Modifier)
        }
        if (catalog.series.isEmpty()) item { EmptyNotice("Seu provedor não retornou séries.") }
    }
}

@Composable
internal fun ChannelsSection(catalog: Catalog, onPlayChannel: (Channel) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)) {
        items(catalog.liveCategories) { category ->
            val channels = catalog.channels.filter { it.categoryId == category.id }
            if (channels.isNotEmpty()) ChannelRow(CategoryNames.short(category.name), channels, onPlayChannel)
        }
        if (catalog.channels.isEmpty()) item { EmptyNotice("Seu provedor não retornou canais ao vivo.") }
    }
}

@Composable
internal fun CategoriesSection(
    catalog: Catalog,
    sortOrder: SortOrder,
    listState: LazyListState,
    onOpenMovie: (Video) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onPlayChannel: (Channel) -> Unit,
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)) {
        items(catalog.movieCategories) { category ->
            val videos = catalog.movies.filter { it.categoryId == category.id }
            if (videos.isNotEmpty()) {
                PosterRow(category.name, CatalogSorter.movies(videos, sortOrder), onOpenMovie, visual = CardVisual.LIMPO)
            }
        }
        items(catalog.seriesCategories) { category ->
            val series = catalog.series.filter { it.categoryId == category.id }
            if (series.isNotEmpty()) {
                SeriesRow(category.name, CatalogSorter.series(series, sortOrder), onOpenSeries, visual = CardVisual.LIMPO)
            }
        }
        items(catalog.liveCategories) { category ->
            val channels = catalog.channels.filter { it.categoryId == category.id }
            if (channels.isNotEmpty()) ChannelRow(category.name, channels, onPlayChannel)
        }
    }
}

@Composable
internal fun SearchSection(catalog: Catalog, onOpenMovie: (Video) -> Unit, onOpenSeries: (Series) -> Unit, onPlayChannel: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    // Ao entrar, o campo já recebe o foco: quem abre a busca quer digitar.
    val campoFoco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { campoFoco.requestFocus() } }
    val q = query.trim().lowercase()
    val active = q.length >= 2
    val movies = if (active) catalog.movies.filter { it.title.lowercase().contains(q) }.distinctBy { it.title.lowercase() }.take(30) else emptyList()
    val series = if (active) catalog.series.filter { it.title.lowercase().contains(q) }.distinctBy { it.title.lowercase() }.take(30) else emptyList()
    val channels = if (active) catalog.channels.filter { it.name.lowercase().contains(q) }.distinctBy { it.name.lowercase() }.take(30) else emptyList()
    Column {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, label = { Text("Buscar filmes, séries e canais") },
            singleLine = true, colors = fieldColors(),
            modifier = Modifier.padding(start = 32.dp, top = 24.dp, end = 32.dp).fillMaxWidth()
                .focusRequester(campoFoco).dpadFocusNav(focusManager),
        )
        when {
            !active -> EmptyNotice("Digite ao menos 2 letras para buscar.")
            movies.isEmpty() && series.isEmpty() && channels.isEmpty() -> EmptyNotice("Nada encontrado para \"$query\".")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 48.dp)) {
                if (movies.isNotEmpty()) item { PosterRow("Filmes", movies, onOpenMovie) }
                if (series.isNotEmpty()) item { SeriesRow("Séries", series, onOpenSeries) }
                if (channels.isNotEmpty()) item { ChannelRow("Canais", channels, onPlayChannel) }
            }
        }
    }
}

@Composable
internal fun ProfileSection(account: Credentials?, onLogout: () -> Unit) {
    Column(Modifier.padding(start = 40.dp, top = 40.dp, end = 40.dp)) {
        Text("Perfil", color = Mist, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(24.dp))
        InfoLine("Servidor", account?.serverUrl ?: "—")
        Spacer(Modifier.height(12.dp))
        InfoLine("Usuário", account?.username ?: "—")
        Spacer(Modifier.height(28.dp))
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)) {
            Text("Sair / trocar conta", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Mist, fontSize = 18.sp)
    }
}

/** Bloco de textos do destaque: tarja, nome, dados e sinopse. */
@Composable
private fun InfoDestaque(
    eyebrow: String,
    title: String?,
    meta: String?,
    synopsis: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(start = 32.dp, end = 24.dp, bottom = 22.dp).widthIn(max = 620.dp)) {
        Text(
            eyebrow, color = Signal, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title ?: "Seu catálogo está pronto", color = Mist, fontSize = 26.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (meta != null) {
            Spacer(Modifier.height(6.dp))
            Text(meta, color = Color(0xFF9AA7B4), fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            // Com um título em foco e a sinopse ainda a caminho, a linha fica vazia:
            // o convite só cabe quando não há nada selecionado.
            synopsis?.takeIf { it.isNotBlank() }
                ?: if (title == null) "Escolha um título nas fileiras abaixo para começar." else "",
            color = Color(0xFFC4CED8), maxLines = 3, fontSize = 13.sp, lineHeight = 19.sp,
        )
    }
}

@Composable
private fun EmptyNotice(text: String) =
    Text(text, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(32.dp))

@Composable
private fun Hero(
    title: String?,
    synopsis: String?,
    imageUrl: String?,
    actionLabel: String,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
    hasRows: Boolean = false,
    downTarget: FocusRequester? = null,
    eyebrow: String = "SELEÇÃO DO DIA",
    meta: String? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            // Garante a descida do banner para a primeira fileira: a busca automática de
            // foco do Compose não atravessa o hero, então tratamos a tecla diretamente.
            .onPreviewKeyEvent { event ->
                val descendo = event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                if (descendo && hasRows && downTarget != null) {
                    runCatching { downTarget.requestFocus() }.isSuccess
                } else {
                    false
                }
            },
    ) {
        AsyncImage(
            model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Panel),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66050307), Color(0x00050307), Ink))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Ink, Color(0x00070A0D)))))
        // Ancorado na base e sem padding de topo: o padding superior reduzia a altura
        // disponível da coluna e era o que cortava o botão.
        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(start = 32.dp, end = 24.dp, bottom = 28.dp)
                .widthIn(max = 560.dp),
        ) {
            Text(
                eyebrow, color = Signal, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                title ?: "Seu catálogo está pronto", color = Mist, fontSize = 26.sp, lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Spacer(Modifier.height(6.dp))
                Text(meta, color = Color(0xFF9AA7B4), fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                // Com um título em foco e a sinopse ainda a caminho, a linha fica vazia:
                // o convite só cabe quando não há nada selecionado.
                synopsis?.takeIf { it.isNotBlank() }
                    ?: if (title == null) "Escolha um título nas fileiras abaixo para começar." else "",
                color = Color(0xFFC4CED8), maxLines = 3, fontSize = 13.sp, lineHeight = 19.sp,
            )
            if (onAction != null) {
                Spacer(Modifier.height(16.dp))
                var playFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                    border = if (playFocused) BorderStroke(3.dp, Color.White) else null,
                    modifier = Modifier
                        .onFocusChanged { playFocused = it.isFocused }
                        .focusProperties { if (hasRows && downTarget != null) down = downTarget },
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PosterRow(
    title: String,
    videos: List<Video>,
    onOpen: (Video) -> Unit,
    modifier: Modifier = Modifier,
    visual: CardVisual = CardVisual.PADRAO,
    arte: Map<String, String> = emptyMap(),
    onFocus: ((Video) -> Unit)? = null,
    onNeedArt: ((Video) -> Unit)? = null,
) {
    val first = remember { FocusRequester() }
    Column {
        RowTitle(title)
        LazyRow(
            modifier = modifier.focusGroup().focusProperties { onEnter = { first.requestFocus() } },
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                // Enquanto a arte 16:9 não chega, a capa segura o lugar do cartão.
                LaunchedEffect(video.id) { onNeedArt?.invoke(video) }
                PosterCard(
                    video.title,
                    arte[video.id]?.let { ImageUrls.detail(it) } ?: ImageUrls.card(video.coverUrl),
                    subtitle(video.year, video.rating),
                    if (index == 0) Modifier.focusRequester(first) else Modifier,
                    visual = visual,
                    onFocus = onFocus?.let { avisar -> { avisar(video) } },
                ) { onOpen(video) }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SeriesRow(
    title: String,
    series: List<Series>,
    onOpen: (Series) -> Unit,
    modifier: Modifier = Modifier,
    visual: CardVisual = CardVisual.PADRAO,
) {
    val first = remember { FocusRequester() }
    Column {
        RowTitle(title)
        LazyRow(
            modifier = modifier.focusGroup().focusProperties { onEnter = { first.requestFocus() } },
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(series, key = { _, item -> item.id }) { index, item ->
                PosterCard(
                    item.title, ImageUrls.card(item.coverUrl), subtitle(item.year, item.rating),
                    if (index == 0) Modifier.focusRequester(first) else Modifier,
                    visual = visual,
                ) { onOpen(item) }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelRow(title: String, channels: List<Channel>, onPlay: (Channel) -> Unit) {
    val first = remember { FocusRequester() }
    Column {
        RowTitle(title)
        LazyRow(
            modifier = Modifier.focusGroup().focusProperties { onEnter = { first.requestFocus() } },
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                ChannelCard(
                    channel.name, ImageUrls.card(channel.logoUrl),
                    if (index == 0) Modifier.focusRequester(first) else Modifier,
                ) { onPlay(channel) }
            }
        }
    }
}
