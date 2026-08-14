package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.model.Video

/** Avisa que um título da sua lista de espera entrou no catálogo. */
@Composable
internal fun ChegouBanner(
    chegada: Pair<String, Video>,
    onOpen: (Video) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val foco = remember { FocusRequester() }
    LaunchedEffect(chegada.second.id) { runCatching { foco.requestFocus() } }
    Column(
        modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Edge, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text("CHEGOU NO CATÁLOGO", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(chegada.second.title, color = Mist, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text("Você pediu para avisar sobre \"${chegada.first}\".", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Ver agora", modifier = Modifier.focusRequester(foco)) { onOpen(chegada.second) }
            ActionButton("Agora não", onClick = onDismiss)
        }
    }
}
