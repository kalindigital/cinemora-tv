package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Channel
import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.SeriesDetail
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Video
import org.json.JSONArray
import org.json.JSONObject

/** Converte as respostas JSON do player_api Xtream nos modelos do app. Funções puras. */
object XtreamParser {
    fun isAuthenticated(json: String): Boolean =
        runCatching { JSONObject(json).optJSONObject("user_info")?.optInt("auth") == 1 }.getOrDefault(false)

    /** A sinopse do filme só vem no get_vod_info (info.plot / info.description), não na listagem. */
    fun moviePlot(json: String): String? {
        val info = JSONObject(json).optJSONObject("info") ?: return null
        return info.optStringOrNull("plot") ?: info.optStringOrNull("description")
    }

    fun categories(json: String): List<Category> = JSONArray(json).objects().map {
        Category(it.optString("category_id"), it.optString("category_name", "Sem categoria"))
    }

    fun movies(json: String, credentials: Credentials): List<Video> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optString("stream_id").ifBlank { return@mapNotNull null }
        Video(
            id = id,
            title = item.optString("name", "Sem título"),
            categoryId = item.optString("category_id"),
            coverUrl = item.optStringOrNull("stream_icon"),
            streamUrl = StreamUrlBuilder.movie(credentials, id, item.optString("container_extension", "mp4")),
            // A listagem VOD deste provedor não traz "year": o ano só aparece no título.
            year = item.optStringOrNull("year") ?: yearFromTitle(item.optString("name")),
            rating = formatRating(item.optString("rating")),
            synopsis = item.optStringOrNull("plot"),
        )
    }

    private val TITLE_YEAR = Regex("\\((19|20)\\d{2}\\)")

    private fun yearFromTitle(title: String): String? =
        TITLE_YEAR.findAll(title).lastOrNull()?.value?.trim('(', ')')

    fun channels(json: String, credentials: Credentials): List<Channel> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optString("stream_id").ifBlank { return@mapNotNull null }
        Channel(
            id = id,
            name = item.optString("name", "Sem título"),
            categoryId = item.optString("category_id"),
            logoUrl = item.optStringOrNull("stream_icon"),
            streamUrl = StreamUrlBuilder.live(credentials, id),
        )
    }

    fun series(json: String): List<Series> = JSONArray(json).objects().mapNotNull { item ->
        val id = item.optString("series_id").ifBlank { return@mapNotNull null }
        Series(
            id = id,
            title = item.optString("name", "Sem título"),
            categoryId = item.optString("category_id"),
            coverUrl = item.optStringOrNull("cover"),
            year = item.optStringOrNull("year") ?: item.optString("releaseDate").take(4).takeIf { it.length == 4 && it.all(Char::isDigit) },
            rating = formatRating(item.optString("rating")),
            synopsis = item.optStringOrNull("plot"),
        )
    }

    fun seriesDetail(json: String, series: Series, credentials: Credentials): SeriesDetail {
        val episodesObj = JSONObject(json).optJSONObject("episodes") ?: return SeriesDetail(series, emptyList())
        val seasons = episodesObj.keys().asSequence().mapNotNull { key ->
            val array = episodesObj.optJSONArray(key) ?: return@mapNotNull null
            val episodes = array.objects().map { ep ->
                val epId = ep.optString("id")
                Episode(
                    id = epId,
                    title = cleanEpisodeTitle(ep.optString("title"), ep.optString("episode_num")),
                    season = key.toIntOrNull() ?: 0,
                    episode = ep.optString("episode_num").toIntOrNull() ?: 0,
                    streamUrl = StreamUrlBuilder.seriesEpisode(credentials, epId, ep.optString("container_extension", "mp4")),
                )
            }.sortedBy { it.episode }
            Season(key.toIntOrNull() ?: 0, episodes)
        }.sortedBy { it.number }.toList()
        return SeriesDetail(series, seasons)
    }

    /** Normaliza a nota do provedor (ex.: "6.666") para uma casa decimal ("6.7"). Nota ausente ou 0 vira nulo. */
    fun formatRating(raw: String): String? {
        val value = raw.trim().toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        return String.format(java.util.Locale.US, "%.1f", value)
    }

    /** Limpa títulos sujos do provedor ("Série - S01E01 - Nome" → "Nome"; sem nome → "Episódio N"). */
    private fun cleanEpisodeTitle(raw: String, episodeNum: String): String {
        val tail = raw.substringAfterLast(" - ").trim()
        val fallback = "Episódio ${episodeNum.ifBlank { "?" }}"
        return if (tail.isBlank() || tail.matches(Regex("(?i)s\\d+e\\d+"))) fallback else tail
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
