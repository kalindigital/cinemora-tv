package br.com.cinemora.tv.data

import org.json.JSONObject

/** Atualização publicada no GitHub Releases. */
data class UpdateInfo(val version: String, val changelog: String, val apkUrl: String)

object GithubUpdates {
    fun parse(json: String, versaoAtual: String): UpdateInfo? {
        val release = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val versao = release.optString("tag_name").removePrefix("v").trim()
        if (versao.isBlank() || !isNewer(versao, versaoAtual)) return null

        val assets = release.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?.optString("browser_download_url")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return UpdateInfo(versao, release.optString("body").trim(), apk)
    }

    /** Compara número a número: "1.10.0" é mais novo que "1.9.0", o que a ordem alfabética erraria. */
    fun isNewer(candidata: String, atual: String): Boolean {
        val novos = partes(candidata)
        val velhos = partes(atual)
        repeat(maxOf(novos.size, velhos.size)) { index ->
            val novo = novos.getOrElse(index) { 0 }
            val velho = velhos.getOrElse(index) { 0 }
            if (novo != velho) return novo > velho
        }
        return false
    }

    private fun partes(versao: String) = versao
        .removePrefix("v")
        .trim()
        .split(".")
        .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
