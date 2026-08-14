package br.com.cinemora.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.AiState
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video

@Composable
internal fun AiSection(
    state: AiState,
    hasKey: Boolean,
    onConfigureKey: () -> Unit,
    onAsk: (String) -> Unit,
    onOpenMovie: (Video) -> Unit,
    onOpenSeries: (Series) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var ouvindo by remember { mutableStateOf(false) }
    var micJaAberto by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Sem chave não faz sentido abrir o microfone: o pedido não teria como ser atendido.
    LaunchedEffect(hasKey) {
        if (hasKey && !micJaAberto) {
            micJaAberto = true
            ouvindo = true
        }
    }

    if (!hasKey) {
        Column(Modifier.fillMaxSize().padding(start = 32.dp, top = 28.dp, end = 32.dp)) {
            Text("Recomendação por IA", color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Falta a chave da OpenAI. Configure em poucos segundos lendo um QR code com o celular.",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(18.dp))
            ActionButton("Configurar chave (QR code)", onClick = onConfigureKey)
        }
        return
    }

    if (ouvindo) {
        VoiceOverlay(
            onResult = { falado -> ouvindo = false; query = falado; onAsk(falado) },
            onDismiss = { ouvindo = false },
        )
        return
    }

    // Tudo em uma lista rolável: com o cabeçalho fixo, as fileiras ficavam cortadas embaixo.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)) {
        item {
            Column(Modifier.padding(start = 32.dp, end = 32.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("O que você quer assistir?") },
                    singleLine = true, colors = fieldColors(),
                    // Enter/OK no teclado já dispara a busca, sem precisar descer até o botão.
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); onAsk(query) }),
                    modifier = Modifier.fillMaxWidth().dpadFocusNav(focusManager),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Falar", icon = Icons.Rounded.Mic) { ouvindo = true }
                    ActionButton("Buscar", icon = Icons.Rounded.Search) { onAsk(query) }
                }
            }
        }
        when (state) {
            AiState.Idle -> item { Notice("Fale ou digite o que você quer assistir.") }
            AiState.Loading -> item { Notice("Pensando em boas opções…") }
            is AiState.Failed -> item { Notice(state.message) }
            is AiState.Loaded -> {
                if (state.matches.movies.isEmpty() && state.matches.series.isEmpty()) {
                    item {
                        Column(Modifier.padding(start = 32.dp, top = 18.dp, end = 32.dp)) {
                            Text("A IA sugeriu títulos que não estão no seu catálogo:", color = Muted, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(state.suggestions.joinToString(" · "), color = Mist, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                } else {
                    if (state.matches.movies.isNotEmpty()) {
                        item { PosterRow("Filmes para \"${state.query}\"", state.matches.movies, onOpenMovie) }
                    }
                    if (state.matches.series.isNotEmpty()) {
                        item { SeriesRow("Séries para \"${state.query}\"", state.matches.series, onOpenSeries) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String) =
    Text(text, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 32.dp, top = 22.dp))
