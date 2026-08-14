package br.com.cinemora.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.data.ResumeLink
import br.com.cinemora.tv.player.PlayerActivity

/**
 * Entrada dos cartões "Continuar assistindo" da tela inicial da TV: recebe apenas um id
 * interno e resolve o stream aqui, sem expor a URL (com usuário e senha) no sistema.
 */
class ResumeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = ResumeLink.idFrom(intent)
        val entrada = id?.let { alvo -> LocalStore(this).resumeEntries().firstOrNull { it.id == alvo } }

        val destino = if (entrada == null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_TITLE, entrada.title)
                putExtra(PlayerActivity.EXTRA_URL, entrada.streamUrl)
                putExtra(PlayerActivity.EXTRA_POSTER, entrada.posterUrl)
            }
        }
        startActivity(destino.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
