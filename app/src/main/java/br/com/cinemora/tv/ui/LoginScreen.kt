package br.com.cinemora.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cinemora.tv.R

@Composable
internal fun LoginScreen(onSignIn: (String, String, String) -> Unit) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Box(Modifier.fillMaxSize().background(Ink)) {
        Image(
            painterResource(R.drawable.login_bg), contentDescription = null,
            contentScale = ContentScale.Crop, alpha = 0.26f, modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .width(360.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.logo), contentDescription = "Cinemora", modifier = Modifier.size(76.dp).clip(CircleShape))
            Spacer(Modifier.height(10.dp))
            Text("CINEMORA", color = Signal, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = server, onValueChange = { server = it }, label = { Text("Servidor") },
                singleLine = true, colors = fieldColors(),
                modifier = Modifier.fillMaxWidth().dpadFocusNav(focusManager),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it }, label = { Text("Usuário") },
                singleLine = true, colors = fieldColors(),
                modifier = Modifier.fillMaxWidth().dpadFocusNav(focusManager),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = fieldColors(),
                modifier = Modifier.fillMaxWidth().dpadFocusNav(focusManager),
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onSignIn(server, username, password) },
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Entrar", fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
internal fun LoadingScreen() = Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
    Text("Montando seu catálogo…", color = Mist, fontSize = 24.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun ErrorScreen(message: String, onRetry: () -> Unit) = Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Não foi possível entrar", color = Mist, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(message, color = Color(0xFFC9C2CB))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)) {
            Text("Voltar aos dados de acesso")
        }
    }
}
