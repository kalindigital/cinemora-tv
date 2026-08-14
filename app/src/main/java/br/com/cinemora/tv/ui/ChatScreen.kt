package br.com.cinemora.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.data.ChatMessage
import br.com.cinemora.tv.data.ChatRole
import br.com.cinemora.tv.data.ChatSession
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

/**
 * Conversa com a IA: fala pelo controle ou digita, ela responde em texto (e em voz) e as
 * sugestões que existem no catálogo viram cartões clicáveis.
 */
@Composable
internal fun ChatScreen(
    catalog: Catalog,
    session: ChatSession?,
    sessions: List<ChatSession>,
    thinking: Boolean,
    error: String?,
    hasKey: Boolean,
    onConfigureKey: () -> Unit,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenChat: (ChatSession) -> Unit,
    onOpenMovie: (Video) -> Unit,
    onOpenSeries: (Series) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var ouvindo by remember { mutableStateOf(false) }
    var micJaAberto by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val campoFoco = remember { FocusRequester() }
    val listState = rememberLazyListState()

    if (!hasKey) {
        SemChave(onConfigureKey)
        return
    }

    // Conversa nova abre no microfone; depois dele o foco vai para o campo de texto.
    LaunchedEffect(session?.id) {
        if (session == null && !micJaAberto) {
            micJaAberto = true
            ouvindo = true
        }
    }
    LaunchedEffect(ouvindo, thinking) {
        if (!ouvindo && !thinking) runCatching { campoFoco.requestFocus() }
    }
    LaunchedEffect(session?.messages?.size) {
        val total = session?.messages?.size ?: 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    if (ouvindo) {
        VoiceOverlay(
            onResult = { falado -> ouvindo = false; query = ""; onSend(falado) },
            onDismiss = { ouvindo = false },
        )
        return
    }

    Row(Modifier.fillMaxSize()) {
        ConversasAnteriores(sessions, session, onNewChat, onOpenChat)
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 20.dp, end = 32.dp)) {
            LazyColumn(
                Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (session == null || session.messages.isEmpty()) {
                    item { Boasvindas() }
                }
                items(session?.messages.orEmpty().size) { index ->
                    val message = session!!.messages[index]
                    Mensagem(message, catalog, onOpenMovie, onOpenSeries)
                }
                if (thinking) item { Text("Pensando…", color = Muted, fontSize = 14.sp) }
                if (error != null) item { Text(error, color = Signal, fontSize = 14.sp) }
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Fale ou escreva") },
                    singleLine = true,
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { focusManager.clearFocus(); onSend(query); query = "" },
                    ),
                    modifier = Modifier.weight(1f).focusRequester(campoFoco).dpadFocusNav(focusManager),
                )
                ActionButton("Falar", icon = Icons.Rounded.Mic) { ouvindo = true }
                ActionButton("Enviar", icon = Icons.Rounded.Send) { onSend(query); query = "" }
            }
        }
    }
}

@Composable
private fun Boasvindas() {
    Column(Modifier.padding(top = 12.dp)) {
        Text("Converse sobre o que assistir", color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pergunte como se estivesse falando com alguém: \"o que estreou de terror esse mês?\", " +
                "\"queria algo leve pra hoje\", \"parecido com Interestelar\". Ela pesquisa na internet " +
                "quando precisa e sugere o que existe no seu catálogo.",
            color = Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.widthIn(max = 640.dp),
        )
    }
}

@Composable
private fun Mensagem(
    message: ChatMessage,
    catalog: Catalog,
    onOpenMovie: (Video) -> Unit,
    onOpenSeries: (Series) -> Unit,
) {
    val doUsuario = message.role == ChatRole.USER
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.align(if (doUsuario) Alignment.End else Alignment.Start)
                .widthIn(max = 620.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (doUsuario) Coral.copy(alpha = 0.18f) else Panel)
                .border(1.dp, if (doUsuario) Coral.copy(alpha = 0.4f) else Edge, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(message.text, color = Mist, fontSize = 15.sp, lineHeight = 21.sp)
        }
        if (message.titles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Sugestoes(message.titles, catalog, onOpenMovie, onOpenSeries)
        }
    }
}

/** As sugestões guardadas são chaves (m:id / s:id) resolvidas no catálogo atual. */
@Composable
private fun Sugestoes(
    keys: List<String>,
    catalog: Catalog,
    onOpenMovie: (Video) -> Unit,
    onOpenSeries: (Series) -> Unit,
) {
    val filmes = remember(keys, catalog) {
        keys.filter { it.startsWith("m:") }.mapNotNull { key ->
            catalog.movies.firstOrNull { it.id == key.removePrefix("m:") }
        }
    }
    val series = remember(keys, catalog) {
        keys.filter { it.startsWith("s:") }.mapNotNull { key ->
            catalog.series.firstOrNull { it.id == key.removePrefix("s:") }
        }
    }
    Column {
        if (filmes.isNotEmpty()) PosterRow("No seu catálogo", filmes, onOpenMovie, compacto = true)
        if (series.isNotEmpty()) SeriesRow("Séries no seu catálogo", series, onOpenSeries, compacto = true)
    }
}

@Composable
private fun ConversasAnteriores(
    sessions: List<ChatSession>,
    atual: ChatSession?,
    onNewChat: () -> Unit,
    onOpenChat: (ChatSession) -> Unit,
) {
    Column(Modifier.width(230.dp).fillMaxHeight().padding(start = 32.dp, top = 16.dp, end = 4.dp)) {
        ActionButton("Nova conversa", icon = Icons.Rounded.Add, onClick = onNewChat)
        Spacer(Modifier.height(12.dp))
        Text("CONVERSAS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(sessions.size) { index ->
                val sessao = sessions[index]
                ConversaItem(sessao.title, sessao.id == atual?.id) { onOpenChat(sessao) }
            }
        }
    }
}

@Composable
private fun ConversaItem(title: String, selecionada: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Coral else if (selecionada) Panel else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            title,
            color = if (focused) Color.White else Mist,
            fontSize = 13.sp,
            maxLines = 2,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SemChave(onConfigureKey: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 32.dp, top = 28.dp, end = 32.dp)) {
        Text("Conversar com a IA", color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Falta a chave da OpenAI. Configure em poucos segundos lendo um QR code com o celular.",
            color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))
        ActionButton("Configurar chave (QR code)", onClick = onConfigureKey)
    }
}
