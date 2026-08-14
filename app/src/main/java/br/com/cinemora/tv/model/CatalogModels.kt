package br.com.cinemora.tv.model

data class Credentials(val serverUrl: String, val username: String, val password: String)

data class Category(val id: String, val name: String)

data class Video(
    val id: String,
    val title: String,
    val categoryId: String,
    val coverUrl: String?,
    val streamUrl: String,
    val year: String? = null,
    val rating: String? = null,
    val synopsis: String? = null,
)

data class Channel(
    val id: String,
    val name: String,
    val categoryId: String,
    val logoUrl: String?,
    val streamUrl: String,
)

data class Series(
    val id: String,
    val title: String,
    val categoryId: String,
    val coverUrl: String?,
    val year: String? = null,
    val rating: String? = null,
    val synopsis: String? = null,
)

data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val streamUrl: String,
)

data class Season(val number: Int, val episodes: List<Episode>)

data class SeriesDetail(val series: Series, val seasons: List<Season>)

/** Catálogo completo do provedor: filmes (VOD), canais (TV ao vivo) e séries. */
data class Catalog(
    val movieCategories: List<Category> = emptyList(),
    val movies: List<Video> = emptyList(),
    val liveCategories: List<Category> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val seriesCategories: List<Category> = emptyList(),
    val series: List<Series> = emptyList(),
)
