package br.com.cinemora.tv.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import br.com.cinemora.tv.data.ResumeEntry

/** Oferece retomar o que ficou pela metade assim que o app abre. */
@Composable
internal fun ResumeBanner(
    entry: ResumeEntry?,
    onResume: (ResumeEntry) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entry == null) return
    // Nas fileiras o D-pad percorre os cartões e nunca chega ao banner: ele assume o foco.
    val continuarFoco = remember { FocusRequester() }
    LaunchedEffect(entry.id) { runCatching { continuarFoco.requestFocus() } }
    Column(
        modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Edge, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text("Continuar assistindo", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(entry.title, color = Mist, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
        if (entry.positionMs >= 60_000) {
            Spacer(Modifier.height(4.dp))
            Text("Você parou em ${formatPosition(entry.positionMs)}.", color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                "Continuar",
                modifier = Modifier.focusRequester(continuarFoco),
                icon = Icons.Rounded.PlayArrow,
            ) { onResume(entry) }
            ActionButton("Agora não", onClick = onDismiss)
        }
    }
}

/** 1h05 / 12min — igual ao usado nas telas de detalhe. */
private fun formatPosition(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "%dh%02d".format(hours, minutes) else "${minutes}min"
}
