package br.com.cinemora.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.data.KeyPairingServer
import br.com.cinemora.tv.data.QrCodes

/**
 * Pareamento por QR: a TV sobe um servidor na rede local e o celular envia a chave.
 * Digitar "sk-..." no controle remoto seria inviável.
 */
@Composable
internal fun KeySetupScreen(onKeyReceived: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var recebida by remember { mutableStateOf(false) }
    var endereco by remember { mutableStateOf<String?>(null) }
    var falhou by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val server = KeyPairingServer { chave ->
            onKeyReceived(chave)
            recebida = true
        }
        if (server.start()) endereco = server.address else falhou = true
        onDispose { server.stop() }
    }

    Column(
        Modifier.fillMaxSize().background(Ink).padding(40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Chave da OpenAI", color = Mist, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "As recomendações por IA usam a sua chave da OpenAI. Envie do celular — é mais fácil que digitar aqui.",
            color = Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.widthIn(max = 720.dp),
        )
        Spacer(Modifier.height(24.dp))

        when {
            recebida -> Text("Chave recebida e salva. As recomendações por IA já estão ativas.", color = Signal, fontSize = 17.sp)
            falhou || endereco == null ->
                Text("Não consegui abrir o pareamento nesta rede. Confira a conexão da TV.", color = Signal, fontSize = 15.sp)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                val qr = remember(endereco) { QrCodes.bitmap(endereco!!) }
                if (qr != null) {
                    Image(
                        qr.asImageBitmap(), contentDescription = "QR com o endereço de pareamento",
                        modifier = Modifier.size(230.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
                Spacer(Modifier.width(28.dp))
                Column(Modifier.widthIn(max = 620.dp)) {
                    Passo(1, "Aponte a câmera do celular para o QR (o celular precisa estar na mesma rede da TV).")
                    Passo(2, "Se preferir, digite no navegador do celular: $endereco")
                    Passo(3, "Cole a chave na página e toque em Enviar. A página traz o link da OpenAI para criar a chave.")
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Onde pegar: platform.openai.com/api-keys",
                        color = Signal, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        ActionButton(if (recebida) "Concluir" else "Voltar", onClick = onClose)
    }
}

@Composable
private fun Passo(numero: Int, texto: String) {
    Row(Modifier.padding(bottom = 10.dp)) {
        Text("$numero.", color = Signal, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Text(texto, color = Mist, fontSize = 15.sp, lineHeight = 21.sp)
    }
}
