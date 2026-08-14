package br.com.cinemora.tv.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Servidor local para digitar a chave da OpenAI pelo celular: a TV mostra um QR com este
 * endereço e o celular (na mesma rede) envia a chave. Nada sai da rede local — não há
 * servidor externo envolvido.
 */
class KeyPairingServer(private val onKey: (String) -> Unit) {
    private var socket: ServerSocket? = null
    @Volatile private var running = false

    val address: String? get() = socket?.let { "http://${localIp()}:${it.localPort}" }

    fun start(): Boolean {
        if (running) return true
        val aberto = runCatching { ServerSocket(PORT) }.getOrNull() ?: return false
        socket = aberto
        running = true
        thread(isDaemon = true, name = "cinemora-pairing") { accept(aberto) }
        return true
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        socket = null
    }

    private fun accept(server: ServerSocket) {
        while (running) {
            val client = runCatching { server.accept() }.getOrNull() ?: continue
            runCatching { handle(client) }
            runCatching { client.close() }
        }
    }

    private fun handle(client: Socket) {
        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
        val requestLine = reader.readLine().orEmpty()
        var contentLength = 0
        while (true) {
            val header = reader.readLine().orEmpty()
            if (header.isBlank()) break
            if (header.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }

        val enviouChave = requestLine.startsWith("POST")
        val corpo = if (enviouChave && contentLength > 0) {
            CharArray(contentLength).also { reader.read(it, 0, contentLength) }.concatToString()
        } else {
            ""
        }
        val chave = if (enviouChave) PairingForm.readKey(corpo) else null
        chave?.let(onKey)

        val html = when {
            chave != null -> page(SUCCESS_BODY)
            enviouChave -> page(FORM_BODY, erro = "Cole a chave antes de enviar.")
            else -> page(FORM_BODY)
        }
        client.getOutputStream().write(
            buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${html.toByteArray().size}\r\n")
                append("Connection: close\r\n\r\n")
                append(html)
            }.toByteArray(),
        )
    }

    private fun localIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull() ?: "0.0.0.0"

    private fun page(body: String, erro: String? = null) = """
        <!doctype html><html lang="pt-br"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cinemora — chave da OpenAI</title>
        <style>
          body { font-family: -apple-system, system-ui, sans-serif; background:#0b0710; color:#f1ecef;
                 margin:0; padding:28px; }
          h1 { font-size:20px; margin:0 0 6px; }
          p { color:#a9a2ab; font-size:14px; line-height:1.5; }
          a { color:#f5222d; }
          input { width:100%; box-sizing:border-box; padding:14px; font-size:16px; margin-top:14px;
                  border-radius:10px; border:1px solid #2a2130; background:#140a10; color:#f1ecef; }
          button { width:100%; padding:15px; font-size:16px; margin-top:12px; border:0;
                   border-radius:10px; background:#e50914; color:#fff; font-weight:600; }
          .erro { color:#f5222d; font-size:14px; margin-top:12px; }
          .ok { font-size:17px; }
        </style></head><body>
        <h1>Cinemora</h1>
        ${erro?.let { "<p class=\"erro\">$it</p>" }.orEmpty()}
        $body
        </body></html>
    """.trimIndent()

    private companion object {
        const val PORT = 8765
        const val FORM_BODY = """
            <p>Cole aqui a sua chave da OpenAI para ativar as recomendações por IA na TV.</p>
            <p>Não tem uma chave?
               <a href="https://platform.openai.com/api-keys" target="_blank">Crie em platform.openai.com/api-keys</a>
               — veja o
               <a href="https://help.openai.com/en/articles/4936850-where-do-i-find-my-openai-api-key" target="_blank">passo a passo da OpenAI</a>.
            </p>
            <form method="POST" action="/">
              <input name="key" type="password" placeholder="sk-..." autocomplete="off" autofocus>
              <button type="submit">Enviar para a TV</button>
            </form>
        """
        const val SUCCESS_BODY = """
            <p class="ok">Chave enviada. Pode voltar para a TV — já está configurada.</p>
        """
    }
}
