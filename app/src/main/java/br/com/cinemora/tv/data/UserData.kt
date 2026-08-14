package br.com.cinemora.tv.data

/** Dados locais do usuário: favoritos e histórico de assistidos (mais recentes primeiro). */
data class UserData(
    val favorites: Set<String> = emptySet(),
    val watched: List<String> = emptyList(),
) {
    fun toggleFavorite(id: String): UserData =
        if (id in favorites) copy(favorites = favorites - id) else copy(favorites = favorites + id)

    fun removeWatched(id: String): UserData = copy(watched = watched.filterNot { it == id })

    fun recordWatched(id: String, limit: Int = 20): UserData =
        copy(watched = (listOf(id) + watched.filterNot { it == id }).take(limit))
}
