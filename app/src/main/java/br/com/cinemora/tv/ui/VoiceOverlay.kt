package br.com.cinemora.tv.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Escuta em tela cheia com visual do app. Usamos o SpeechRecognizer direto, em vez do
 * RecognizerIntent, porque a tela do sistema aparece como um painel solto sobre o app.
 */
@Composable
internal fun VoiceOverlay(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Preparando o microfone…") }
    var level by remember { mutableFloatStateOf(0f) }
    var permitido by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        permitido = concedida
        if (!concedida) status = "Sem acesso ao microfone. Use o campo para digitar."
    }

    LaunchedEffect(Unit) { if (!permitido) pedirPermissao.launch(Manifest.permission.RECORD_AUDIO) }

    BackHandler(onBack = onDismiss)

    if (permitido) {
        DisposableEffect(Unit) {
            val disponivel = SpeechRecognizer.isRecognitionAvailable(context)
            val recognizer = if (disponivel) SpeechRecognizer.createSpeechRecognizer(context) else null
            if (recognizer == null) {
                status = "Este aparelho não tem reconhecimento de voz. Use o campo para digitar."
            } else {
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status = "Pode falar" }
                    override fun onBeginningOfSpeech() { status = "Ouvindo…" }
                    override fun onRmsChanged(rmsdB: Float) { level = (rmsdB / 10f).coerceIn(0f, 1f) }
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() { status = "Entendendo…" }
                    override fun onError(error: Int) { status = errorMessage(error) }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (text.isBlank()) status = "Não entendi. Tente de novo ou digite." else onResult(text)
                    }
                    override fun onPartialResults(results: Bundle?) {
                        partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                recognizer.startListening(listenIntent(context.packageName))
            }
            onDispose { recognizer?.destroy() }
        }
    }

    val escala by animateFloatAsState(1f + level * 0.35f, label = "mic")
    Box(Modifier.fillMaxSize().background(Color(0xF0090509)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(112.dp).scale(escala).clip(CircleShape).background(Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(26.dp))
            Text(status, color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            if (partial.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    partial, color = Signal, fontSize = 17.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 620.dp).padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text("Pressione Voltar para digitar", color = Muted, fontSize = 13.sp)
        }
    }
}

private fun listenIntent(packageName: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
}

private fun errorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi. Tente de novo ou digite."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Tente de novo ou digite."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sem conexão para reconhecer a fala."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem acesso ao microfone."
    else -> "Não consegui ouvir. Use o campo para digitar."
}
