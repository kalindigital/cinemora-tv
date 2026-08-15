package br.com.cinemora.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Fundo das telas de detalhe: a arte do título ocupa a tela e escurece à esquerda e embaixo,
 * onde ficam o texto e os botões.
 */
@Composable
internal fun DetalheFundo(imageUrl: String?, description: String?, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        AsyncImage(
            model = imageUrl, contentDescription = description, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Ink, 0.42f to Color(0xF0070A0D), 0.78f to Color(0x33070A0D), 1f to Color(0x00070A0D)),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0xB3050307), 0.45f to Color(0x40050307), 1f to Color(0xF2050307)),
            ),
        )
        content()
    }
}

/** Botão principal da tela de detalhe: pílula clara, como no aparelho da sala. */
@Composable
internal fun BotaoPrincipal(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.width(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (focused) Color(0xFFF4F6F8) else Color(0x33FFFFFF))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, contentDescription = null, tint = if (focused) Ink else Mist,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label, color = if (focused) Ink else Mist, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/** Item da lista de opções, abaixo do botão principal. */
@Composable
internal fun OpcaoMenu(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.width(320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Color(0x26FFFFFF) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, contentDescription = null, tint = if (focused) Mist else Muted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (focused) Mist else Color(0xFFB9C2CC), fontSize = 14.sp, maxLines = 1)
    }
}

/** Linha de dados do detalhe: ano · gênero · duração · nota. */
internal fun linhaDeDados(vararg partes: String?): String =
    partes.filterNotNull().filter { it.isNotBlank() }.joinToString("   ·   ")

/** Barra fina de progresso, usada na miniatura do episódio e nos cartões. */
@Composable
internal fun BarraProgresso(fracao: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color(0x99000000))) {
        Box(Modifier.fillMaxWidth(fracao.coerceIn(0f, 1f)).height(4.dp).background(Signal))
    }
}
