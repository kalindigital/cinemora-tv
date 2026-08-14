package br.com.cinemora.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

internal val Ink = Color(0xFF07090D)
internal val Panel = Color(0xFF141019)
internal val Mist = Color(0xFFF1ECEF)
internal val Signal = Color(0xFFF5222D)
internal val Coral = Color(0xFFE50914)
internal val Muted = Color(0xFFA9A2AB)
internal val Edge = Color(0xFF2A2130)

/** Em TV os campos de texto capturam ↑/↓ para o cursor; interceptamos para mover o foco. */
internal fun Modifier.dpadFocusNav(focusManager: FocusManager): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
        else -> false
    }
}

@Composable
internal fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Signal,
    unfocusedBorderColor = Edge,
    focusedTextColor = Mist,
    unfocusedTextColor = Mist,
    focusedLabelColor = Signal,
    unfocusedLabelColor = Muted,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
    cursorColor = Signal,
    focusedContainerColor = Color(0xFF0C121A),
    unfocusedContainerColor = Color(0xFF0C121A),
)

/**
 * Botão de ação padrão: com foco fica vermelho sólido e texto branco;
 * sem foco fica com fundo vermelho esmaecido e texto vermelho forte.
 */
@Composable
internal fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) Coral else Coral.copy(alpha = 0.16f),
            contentColor = if (focused) Color.White else Signal,
        ),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal)
    }
}

/** Botão redondo só com ícone, para as ações do chat. */
@Composable
internal fun IconActionButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (focused) Coral else Coral.copy(alpha = 0.16f))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (focused) Color.White else Signal, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun RowTitle(text: String) = Text(
    text, color = Mist, fontWeight = FontWeight.Bold, fontSize = 20.sp,
    modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 10.dp),
)

/** Cartão-pôster vertical (filmes e séries), com destaque ao foco. */
@Composable
internal fun PosterCard(
    title: String,
    imageUrl: String?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    compacto: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier.width(if (compacto) 108.dp else 150.dp)
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.06f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(if (focused) 3.dp else 1.dp, if (focused) Signal else Edge, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable(),
    ) {
        AsyncImage(
            model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(if (compacto) 154.dp else 214.dp).background(Color(0xFF26313D)),
        )
        Text(
            title, color = Mist, fontWeight = FontWeight.Normal,
            minLines = if (compacto) 1 else 2, maxLines = if (compacto) 1 else 2,
            fontSize = if (compacto) 11.sp else 13.sp, lineHeight = 16.sp,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        )
        if (subtitle != null) {
            Text(subtitle, color = Muted, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 3.dp, bottom = 8.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Cartão de canal ao vivo, com o logo centralizado. */
@Composable
internal fun ChannelCard(name: String, logoUrl: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier.width(176.dp)
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.06f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(if (focused) 3.dp else 1.dp, if (focused) Signal else Edge, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable(),
    ) {
        AsyncImage(
            model = logoUrl, contentDescription = name, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(96.dp).background(Color(0xFF0C1218)).padding(14.dp),
        )
        Text(name, color = Mist, fontWeight = FontWeight.Normal, maxLines = 1, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
    }
}
