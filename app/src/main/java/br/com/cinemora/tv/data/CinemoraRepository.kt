package br.com.cinemora.tv.data

import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.SeriesDetail
import br.com.cinemora.tv.model.Video

/** Conteúdo da Home: catálogo + os destaques (filme e série) sorteados para este cache. */
data class HomeContent(val catalog: Catalog, val featured: Video?, val featuredSeries: Series?)

/** Orquestra login (cache → rede), cache com validade e dados locais do usuário. */
class CinemoraRepository(
    private val store: LocalStore,
    private val client: ProviderClient = ProviderClient(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun savedCredentials(): Credentials? = store.credentials()

    /** Usa o cache em disco enquanto estiver dentro da validade; senão busca do servidor e salva. */
    fun loadCatalog(credentials: Credentials, forceRefresh: Boolean = false): HomeContent {
        if (!forceRefresh) {
            val cached = store.cachedCatalog()
            val savedAt = store.catalogSavedAt()
            if (cached != null && CachePolicy.isFresh(savedAt, clock(), store.cacheTtl())) {
                return homeContent(cached, savedAt)
            }
        }
        val fresh = client.loadCatalog(credentials)
        val now = clock()
        store.saveCredentials(credentials)
        store.saveCatalog(fresh, now)
        return homeContent(fresh, now)
    }

    /** Semente diferente para a série não cair sempre no mesmo índice do filme. */
    private fun homeContent(catalog: Catalog, seed: Long) = HomeContent(
        catalog = catalog,
        featured = FeaturedPicker.pick(catalog.movies, seed),
        featuredSeries = FeaturedPicker.pick(catalog.series, seed xor 0x5EE5),
    )

    fun loadSeriesDetail(credentials: Credentials, series: Series): SeriesDetail =
        client.loadSeriesDetail(credentials, series)

    fun loadMoviePlot(credentials: Credentials, videoId: String): String? =
        client.loadMoviePlot(credentials, videoId)

    fun userData(): UserData = store.userData()

    fun toggleFavorite(id: String): UserData = store.userData().toggleFavorite(id).also { store.saveUserData(it) }

    fun recordWatched(id: String): UserData = store.userData().recordWatched(id).also { store.saveUserData(it) }

    fun resumePosition(streamUrl: String): Long = store.position(streamUrl)

    fun clearPosition(streamUrl: String) = store.clearPosition(streamUrl)

    fun isStreamWatched(streamUrl: String): Boolean = store.isStreamWatched(streamUrl)

    fun resumeEntries(): List<ResumeEntry> = store.resumeEntries()

    fun openAiKey(): String? = store.openAiKey()

    fun saveOpenAiKey(key: String) = store.saveOpenAiKey(key)

    fun finishedStream(): String? = store.finishedStream()

    fun clearFinishedStream() = store.clearFinishedStream()

    fun removeWatched(id: String): UserData = store.userData().removeWatched(id).also { store.saveUserData(it) }

    fun sortOrder(): SortOrder = store.sortOrder()

    fun setSortOrder(order: SortOrder) = store.setSortOrder(order)

    fun cacheTtl(): CacheTtl = store.cacheTtl()

    fun setCacheTtl(ttl: CacheTtl) = store.setCacheTtl(ttl)

    fun clearCache() = store.clearCatalog()

    /** Sair / trocar conta: apaga credenciais e cache. */
    fun logout() {
        store.clearCredentials()
        store.clearCatalog()
    }
}
