package br.com.cinemora.tv.data

import android.content.Context
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Credentials
import java.io.File

/**
 * Persistência local: credenciais (para auto-login), preferências de cache,
 * catálogo em disco e dados do usuário (favoritos/assistidos).
 *
 * Observação: as credenciais ficam em SharedPreferences simples. Para um app de TV
 * pessoal é aceitável; um endurecimento futuro seria EncryptedSharedPreferences.
 */
class LocalStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("cinemora", Context.MODE_PRIVATE)
    private val catalogFile: File get() = File(app.filesDir, "catalog.json")

    fun saveCredentials(c: Credentials) {
        prefs.edit().putString(KEY_SERVER, c.serverUrl).putString(KEY_USER, c.username).putString(KEY_PASS, c.password).apply()
    }

    fun credentials(): Credentials? {
        val server = prefs.getString(KEY_SERVER, null) ?: return null
        val user = prefs.getString(KEY_USER, null) ?: return null
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        return Credentials(server, user, pass)
    }

    fun clearCredentials() {
        prefs.edit().remove(KEY_SERVER).remove(KEY_USER).remove(KEY_PASS).apply()
    }

    fun cacheTtl(): CacheTtl =
        runCatching { CacheTtl.valueOf(prefs.getString(KEY_TTL, "") ?: "") }.getOrDefault(CacheTtl.TWELVE_HOURS)

    fun setCacheTtl(ttl: CacheTtl) {
        prefs.edit().putString(KEY_TTL, ttl.name).apply()
    }

    /** Chave da OpenAI enviada pelo celular; tem prioridade sobre a do build. */
    fun openAiKey(): String? = prefs.getString(KEY_OPENAI, null)?.takeIf { it.isNotBlank() }

    fun saveOpenAiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI, key.trim()).apply()
    }

    fun clearOpenAiKey() {
        prefs.edit().remove(KEY_OPENAI).apply()
    }

    fun chatSessions(): List<ChatSession> = ChatStore.decode(prefs.getString(KEY_CHATS, "[]").orEmpty())

    fun saveChatSessions(sessions: List<ChatSession>) {
        prefs.edit().putString(KEY_CHATS, ChatStore.encode(sessions)).apply()
    }

    fun voiceMode(): VoiceMode =
        runCatching { VoiceMode.valueOf(prefs.getString(KEY_VOICE, "") ?: "") }.getOrDefault(VoiceMode.GOOGLE)

    fun setVoiceMode(mode: VoiceMode) {
        prefs.edit().putString(KEY_VOICE, mode.name).apply()
    }

    fun openAiVoice(): String = prefs.getString(KEY_OPENAI_VOICE, "alloy") ?: "alloy"

    fun setOpenAiVoice(voice: String) {
        prefs.edit().putString(KEY_OPENAI_VOICE, voice).apply()
    }

    fun voiceSpeed(): VoiceSpeed =
        runCatching { VoiceSpeed.valueOf(prefs.getString(KEY_VOICE_SPEED, "") ?: "") }.getOrDefault(VoiceSpeed.NORMAL)

    fun setVoiceSpeed(speed: VoiceSpeed) {
        prefs.edit().putString(KEY_VOICE_SPEED, speed.name).apply()
    }

    fun knownIds(): Set<String> = prefs.getStringSet(KEY_KNOWN, emptySet()).orEmpty()

    fun saveKnownIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_KNOWN, ids).apply()
    }

    fun familyMode(): Boolean = prefs.getBoolean(KEY_FAMILY, false)

    fun setFamilyMode(ativo: Boolean) {
        prefs.edit().putBoolean(KEY_FAMILY, ativo).apply()
    }

    fun watchlist(): List<String> =
        prefs.getString(KEY_WATCHLIST, "").orEmpty().split("|").filter { it.isNotBlank() }

    fun saveWatchlist(itens: List<String>) {
        prefs.edit().putString(KEY_WATCHLIST, itens.joinToString("|")).apply()
    }

    fun tasteProfile(): String? = prefs.getString(KEY_TASTE, null)?.takeIf { it.isNotBlank() }

    fun saveTasteProfile(perfil: String) {
        prefs.edit().putString(KEY_TASTE, perfil).apply()
    }

    fun liveEnabled(): Boolean = prefs.getBoolean(KEY_LIVE, true)

    fun setLiveEnabled(ativo: Boolean) {
        prefs.edit().putBoolean(KEY_LIVE, ativo).apply()
    }

    fun typewriter(): Boolean = prefs.getBoolean(KEY_TYPEWRITER, true)

    fun setTypewriter(ativo: Boolean) {
        prefs.edit().putBoolean(KEY_TYPEWRITER, ativo).apply()
    }

    fun sortOrder(): SortOrder =
        runCatching { SortOrder.valueOf(prefs.getString(KEY_SORT, "") ?: "") }.getOrDefault(SortOrder.PADRAO)

    fun setSortOrder(order: SortOrder) {
        prefs.edit().putString(KEY_SORT, order.name).apply()
    }

    fun saveCatalog(catalog: Catalog, nowMillis: Long) {
        catalogFile.writeText(CatalogJson.encode(catalog))
        prefs.edit().putLong(KEY_SAVED_AT, nowMillis).apply()
    }

    fun cachedCatalog(): Catalog? =
        catalogFile.takeIf { it.exists() }?.let { runCatching { CatalogJson.decode(it.readText()) }.getOrNull() }

    fun catalogSavedAt(): Long = prefs.getLong(KEY_SAVED_AT, 0L)

    fun clearCatalog() {
        catalogFile.delete()
        prefs.edit().remove(KEY_SAVED_AT).apply()
    }

    /** Posição de retomada por título (chave derivada da URL do stream). */
    fun savePosition(streamUrl: String, positionMs: Long) {
        prefs.edit().putLong(positionKey(streamUrl), positionMs).apply()
    }

    fun position(streamUrl: String): Long = prefs.getLong(positionKey(streamUrl), 0L)

    fun clearPosition(streamUrl: String) {
        prefs.edit().remove(positionKey(streamUrl)).apply()
    }

    private fun positionKey(streamUrl: String) = "pos:${streamUrl.hashCode()}"

    /** Episódios/filmes concluídos, para marcar na lista. */
    fun markStreamWatched(streamUrl: String) {
        val atuais = prefs.getStringSet(KEY_WATCHED_STREAMS, emptySet()).orEmpty().toMutableSet()
        atuais += streamUrl.hashCode().toString()
        prefs.edit().putStringSet(KEY_WATCHED_STREAMS, atuais).apply()
    }

    /** Títulos em andamento, espelhados na linha "Continuar assistindo" da TV. */
    fun resumeEntries(): List<ResumeEntry> =
        ResumeRegistry.decode(prefs.getString(KEY_RESUME, "[]").orEmpty())

    fun saveResumeEntries(items: List<ResumeEntry>) {
        prefs.edit().putString(KEY_RESUME, ResumeRegistry.encode(items)).apply()
    }

    /** Sinal do player para a Home oferecer recomendações; sobrevive à Activity ser destruída. */
    fun saveFinishedStream(streamUrl: String) {
        prefs.edit().putString(KEY_FINISHED, streamUrl).apply()
    }

    fun finishedStream(): String? = prefs.getString(KEY_FINISHED, null)

    fun clearFinishedStream() {
        prefs.edit().remove(KEY_FINISHED).apply()
    }

    fun isStreamWatched(streamUrl: String): Boolean =
        streamUrl.hashCode().toString() in prefs.getStringSet(KEY_WATCHED_STREAMS, emptySet()).orEmpty()

    fun userData(): UserData {
        val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()
        val watched = prefs.getString(KEY_WATCHED, "").orEmpty().split(",").filter { it.isNotBlank() }
        return UserData(favorites, watched)
    }

    fun saveUserData(data: UserData) {
        prefs.edit()
            .putStringSet(KEY_FAVORITES, data.favorites)
            .putString(KEY_WATCHED, data.watched.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_USER = "user"
        const val KEY_PASS = "pass"
        const val KEY_TTL = "cache_ttl"
        const val KEY_SAVED_AT = "catalog_saved_at"
        const val KEY_FAVORITES = "favorites"
        const val KEY_WATCHED = "watched"
        const val KEY_SORT = "sort_order"
        const val KEY_WATCHED_STREAMS = "watched_streams"
        const val KEY_FINISHED = "finished_stream"
        const val KEY_OPENAI = "openai_key"
        const val KEY_RESUME = "resume_entries"
        const val KEY_CHATS = "chat_sessions"
        const val KEY_VOICE = "voice_mode"
        const val KEY_OPENAI_VOICE = "openai_voice"
        const val KEY_VOICE_SPEED = "voice_speed"
        const val KEY_TYPEWRITER = "typewriter"
        const val KEY_LIVE = "live_enabled"
        const val KEY_KNOWN = "known_ids"
        const val KEY_FAMILY = "family_mode"
        const val KEY_WATCHLIST = "watchlist"
        const val KEY_TASTE = "taste_profile"
    }
}
