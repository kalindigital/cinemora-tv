package br.com.cinemora.tv.data

/**
 * Página que toca o trailer DENTRO do aplicativo, em tela cheia.
 *
 * Usa o player oficial do YouTube (IFrame Player API) — é o jeito autorizado de
 * embutir: quem reproduz continua sendo o player deles, com anúncios e métricas.
 * Sair para o aplicativo do YouTube ficava ruim (o usuário perdia o contexto e
 * voltava sem saber onde estava), então ele passa a ser apenas a reserva.
 *
 * Nem todo vídeo pode ser embutido: o dono do canal pode desativar. Nesse caso o
 * player devolve o erro 101 ou 150 e a tela avisa o aplicativo, que abre o
 * trailer no YouTube como antes.
 */
object TrailerPlayerPage {
    /** A API do IFrame só aceita o embed se a página for servida desta origem. */
    const val BASE_URL = "https://www.youtube.com"

    /** Nome da ponte JavaScript ↔ Android usada pela tela do trailer. */
    const val BRIDGE = "CinemoraTrailer"

    /** Códigos do YouTube para "o dono não permite tocar fora do site". */
    fun ehEmbedProibido(codigo: Int): Boolean = codigo == 101 || codigo == 150

    fun html(videoId: String): String {
        val id = escapar(videoId)
        return """
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  html,body{margin:0;height:100%;background:#000;overflow:hidden}
  #player{width:100vw;height:100vh}
</style>
</head>
<body>
<div id="player"></div>
<script src="$BASE_URL/iframe_api"></script>
<script>
  var player = null;

  function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
      videoId: '$id',
      playerVars: {
        autoplay: 1,
        rel: 0,
        modestbranding: 1,
        playsinline: 1,
        origin: '$BASE_URL'
      },
      events: {
        onReady: function (e) { e.target.playVideo(); },
        onError: function (e) { $BRIDGE.aoFalhar(e.data); },
        onStateChange: function (e) { if (e.data === YT.PlayerState.ENDED) $BRIDGE.aoTerminar(); }
      }
    });
  }

  // Chamadas do controle remoto (o Android intercepta as teclas do D-pad).
  window.cinemoraAlternar = function () {
    if (!player) return;
    var estado = player.getPlayerState();
    if (estado === YT.PlayerState.PLAYING) player.pauseVideo(); else player.playVideo();
  };
  window.cinemoraPular = function (segundos) {
    if (!player) return;
    player.seekTo(Math.max(0, player.getCurrentTime() + segundos), true);
  };
</script>
</body>
</html>
        """.trimIndent()
    }

    /** O identificador entra dentro de aspas simples no script: nada pode escapar delas. */
    private fun escapar(videoId: String): String =
        videoId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
}
