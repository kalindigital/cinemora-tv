package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.SeriesDetail
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Cliente para um catálogo Xtream (filmes, canais ao vivo e séries) do usuário. */
class ProviderClient {
    fun loadCatalog(credentials: Credentials): Catalog {
        require(XtreamParser.isAuthenticated(request(credentials, null))) {
            "Não foi possível validar esta conta. Confira os dados e tente novamente."
        }
        return Catalog(
            movieCategories = section { XtreamParser.categories(request(credentials, "get_vod_categories")) },
            movies = section { XtreamParser.movies(request(credentials, "get_vod_streams"), credentials) },
            liveCategories = section { XtreamParser.categories(request(credentials, "get_live_categories")) },
            channels = section { XtreamParser.channels(request(credentials, "get_live_streams"), credentials) },
            seriesCategories = section { XtreamParser.categories(request(credentials, "get_series_categories")) },
            series = section { XtreamParser.series(request(credentials, "get_series")) },
        )
    }

    fun loadSeriesDetail(credentials: Credentials, series: Series): SeriesDetail {
        val json = request(credentials, "get_series_info", mapOf("series_id" to series.id))
        return XtreamParser.seriesDetail(json, series, credentials)
    }

    fun loadMoviePlot(credentials: Credentials, videoId: String): String? =
        XtreamParser.moviePlot(request(credentials, "get_vod_info", mapOf("vod_id" to videoId)))

    /** Cada seção é opcional: um provedor sem canais ou séries não deve derrubar o catálogo. */
    private fun <T> section(load: () -> List<T>): List<T> = runCatching(load).getOrDefault(emptyList())

    private fun request(credentials: Credentials, action: String?, extra: Map<String, String> = emptyMap()): String {
        val base = credentials.serverUrl.trim().trimEnd('/')
        require(base.startsWith("http://") || base.startsWith("https://")) {
            "Informe o servidor com http:// ou https://."
        }
        val query = buildList {
            add("username=${credentials.username.encode()}")
            add("password=${credentials.password.encode()}")
            action?.let { add("action=${it.encode()}") }
            extra.forEach { (key, value) -> add("${key.encode()}=${value.encode()}") }
        }.joinToString("&")
        val connection = (java.net.URL("$base/player_api.php?$query").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        return try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: error("O servidor não retornou uma resposta válida.")
        } finally {
            connection.disconnect()
        }
    }

    private fun String.encode() = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}
