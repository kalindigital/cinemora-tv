package br.com.cinemora.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import br.com.cinemora.tv.data.TrailerPlayerPage
import br.com.cinemora.tv.data.Trailers

/**
 * Toca o trailer em tela cheia dentro do aplicativo, no player oficial do
 * YouTube embutido. Se o vídeo não puder ser embutido (o dono do canal desativa)
 * ou a TV não tiver WebView, cai para o aplicativo do YouTube — o caminho antigo.
 */
class TrailerActivity : Activity() {

    private var web: WebView? = null
    private var videoId: String = ""
    private var jaSaiu = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId = intent.getStringExtra(EXTRA_VIDEO_ID).orEmpty()
        Log.i(TAG, "abrindo trailer videoId='$videoId'")
        if (videoId.isBlank()) { Log.w(TAG, "sem videoId: encerrando"); finish(); return }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val view = runCatching { WebView(this) }.getOrNull()
        if (view == null) { cairParaYoutube(); return }   // TV sem WebView

        view.setBackgroundColor(Color.BLACK)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // O trailer precisa começar sozinho: na TV não há toque para "liberar" o áudio.
            mediaPlaybackRequiresUserGesture = false
        }
        view.addJavascriptInterface(Ponte(), TrailerPlayerPage.BRIDGE)
        view.webViewClient = object : WebViewClient() {
            override fun onReceivedError(v: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                Log.w(TAG, "erro de rede em ${req?.url} principal=${req?.isForMainFrame}: ${err?.description}")
                // Só a falha da própria página conta; recursos soltos do player não.
                if (req?.isForMainFrame == true) cairParaYoutube()
            }
        }
        // Sem isto os erros de JavaScript do player ficam invisíveis no logcat.
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.i(TAG, "console: ${msg.message()} (linha ${msg.lineNumber()})")
                return true
            }
        }
        // A base precisa ser a origem do YouTube, senão a API do IFrame recusa o embed.
        view.loadDataWithBaseURL(
            TrailerPlayerPage.BASE_URL,
            TrailerPlayerPage.html(videoId),
            "text/html",
            "utf-8",
            null,
        )
        web = view
        setContentView(view)
    }

    /** O controle da TV manda teclas: OK pausa/retoma, laterais pulam, Voltar sai. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val js = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "window.cinemoraAlternar()"
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "window.cinemoraPular(10)"
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> "window.cinemoraPular(-10)"
            else -> null
        }
        if (js != null) {
            web?.evaluateJavascript(js, null)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Abre no aplicativo do YouTube e encerra esta tela — usada quando o embed não rola. */
    private fun cairParaYoutube() {
        if (jaSaiu) return
        jaSaiu = true
        if (!Trailers.abrir(this, videoId)) {
            Toast.makeText(this, "Não consegui abrir o trailer.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private inner class Ponte {
        /** Erro do player: embed proibido manda para o YouTube; o resto só encerra. */
        @JavascriptInterface
        fun aoFalhar(codigo: Int) = runOnUiThread {
            val motivo = if (TrailerPlayerPage.ehEmbedProibido(codigo)) "embed proibido pelo canal" else "falha do player"
            Log.w(TAG, "erro $codigo ($motivo): abrindo no aplicativo do YouTube")
            if (TrailerPlayerPage.deveCairParaYoutube(codigo)) cairParaYoutube() else finish()
        }

        @JavascriptInterface
        fun aoTerminar() = runOnUiThread { finish() }
    }

    override fun onPause() {
        super.onPause()
        web?.onPause()
    }

    override fun onResume() {
        super.onResume()
        web?.onResume()
    }

    override fun onDestroy() {
        // Sem isto o áudio do trailer continua tocando depois de sair da tela.
        web?.apply {
            loadUrl("about:blank")
            destroy()
        }
        web = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VIDEO_ID = "videoId"
        private const val TAG = "TrailerActivity"
    }
}
