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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.UpdateState

/** Aviso de nova versão publicada no GitHub, com o changelog do release. */
@Composable
internal fun UpdateBanner(
    state: UpdateState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        else -> return
    }
    val baixando = state as? UpdateState.Downloading

    Column(
        modifier
            .widthIn(max = 460.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Edge, RoundedCornerShape(12.dp))
            .padding(18.dp),
    ) {
        Text("Atualização disponível  ·  ${info.version}", color = Mist, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (info.changelog.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(info.changelog, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 6)
        }
        Spacer(Modifier.height(14.dp))
        if (baixando != null) {
            Text("Baixando… ${baixando.percent}%", color = Signal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Atualizar agora", onClick = onUpdate)
                ActionButton("Agora não", onClick = onDismiss)
            }
        }
    }
}
