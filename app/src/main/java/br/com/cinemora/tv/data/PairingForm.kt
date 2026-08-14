package br.com.cinemora.tv.data

import java.net.URLDecoder

/** Lê a chave enviada pelo celular no formulário de pareamento. */
object PairingForm {
    fun readKey(body: String): String? = body
        .split("&")
        .firstOrNull { it.startsWith("key=") }
        ?.removePrefix("key=")
        ?.let { URLDecoder.decode(it, "UTF-8") }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
