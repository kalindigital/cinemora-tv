package br.com.cinemora.tv.data

import br.com.cinemora.tv.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Recomendação por IA. O catálogo tem dezenas de milhares de títulos, então não é enviado:
 * pedimos títulos ao modelo e casamos com o catálogo local via [CatalogMatcher].
 */
class OpenAiClient(
    private val keyProvider: () -> String? = { null },
    private val fallbackKey: String = BuildConfig.OPENAI_API_KEY,
    private val organization: String = BuildConfig.OPENAI_ORGANIZATION,
    private val project: String = BuildConfig.OPENAI_PROJECT,
    private val model: String = "gpt-4o-mini",
) {
    private val apiKey: String get() = keyProvider()?.takeIf { it.isNotBlank() } ?: fallbackKey

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun recommend(request: String): List<String> {
        require(isConfigured()) { "Configure a OPENAI_API_KEY em local.properties." }
        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.8)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", request)),
            )
        return parseTitles(post(body.toString()))
    }

    private fun post(payload: String): String {
        val connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            if (organization.isNotBlank()) setRequestProperty("OpenAI-Organization", organization)
            if (project.isNotBlank()) setRequestProperty("OpenAI-Project", project)
        }
        return try {
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val ok = connection.responseCode in 200..299
            val text = (if (ok) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (!ok) error(errorMessage(text, connection.responseCode))
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return detail?.takeIf { it.isNotBlank() } ?: "A IA respondeu com erro $code."
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "Você recomenda filmes e séries para um catálogo brasileiro de streaming. " +
                "Responda SOMENTE em JSON no formato {\"titulos\": [\"Título 1\", \"Título 2\"]}, " +
                "com 8 a 15 títulos reais que atendam ao pedido. " +
                "Use o título em português do Brasil quando existir, sem o ano e sem comentários."

        /** Extrai os títulos da resposta do chat (o conteúdo é um JSON dentro do JSON). */
        fun parseTitles(response: String): List<String> {
            val content = JSONObject(response)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank()) return emptyList()
            val titles = JSONObject(content).optJSONArray("titulos") ?: return emptyList()
            return List(titles.length()) { titles.optString(it) }.filter { it.isNotBlank() }
        }
    }
}
