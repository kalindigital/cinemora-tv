package br.com.cinemora.tv

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.com.cinemora.tv.data.AiMatches
import br.com.cinemora.tv.data.CacheTtl
import br.com.cinemora.tv.data.CatalogMatcher
import br.com.cinemora.tv.data.CinemoraRepository
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.data.UpdateInfo
import br.com.cinemora.tv.data.UpdateService
import br.com.cinemora.tv.data.OpenAiClient
import br.com.cinemora.tv.data.Recommendations
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.data.UserData
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.SeriesDetail
import br.com.cinemora.tv.model.Video
import java.util.concurrent.Executors

sealed interface AppState {
    data object Login : AppState
    data object Loading : AppState
    data class Home(val catalog: Catalog, val featured: Video?, val featuredSeries: Series?, val userData: UserData) : AppState
    data class Error(val message: String) : AppState
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState
    data class Failed(val message: String) : UpdateState
}

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Loaded(val query: String, val matches: AiMatches, val suggestions: List<String>) : AiState
    data class Failed(val message: String) : AiState
}

sealed interface DetailState {
    data object Idle : DetailState
    data object Loading : DetailState
    data class Loaded(val detail: SeriesDetail) : DetailState
    data class Failed(val message: String) : DetailState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CinemoraRepository(LocalStore(app))
    private val openAi = OpenAiClient(keyProvider = { repo.openAiKey() })
    private val updates = UpdateService(app)
    var state: AppState by mutableStateOf(AppState.Login)
        private set
    var seriesDetail: DetailState by mutableStateOf(DetailState.Idle)
        private set
    var cacheTtl: CacheTtl by mutableStateOf(repo.cacheTtl())
        private set
    var moviePlot: String? by mutableStateOf(null)
        private set
    var featuredPlot: String? by mutableStateOf(null)
        private set
    var aiState: AiState by mutableStateOf(AiState.Idle)
        private set
    var sortOrder: SortOrder by mutableStateOf(repo.sortOrder())
        private set
    /** Posição salva do filme aberto (0 = ainda não assistido). */
    var resumeMs: Long by mutableStateOf(0L)
        private set
    var recommendations: List<Video> by mutableStateOf(emptyList())
        private set
    var hasOpenAiKey: Boolean by mutableStateOf(false)
        private set
    var updateState: UpdateState by mutableStateOf(UpdateState.Idle)
        private set
    private var openedMovie: Video? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        autoLogin()
        hasOpenAiKey = openAi.isConfigured()
        checkForUpdate(silencioso = true)
    }

    /** Consulta o último release do GitHub; silencioso não reclama quando não há novidade. */
    fun checkForUpdate(silencioso: Boolean = false) {
        if (!updates.isConfigured()) {
            if (!silencioso) updateState = UpdateState.Failed("Configure GITHUB_REPO em local.properties.")
            return
        }
        executor.execute {
            val info = updates.check()
            mainHandler.post {
                updateState = when {
                    info != null -> UpdateState.Available(info)
                    silencioso -> UpdateState.Idle
                    else -> UpdateState.Failed("Você já está na versão mais recente.")
                }
            }
        }
    }

    /** Baixa o APK do release e entrega ao instalador do sistema. */
    fun downloadUpdate() {
        val info = (updateState as? UpdateState.Available)?.info ?: return
        updateState = UpdateState.Downloading(info, 0)
        executor.execute {
            val arquivo = updates.download(info) { percent ->
                mainHandler.post {
                    if (updateState is UpdateState.Downloading) updateState = UpdateState.Downloading(info, percent)
                }
            }
            mainHandler.post {
                if (arquivo == null) {
                    updateState = UpdateState.Failed("Não consegui baixar a atualização.")
                } else {
                    updates.install(arquivo)
                    updateState = UpdateState.Idle
                }
            }
        }
    }

    fun dismissUpdate() { updateState = UpdateState.Idle }

    fun account(): Credentials? = repo.savedCredentials()

    private fun autoLogin() {
        val credentials = repo.savedCredentials() ?: return
        loadInto(credentials, forceRefresh = false)
    }

    fun signIn(server: String, username: String, password: String) {
        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            state = AppState.Error("Preencha servidor, usuário e senha.")
            return
        }
        loadInto(Credentials(server, username, password), forceRefresh = true)
    }

    fun refresh() {
        val credentials = repo.savedCredentials() ?: return
        loadInto(credentials, forceRefresh = true)
    }

    private fun loadInto(credentials: Credentials, forceRefresh: Boolean) {
        state = AppState.Loading
        featuredPlot = null
        executor.execute {
            val loaded = runCatching { repo.loadCatalog(credentials, forceRefresh) }
            val result = loaded.fold(
                onSuccess = { AppState.Home(it.catalog, it.featured, it.featuredSeries, repo.userData()) },
                onFailure = { AppState.Error(it.message ?: "Não foi possível carregar o catálogo.") },
            )
            mainHandler.post { state = result; onPlaybackFinished() }
            // A sinopse do destaque só existe no get_vod_info; busca em seguida para não atrasar a Home.
            loaded.getOrNull()?.featured?.let { featured ->
                val plot = runCatching { repo.loadMoviePlot(credentials, featured.id) }.getOrNull()
                mainHandler.post { featuredPlot = plot ?: featured.synopsis }
            }
        }
    }

    fun toggleFavorite(id: String) {
        val updated = repo.toggleFavorite(id)
        (state as? AppState.Home)?.let { state = it.copy(userData = updated) }
    }

    fun recordWatched(videoId: String) {
        executor.execute {
            val updated = repo.recordWatched(videoId)
            mainHandler.post { (state as? AppState.Home)?.let { state = it.copy(userData = updated) } }
        }
    }

    fun loadSeriesDetail(series: Series) {
        seriesDetail = DetailState.Loading
        executor.execute {
            val credentials = repo.savedCredentials()
            val result = if (credentials == null) {
                DetailState.Failed("Sessão expirada. Entre novamente.")
            } else {
                runCatching { repo.loadSeriesDetail(credentials, series) }
                    .fold(onSuccess = { DetailState.Loaded(it) }, onFailure = { DetailState.Failed(it.message ?: "Não foi possível carregar a série.") })
            }
            mainHandler.post { seriesDetail = result }
        }
    }

    fun clearSeriesDetail() { seriesDetail = DetailState.Idle }

    fun loadMoviePlot(video: Video) {
        openedMovie = video
        moviePlot = null
        resumeMs = repo.resumePosition(video.streamUrl)
        executor.execute {
            val credentials = repo.savedCredentials()
            val plot = if (credentials == null) null else runCatching { repo.loadMoviePlot(credentials, video.id) }.getOrNull()
            mainHandler.post { moviePlot = plot ?: video.synopsis ?: "" }
        }
    }

    /** Posição salva de qualquer stream (usada pela lista de episódios). */
    fun resumePositionOf(streamUrl: String): Long = repo.resumePosition(streamUrl)

    fun isStreamWatched(streamUrl: String): Boolean = repo.isStreamWatched(streamUrl)

    /**
     * Ao terminar um filme, a Home oferece títulos parecidos. O sinal vem do disco porque a
     * TV costuma destruir esta Activity enquanto o player está em cena.
     */
    fun onPlaybackFinished() {
        val home = state as? AppState.Home ?: return
        val terminado = repo.finishedStream() ?: return
        val movie = openedMovie?.takeIf { it.streamUrl == terminado }
            ?: home.catalog.movies.firstOrNull { it.streamUrl == terminado }
            ?: return
        repo.clearFinishedStream()
        recommendations = Recommendations.related(home.catalog, movie, home.userData.watched.toSet())
    }

    fun clearRecommendations() { recommendations = emptyList() }

    fun clearMoviePlot() { moviePlot = null; resumeMs = 0L }

    /** Tira do "Continuar assistindo" e descarta a posição salva. */
    fun removeWatched(video: Video) {
        val updated = repo.removeWatched(video.id)
        repo.clearPosition(video.streamUrl)
        resumeMs = 0L
        (state as? AppState.Home)?.let { state = it.copy(userData = updated) }
    }

    /** Pede recomendações à IA e casa os títulos sugeridos com o catálogo local. */
    fun askAi(query: String) {
        val request = query.trim()
        if (request.isBlank()) return
        if (!openAi.isConfigured()) {
            aiState = AiState.Failed("Configure a chave da OpenAI em Definições — é rápido, por QR code.")
            return
        }
        val catalog = (state as? AppState.Home)?.catalog ?: return
        aiState = AiState.Loading
        executor.execute {
            val result = runCatching { openAi.recommend(request) }
                .fold(
                    onSuccess = { titles -> AiState.Loaded(request, CatalogMatcher.match(titles, catalog), titles) },
                    onFailure = { AiState.Failed(it.message ?: "Não foi possível falar com a IA.") },
                )
            mainHandler.post { aiState = result }
        }
    }

    fun clearAi() { aiState = AiState.Idle }

    fun saveOpenAiKey(key: String) { repo.saveOpenAiKey(key); hasOpenAiKey = openAi.isConfigured() }

    fun changeSortOrder(order: SortOrder) { repo.setSortOrder(order); sortOrder = order }
    fun changeCacheTtl(ttl: CacheTtl) { repo.setCacheTtl(ttl); cacheTtl = ttl }
    fun clearCache() { executor.execute { repo.clearCache() } }
    fun logout() { repo.logout(); seriesDetail = DetailState.Idle; state = AppState.Login }
    fun returnToLogin() { state = AppState.Login }

    override fun onCleared() { executor.shutdownNow() }
}
