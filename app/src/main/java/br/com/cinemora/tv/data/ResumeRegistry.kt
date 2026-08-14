package br.com.cinemora.tv.data

import org.json.JSONArray
import org.json.JSONObject

/** Título em andamento, exibido na linha "Continuar assistindo" da tela inicial da TV. */
data class ResumeEntry(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
)

object ResumeRegistry {
    fun encode(items: List<ResumeEntry>): String = JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("streamUrl", item.streamUrl)
                    .put("posterUrl", item.posterUrl)
                    .put("positionMs", item.positionMs)
                    .put("durationMs", item.durationMs),
            )
        }
    }.toString()

    fun decode(json: String): List<ResumeEntry> = runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ResumeEntry(
                id = item.optString("id"),
                title = item.optString("title"),
                streamUrl = item.optString("streamUrl"),
                posterUrl = if (item.isNull("posterUrl")) null else item.optString("posterUrl").takeIf { it.isNotBlank() },
                positionMs = item.optLong("positionMs"),
                durationMs = item.optLong("durationMs"),
            )
        }
    }.getOrDefault(emptyList())

    /** O que está sendo assistido agora vai para o topo, sem duplicar. */
    fun upsert(items: List<ResumeEntry>, entry: ResumeEntry, limit: Int = 10): List<ResumeEntry> =
        (listOf(entry) + items.filterNot { it.id == entry.id }).take(limit)

    fun remove(items: List<ResumeEntry>, id: String): List<ResumeEntry> = items.filterNot { it.id == id }
}
