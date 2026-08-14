package br.com.cinemora.tv.data

import org.json.JSONArray
import org.json.JSONObject

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String, val titles: List<String>)

data class ChatSession(val id: String, val title: String, val updatedAt: Long, val messages: List<ChatMessage>)

/** Conversas com a IA, guardadas separadas para o contexto não crescer sem limite. */
object ChatStore {
    fun encode(sessions: List<ChatSession>): String = JSONArray().apply {
        sessions.forEach { session ->
            put(
                JSONObject()
                    .put("id", session.id)
                    .put("title", session.title)
                    .put("updatedAt", session.updatedAt)
                    .put(
                        "messages",
                        JSONArray().apply {
                            session.messages.forEach { message ->
                                put(
                                    JSONObject()
                                        .put("role", message.role.name)
                                        .put("text", message.text)
                                        .put("titles", JSONArray(message.titles)),
                                )
                            }
                        },
                    ),
            )
        }
    }.toString()

    fun decode(json: String): List<ChatSession> = runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val messages = item.optJSONArray("messages") ?: JSONArray()
            ChatSession(
                id = item.optString("id"),
                title = item.optString("title"),
                updatedAt = item.optLong("updatedAt"),
                messages = List(messages.length()) { position ->
                    val message = messages.getJSONObject(position)
                    val titles = message.optJSONArray("titles") ?: JSONArray()
                    ChatMessage(
                        role = runCatching { ChatRole.valueOf(message.optString("role")) }.getOrDefault(ChatRole.USER),
                        text = message.optString("text"),
                        titles = List(titles.length()) { titles.optString(it) },
                    )
                },
            )
        }
    }.getOrDefault(emptyList())

    /** O título da conversa é a primeira pergunta, encurtada para caber na lista. */
    fun titleFrom(question: String, limit: Int = 32): String {
        val limpa = question.trim()
        return if (limpa.length <= limit) limpa else limpa.take(limit).trimEnd() + "…"
    }

    fun upsert(sessions: List<ChatSession>, session: ChatSession, limit: Int = 20): List<ChatSession> =
        (listOf(session) + sessions.filterNot { it.id == session.id }).take(limit)

    /** Só as últimas mensagens vão ao modelo: história inteira encareceria cada pergunta. */
    fun lastMessages(messages: List<ChatMessage>, limit: Int = 10): List<ChatMessage> = messages.takeLast(limit)
}
