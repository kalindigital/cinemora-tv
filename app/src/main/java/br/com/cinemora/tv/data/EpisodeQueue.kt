package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Episode
import br.com.cinemora.tv.model.Season

/** Próximos episódios a partir do atual, atravessando temporadas. */
object EpisodeQueue {
    fun upcoming(seasons: List<Season>, currentEpisodeId: String): List<Episode> {
        val todos = emOrdem(seasons)
        val atual = todos.indexOfFirst { it.id == currentEpisodeId }
        return if (atual < 0) emptyList() else todos.drop(atual + 1)
    }

    /**
     * Onde o "Continuar" da série deve levar: o episódio em andamento, senão o primeiro
     * não assistido. Série toda vista recomeça do início.
     */
    fun resumeTarget(
        seasons: List<Season>,
        watched: (String) -> Boolean,
        position: (String) -> Long,
    ): Episode? {
        val todos = emOrdem(seasons)
        return todos.firstOrNull { position(it.streamUrl) > 0 }
            ?: todos.firstOrNull { !watched(it.streamUrl) }
            ?: todos.firstOrNull()
    }

    private fun emOrdem(seasons: List<Season>) =
        seasons.sortedBy { it.number }.flatMap { season -> season.episodes.sortedBy { it.episode } }
}
