package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Resposta avulsa da IA: "Vale a pena?" e "Onde eu parei?". */
@Composable
internal fun InsightDialog(titulo: String, texto: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val foco = remember { FocusRequester() }
    LaunchedEffect(titulo) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 760.dp).padding(32.dp)) {
            Text(titulo, color = Signal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(texto, color = Mist, fontSize = 18.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(24.dp))
            ActionButton("Fechar", modifier = Modifier.focusRequester(foco), onClick = onClose)
        }
    }
}
