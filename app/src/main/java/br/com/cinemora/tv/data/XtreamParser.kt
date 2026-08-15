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

/** Complemento do filme: sinopse, arte 16:9, gênero e duração. */
data class MovieExtra(
    val plot: String?,
    val backdrop: String?,
    val genre: String?,
    val duration: String?,
    val cast: String? = null,
    val director: String? = null,
)

/** Converte as respostas JSON do player_api Xtream nos modelos do app. Funções puras. */
object XtreamParser {
    fun isAuthenticated(json: String): Boolean =
        runCatching { JSONObject(json).optJSONObject("user_info")?.optInt("auth") == 1 }.getOrDefault(false)

    /** Dados extras do filme, usados no destaque grande da lista. */
    fun movieExtra(json: String): MovieExtra {
        val info = runCatching { JSONObject(json).optJSONObject("info") }.getOrNull()
            ?: return MovieExtra(null, null, null, null)
        val backdrop = when (val bruto = info.opt("backdrop_path")) {
            is JSONArray -> (0 until bruto.length()).map { bruto.optString(it) }.firstOrNull { it.isNotBlank() }
            is String -> bruto.takeIf { it.isNotBlank() }
            else -> null
        }
        return MovieExtra(
            plot = info.optStringOrNull("plot") ?: info.optStringOrNull("description"),
            backdrop = backdrop,
            genre = info.optStringOrNull("genre"),
            duration = formatDuration(info.optString("duration")),
            cast = info.optStringOrNull("cast"),
            director = info.optStringOrNull("director"),
        )
    }

    /** O detalhe da série tem os mesmos campos do filme, menos a duração: essa é por episódio. */
    fun seriesExtra(json: String): MovieExtra {
        val base = movieExtra(json)
        val minutos = runCatching {
            JSONObject(json).optJSONObject("info")?.optString("episode_run_time")
        }.getOrNull()?.trim()?.toIntOrNull()
        return base.copy(duration = minutos?.let { "${it}min" })
    }

    /** "2:04:00" -> "2h04"; "0:45:00" -> "45min". */
    private fun formatDuration(raw: String): String? {
        val partes = raw.split(":").mapNotNull { it.trim().toIntOrNull() }
        if (partes.size < 2) return null
        val horas = partes[0]
        val minutos = partes[1]
        return if (horas > 0) "%dh%02d".format(horas, minutos) else "${minutos}min"
    }

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
        val extra = seriesExtra(json)
        val episodesObj = JSONObject(json).optJSONObject("episodes") ?: return SeriesDetail(series, emptyList(), extra)
        val seasons = episodesObj.keys().asSequence().mapNotNull { key ->
            val array = episodesObj.optJSONArray(key) ?: return@mapNotNull null
            val episodes = array.objects().map { ep ->
                val epId = ep.optString("id")
                val info = ep.optJSONObject("info")
                Episode(
                    id = epId,
                    title = cleanEpisodeTitle(ep.optString("title"), ep.optString("episode_num")),
                    season = key.toIntOrNull() ?: 0,
                    episode = ep.optString("episode_num").toIntOrNull() ?: 0,
                    streamUrl = StreamUrlBuilder.seriesEpisode(credentials, epId, ep.optString("container_extension", "mp4")),
                    plot = info?.optStringOrNull("plot"),
                    duration = info?.optString("duration")?.let(::formatDuration),
                    thumbUrl = info?.optStringOrNull("movie_image"),
                )
            }.sortedBy { it.episode }
            Season(key.toIntOrNull() ?: 0, episodes)
        }.sortedBy { it.number }.toList()
        return SeriesDetail(series, seasons, extra)
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
