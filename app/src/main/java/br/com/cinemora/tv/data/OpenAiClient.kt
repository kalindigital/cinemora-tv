package br.com.cinemora.tv.data

import br.com.cinemora.tv.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Resposta da conversa: texto exibido/falado e títulos sugeridos. */
data class ChatReply(val text: String, val titles: List<String>, val type: String = "ambos")

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
        return parseTitles(post("https://api.openai.com/v1/chat/completions", body.toString()))
    }

    private fun post(url: String, payload: String): String = String(postBytes(url, payload))

    private fun postBytes(url: String, payload: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
            val bytes = (if (ok) connection.inputStream else connection.errorStream)?.readBytes() ?: ByteArray(0)
            if (!ok) error(errorMessage(String(bytes), connection.responseCode))
            bytes
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return detail?.takeIf { it.isNotBlank() } ?: "A IA respondeu com erro $code."
    }

    /** Conversa com busca na web: o modelo decide quando pesquisar. */
    fun chat(messages: List<ChatMessage>): ChatReply {
        require(isConfigured()) { "Configure a chave da OpenAI." }
        val entrada = JSONArray()
        messages.forEach { message ->
            entrada.put(
                JSONObject()
                    .put("role", if (message.role == ChatRole.USER) "user" else "assistant")
                    .put("content", message.text),
            )
        }
        val body = JSONObject()
            .put("model", CHAT_MODEL)
            .put("instructions", CHAT_PROMPT)
            .put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
            .put("input", entrada)
            .put("text", JSONObject().put("format", CHAT_FORMAT))
        return parseChat(post("https://api.openai.com/v1/responses", body.toString()))
    }

    /** Pergunta avulsa em texto (resumo, veredito, perfil). Sem JSON, sem cartões. */
    fun askText(instrucoes: String, pedido: String, comWeb: Boolean = false): String {
        require(isConfigured()) { "Configure a chave da OpenAI." }
        val body = JSONObject()
            .put("model", CHAT_MODEL)
            .put("instructions", instrucoes)
            .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", pedido)))
        if (comWeb) body.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
        val resposta = post("https://api.openai.com/v1/responses", body.toString())
        val saida = JSONObject(resposta).optJSONArray("output") ?: return ""
        return (0 until saida.length())
            .map { saida.getJSONObject(it) }
            .firstOrNull { it.optString("type") == "message" }
            ?.optJSONArray("content")?.optJSONObject(0)?.optString("text").orEmpty()
    }

    /** Áudio da resposta pela OpenAI, quando o usuário escolhe essa voz. */
    fun speech(text: String, voice: String = "alloy", speed: Float = 1f): ByteArray? = runCatching {
        val body = JSONObject()
            .put("model", "gpt-4o-mini-tts")
            .put("voice", voice)
            .put("input", text.take(900))
            .put("speed", speed)
            .put("response_format", "mp3")
        postBytes("https://api.openai.com/v1/audio/speech", body.toString())
    }.getOrNull()

    companion object {
        private const val SYSTEM_PROMPT =
            "Você recomenda filmes e séries para um catálogo brasileiro de streaming. " +
                "Responda SOMENTE em JSON no formato {\"titulos\": [\"Título 1\", \"Título 2\"]}, " +
                "com 8 a 15 títulos reais que atendam ao pedido. " +
                "Use o título em português do Brasil quando existir, sem o ano e sem comentários."

        const val CHAT_MODEL = "gpt-4o-mini"

        val CHAT_FORMAT: JSONObject
            get() = JSONObject()
                .put("type", "json_schema")
                .put("name", "resposta_chat")
                .put("strict", true)
                .put(
                    "schema",
                    JSONObject()
                        .put("type", "object")
                        .put(
                            "properties",
                            JSONObject()
                                .put("resposta", JSONObject().put("type", "string"))
                                .put(
                                    "titulos",
                                    JSONObject().put("type", "array")
                                        .put("items", JSONObject().put("type", "string")),
                                )
                                .put(
                                    "tipo",
                                    JSONObject().put("type", "string")
                                        .put("enum", JSONArray().put("filme").put("serie").put("ambos")),
                                ),
                        )
                        .put("required", JSONArray().put("resposta").put("titulos").put("tipo"))
                        .put("additionalProperties", false),
                )

        const val CHAT_PROMPT =
            "Você é o assistente do Cinemora, um app de TV. Converse em português do Brasil, " +
                "de forma direta e amigável, em no máximo 3 frases — o texto será lido em voz alta. " +
                "Pesquise na web quando a pergunta envolver lançamentos, novidades ou fatos recentes. " +
                "Em 'titulos' coloque os filmes ou séries citados (título em português quando existir, sem o ano); " +
                "deixe a lista vazia quando não estiver recomendando nada. " +
                "Em 'tipo' informe 'filme' se pediram filmes, 'serie' se pediram séries e 'ambos' quando não " +
                "especificarem; respeite isso também nos títulos sugeridos. " +
                "NUNCA escreva links ou endereços de sites. " +
                "Quando a mensagem trouxer '[Disponíveis no catálogo do usuário: ...]', trate esses títulos como " +
                "o que a pessoa já pode assistir agora e priorize-os na resposta. Não sugira alugar, comprar ou " +
                "assinar outro serviço; se o título realmente não estiver no catálogo, apenas diga que não encontrou."

        const val PROMPT_VEREDITO =
            "Você ajuda alguém a decidir se vale a pena assistir. Em no máximo 3 frases, em português do " +
                "Brasil, diga como o filme foi recebido pela crítica e pelo público e para quem ele serve. " +
                "NUNCA conte spoilers, reviravoltas ou o final. Não escreva links."

        const val PROMPT_RESUMO =
            "Você lembra alguém do que aconteceu numa série. Em no máximo 4 frases, em português do Brasil, " +
                "resuma os acontecimentos ATÉ o episódio indicado, sem revelar nada do episódio seguinte " +
                "nem do futuro da série. Não escreva links."

        const val PROMPT_PERFIL =
            "A partir dos títulos que a pessoa assistiu e favoritou, descreva o gosto dela em no máximo 2 " +
                "frases, em português do Brasil: gêneros, épocas e tipos de história que combinam com ela. " +
                "Escreva em segunda pessoa, direto, sem listar os títulos."

        /** Lê o texto da conversa e as sugestões da resposta da API de respostas. */
        fun parseChat(response: String): ChatReply {
            val saida = JSONObject(response).optJSONArray("output") ?: return ChatReply("", emptyList())
            val texto = (0 until saida.length())
                .map { saida.getJSONObject(it) }
                .firstOrNull { it.optString("type") == "message" }
                ?.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
            if (texto.isBlank()) return ChatReply("", emptyList())
            val conteudo = runCatching { JSONObject(texto) }.getOrNull()
                ?: return ChatReply(texto, emptyList())
            val titulos = conteudo.optJSONArray("titulos") ?: JSONArray()
            return ChatReply(
                text = conteudo.optString("resposta"),
                titles = List(titulos.length()) { titulos.optString(it) }.filter { it.isNotBlank() },
                type = conteudo.optString("tipo").ifBlank { "ambos" },
            )
        }

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
