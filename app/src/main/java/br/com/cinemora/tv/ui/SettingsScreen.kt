package br.com.cinemora.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import br.com.cinemora.tv.BuildConfig
import br.com.cinemora.tv.UpdateState
import br.com.cinemora.tv.data.CacheTtl
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.data.VOZES_OPENAI
import br.com.cinemora.tv.data.VoiceMode
import br.com.cinemora.tv.data.VoiceSpeed

@Composable
internal fun SettingsSection(
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    hasOpenAiKey: Boolean,
    onConfigureKey: () -> Unit,
    cacheTtl: CacheTtl,
    onSetCacheTtl: (CacheTtl) -> Unit,
    sortOrder: SortOrder,
    onSetSortOrder: (SortOrder) -> Unit,
    onClearCache: () -> Unit,
    onRefresh: () -> Unit,
    voiceMode: VoiceMode,
    onSetVoiceMode: (VoiceMode) -> Unit,
    openAiVoice: String,
    onSetOpenAiVoice: (String) -> Unit,
    voiceSpeed: VoiceSpeed,
    onSetVoiceSpeed: (VoiceSpeed) -> Unit,
    typewriter: Boolean,
    onSetTypewriter: (Boolean) -> Unit,
    liveEnabled: Boolean,
    onSetLiveEnabled: (Boolean) -> Unit,
) {
    var cleared by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = 40.dp, top = 22.dp, end = 40.dp, bottom = 32.dp)) {
        Text("Definições", color = Mist, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))
        Text("VERSÃO DO APP", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            when (updateState) {
                is UpdateState.Available -> "Nova versão ${updateState.info.version} disponível."
                is UpdateState.Downloading -> "Baixando ${updateState.info.version}… ${updateState.percent}%"
                is UpdateState.Failed -> updateState.message
                UpdateState.Idle -> "Instalada: ${BuildConfig.VERSION_NAME}"
            },
            color = if (updateState is UpdateState.Available) Signal else Muted, fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (updateState is UpdateState.Available) {
                ActionButton("Atualizar agora", onClick = onDownloadUpdate)
            }
            ActionButton(
                "Buscar atualização",
                // Voltando das opções de baixo, a página retorna ao topo em vez de ficar cortada.
                modifier = Modifier.onFocusChanged { if (it.isFocused) scope.launch { scrollState.animateScrollTo(0) } },
                onClick = onCheckUpdate,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("RECOMENDAÇÃO POR IA", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasOpenAiKey) "Chave da OpenAI configurada." else "Sem chave configurada — a busca por IA fica indisponível.",
            color = if (hasOpenAiKey) Signal else Muted, fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton(if (hasOpenAiKey) "Trocar chave (QR code)" else "Configurar chave (QR code)", onClick = onConfigureKey)
        Spacer(Modifier.height(14.dp))
        Text("VOZ DA IA", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VoiceMode.entries.forEach { modo -> Chip(modo.label(), modo == voiceMode) { onSetVoiceMode(modo) } }
        }
        if (voiceMode == VoiceMode.OPENAI) {
            Spacer(Modifier.height(10.dp))
            Text("Escolha a voz — ao selecionar, ela se apresenta.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(VOZES_OPENAI) { voz ->
                    Chip(voz.replaceFirstChar { it.uppercase() }, voz == openAiVoice) { onSetOpenAiVoice(voz) }
                }
            }
        }
        if (voiceMode != VoiceMode.MUDO) {
            Spacer(Modifier.height(10.dp))
            Text("VELOCIDADE DA FALA", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VoiceSpeed.entries.forEach { v -> Chip(v.label(), v == voiceSpeed) { onSetVoiceSpeed(v) } }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("CONVERSA AO VIVO", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text("Fala contínua com a IA. É o recurso mais caro: cobra por minuto de áudio.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Chip("Ativada", liveEnabled) { onSetLiveEnabled(true) }
            Chip("Desativada", !liveEnabled) { onSetLiveEnabled(false) }
        }
        Spacer(Modifier.height(14.dp))
        Text("RESPOSTA NO CHAT", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Chip("Escrevendo aos poucos", typewriter) { onSetTypewriter(true) }
            Chip("Texto de uma vez", !typewriter) { onSetTypewriter(false) }
        }
        Spacer(Modifier.height(18.dp))
        Text("ORDENAR OS TÍTULOS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SortOrder.entries.forEach { order -> Chip(order.label(), order == sortOrder) { onSetSortOrder(order) } }
        }
        Spacer(Modifier.height(18.dp))
        Text("VALIDADE DO CACHE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CacheTtl.entries.forEach { ttl -> Chip(ttl.label(), ttl == cacheTtl) { onSetCacheTtl(ttl) } }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Dentro da validade, o catálogo abre instantâneo do disco. Vencido, é rebuscado do servidor.",
            color = Muted, fontSize = 13.sp, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text("LISTA / CACHE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)) {
                Text("Atualizar agora", fontWeight = FontWeight.Medium)
            }
            Button(onClick = { onClearCache(); cleared = true }, colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Mist)) {
                Text("Limpar cache")
            }
        }
        if (cleared) {
            Spacer(Modifier.height(14.dp))
            Text("Cache limpo. Será rebuscado no próximo acesso ou ao atualizar.", color = Signal, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Coral else Panel)
            .border(if (focused) 2.dp else 1.dp, if (selected || focused) Coral else Edge, RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, color = if (selected) Color.White else Mist, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

private fun VoiceSpeed.label(): String = when (this) {
    VoiceSpeed.LENTA -> "Lenta"
    VoiceSpeed.NORMAL -> "Normal"
    VoiceSpeed.RAPIDA -> "Rápida"
}

private fun VoiceMode.label(): String = when (this) {
    VoiceMode.GOOGLE -> "Google (rápida)"
    VoiceMode.OPENAI -> "OpenAI (natural)"
    VoiceMode.MUDO -> "Sem voz"
}

private fun SortOrder.label(): String = when (this) {
    SortOrder.PADRAO -> "Do provedor"
    SortOrder.ALFABETICA -> "A-Z"
    SortOrder.LANCAMENTO -> "Lançamento"
    SortOrder.NOTA -> "Nota"
}

private fun CacheTtl.label(): String = when (this) {
    CacheTtl.SIX_HOURS -> "6 horas"
    CacheTtl.TWELVE_HOURS -> "12 horas"
    CacheTtl.TWENTY_FOUR_HOURS -> "24 horas"
}
