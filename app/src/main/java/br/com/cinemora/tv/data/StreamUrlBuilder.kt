package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Credentials

/** Monta as URLs de stream do padrão Xtream Codes para filme, canal ao vivo e episódio. */
object StreamUrlBuilder {
    fun movie(credentials: Credentials, streamId: String, extension: String): String =
        "${base(credentials)}/movie/${credentials.username}/${credentials.password}/$streamId.${ext(extension, "mp4")}"

    fun live(credentials: Credentials, streamId: String, extension: String = "ts"): String =
        "${base(credentials)}/live/${credentials.username}/${credentials.password}/$streamId.${ext(extension, "ts")}"

    fun seriesEpisode(credentials: Credentials, episodeId: String, extension: String): String =
        "${base(credentials)}/series/${credentials.username}/${credentials.password}/$episodeId.${ext(extension, "mp4")}"

    private fun base(credentials: Credentials) = credentials.serverUrl.trim().trimEnd('/')

    private fun ext(extension: String, fallback: String) = extension.ifBlank { fallback }.trimStart('.')
}
