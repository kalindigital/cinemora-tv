package br.com.cinemora.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.data.ResumeLink
import br.com.cinemora.tv.launcher.OpenLink
import br.com.cinemora.tv.player.PlayerActivity

/**
 * Entrada dos cartões "Continuar assistindo" da tela inicial da TV: recebe apenas um id
 * interno e resolve o stream aqui, sem expor a URL (com usuário e senha) no sistema.
 */
class ResumeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // cinemora://br.com.cinemora.tv/movie/{id} ou /series/{id}: abre o detalhe no app.
        val abrir = intent.data?.pathSegments?.let { OpenLink.parse(it) }
        if (abrir != null) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_KIND, abrir.kind)
                    .putExtra(MainActivity.EXTRA_OPEN_ID, abrir.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            finish()
            return
        }
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
