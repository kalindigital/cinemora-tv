package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.model.Video

/** Mostrada quando um filme termina: o que assistir em seguida. */
@Composable
internal fun RecommendationsScreen(items: List<Video>, onOpen: (Video) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    LazyColumn(Modifier.fillMaxSize().background(Ink), contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)) {
        item {
            Column(Modifier.padding(start = 32.dp, end = 32.dp)) {
                Text("O que assistir agora?", color = Mist, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Títulos parecidos com o que você acabou de ver.", color = Muted, fontSize = 14.sp)
            }
        }
        item { PosterRow("Recomendados para você", items, onOpen) }
        item {
            Column(Modifier.padding(start = 32.dp, top = 24.dp)) {
                ActionButton("Voltar ao catálogo", onClick = onClose)
            }
        }
    }
}
