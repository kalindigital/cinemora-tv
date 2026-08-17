package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Category
import br.com.cinemora.tv.model.Channel
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.Video
import org.json.JSONArray
import org.json.JSONObject

/** Serializa o catálogo para o cache em disco e de volta. */
object CatalogJson {
    fun encode(catalog: Catalog): String = JSONObject()
        .put("movieCategories", categoriesJson(catalog.movieCategories))
        .put("movies", moviesJson(catalog.movies))
        .put("liveCategories", categoriesJson(catalog.liveCategories))
        .put("channels", channelsJson(catalog.channels))
        .put("seriesCategories", categoriesJson(catalog.seriesCategories))
        .put("series", seriesJson(catalog.series))
        .toString()

    fun decode(json: String): Catalog {
        val root = JSONObject(json)
        return Catalog(
            movieCategories = root.optJSONArray("movieCategories").map(::category),
            movies = root.optJSONArray("movies").map(::movie),
            liveCategories = root.optJSONArray("liveCategories").map(::category),
            channels = root.optJSONArray("channels").map(::channel),
            seriesCategories = root.optJSONArray("seriesCategories").map(::category),
            series = root.optJSONArray("series").map(::series),
        )
    }

    private fun categoriesJson(items: List<Category>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
    }

    private fun moviesJson(items: List<Video>) = JSONArray().apply {
        items.forEach {
            put(
                JSONObject()
                    .put("id", it.id).put("title", it.title).put("categoryId", it.categoryId)
                    .put("coverUrl", it.coverUrl).put("streamUrl", it.streamUrl)
                    .put("year", it.year).put("rating", it.rating).put("synopsis", it.synopsis),
            )
        }
    }

    private fun channelsJson(items: List<Channel>) = JSONArray().apply {
        items.forEach {
            put(
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("categoryId", it.categoryId)
                    .put("logoUrl", it.logoUrl).put("streamUrl", it.streamUrl),
            )
        }
    }

    private fun seriesJson(items: List<Series>) = JSONArray().apply {
        items.forEach {
            put(
                JSONObject()
                    .put("id", it.id).put("title", it.title).put("categoryId", it.categoryId)
                    .put("coverUrl", it.coverUrl).put("year", it.year)
                    .put("rating", it.rating).put("synopsis", it.synopsis)
                    .put("backdropUrl", it.backdropUrl),
            )
        }
    }

    private fun category(o: JSONObject) = Category(o.optString("id"), o.optString("name"))

    private fun movie(o: JSONObject) = Video(
        id = o.optString("id"), title = XtreamParser.cleanTitle(o.optString("title")), categoryId = o.optString("categoryId"),
        coverUrl = o.optStringOrNull("coverUrl"), streamUrl = o.optString("streamUrl"),
        year = o.optStringOrNull("year"), rating = o.optStringOrNull("rating"), synopsis = o.optStringOrNull("synopsis"),
    )

    private fun channel(o: JSONObject) = Channel(
        id = o.optString("id"), name = o.optString("name"), categoryId = o.optString("categoryId"),
        logoUrl = o.optStringOrNull("logoUrl"), streamUrl = o.optString("streamUrl"),
    )

    private fun series(o: JSONObject) = Series(
        id = o.optString("id"), title = XtreamParser.cleanTitle(o.optString("title")), categoryId = o.optString("categoryId"),
        coverUrl = o.optStringOrNull("coverUrl"), year = o.optStringOrNull("year"),
        rating = o.optStringOrNull("rating"), synopsis = o.optStringOrNull("synopsis"),
        backdropUrl = o.optStringOrNull("backdropUrl"),
    )

    private fun <T> JSONArray?.map(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return List(length()) { transform(getJSONObject(it)) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
