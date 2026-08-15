package br.com.cinemora.tv

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import br.com.cinemora.tv.data.AiMatches
import br.com.cinemora.tv.data.CacheTtl
import br.com.cinemora.tv.data.CatalogMatcher
import br.com.cinemora.tv.data.CatalogNews
import br.com.cinemora.tv.data.CatalogSearch
import br.com.cinemora.tv.data.FamilyFilter
import br.com.cinemora.tv.data.FilaArte
import br.com.cinemora.tv.data.MovieExtra
import br.com.cinemora.tv.data.Watchlist
import br.com.cinemora.tv.data.CinemoraRepository
import br.com.cinemora.tv.data.SortOrder
import br.com.cinemora.tv.data.UpdateInfo
import br.com.cinemora.tv.data.UpdateService
import br.com.cinemora.tv.data.WatchNext
import br.com.cinemora.tv.data.ChatMessage
import br.com.cinemora.tv.data.ChatReply
import br.com.cinemora.tv.data.ChatRole
import br.com.cinemora.tv.data.ChatSession
import br.com.cinemora.tv.data.ChatStore
import br.com.cinemora.tv.data.OpenAiClient
import br.com.cinemora.tv.data.RealtimeEvent
import br.com.cinemora.tv.data.RealtimeSession
import br.com.cinemora.tv.data.Speaker
import br.com.cinemora.tv.data.VoiceMode
import br.com.cinemora.tv.data.VoiceSpeed
import br.com.cinemora.tv.data.Recommendations
import br.com.cinemora.tv.data.SemLinks
import br.com.cinemora.tv.data.ResumeEntry
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.data.UserData
import br.com.cinemora.tv.model.Catalog
import br.com.cinemora.tv.model.Credentials
import br.com.cinemora.tv.model.Series
import br.com.cinemora.tv.model.SeriesDetail
import br.com.cinemora.tv.model.Video
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

sealed interface AppState {
    data object Login : AppState
    data object Loading : AppState
    data class Home(
        val catalog: Catalog,
        val featured: Video?,
        val featuredSeries: Series?,
        val selecaoFilmes: List<Video>,
        val selecaoSeries: List<Series>,
        val userData: UserData,
    ) : AppState
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
    private val speaker = Speaker(
        app,
        openAi,
        onFalando = { falando -> speaking = falando },
        vozOpenAi = { openAiVoice },
        velocidade = { voiceSpeed },
    )
    var state: AppState by mutableStateOf(AppState.Login)
        private set
    var seriesDetail: DetailState by mutableStateOf(DetailState.Idle)
        private set
    var cacheTtl: CacheTtl by mutableStateOf(repo.cacheTtl())
        private set
    var moviePlot: String? by mutableStateOf(null)
        private set
    /** Gênero, duração, elenco e arte do filme aberto, para a tela de detalhe. */
    var movieDetailExtra: MovieExtra? by mutableStateOf(null)
        private set
    var featuredPlot: String? by mutableStateOf(null)
        private set
    /** Filme em foco na lista de filmes: alimenta a arte grande do topo. */
    var movieFocus: Video? by mutableStateOf(null)
        private set
    var movieFocusExtra: MovieExtra? by mutableStateOf(null)
        private set
    /** Série em foco na lista de séries, com o mesmo papel do destaque de filmes. */
    var seriesFocus: Series? by mutableStateOf(null)
        private set
    var seriesFocusExtra: MovieExtra? by mutableStateOf(null)
        private set
    val seriesArt = mutableStateMapOf<String, String>()
    /** Quanto já foi assistido de cada stream (0..1), para a barra nos cartões. */
    var progresso: Map<String, Float> by mutableStateOf(emptyMap())
        private set
    /** Arte 16:9 por filme, preenchida aos poucos conforme os cartões aparecem na tela. */
    val movieArt = mutableStateMapOf<String, String>()
    var aiState: AiState by mutableStateOf(AiState.Idle)
        private set
    /** Conversas com a IA: a atual e a lista para retomar. */
    var chatSessions: List<ChatSession> by mutableStateOf(emptyList())
        private set
    var currentChat: ChatSession? by mutableStateOf(null)
        private set
    var chatThinking: Boolean by mutableStateOf(false)
        private set
    var chatError: String? by mutableStateOf(null)
        private set
    var voiceMode: VoiceMode by mutableStateOf(VoiceMode.GOOGLE)
        private set
    var speaking: Boolean by mutableStateOf(false)
        private set
    var openAiVoice: String by mutableStateOf("alloy")
        private set
    var voiceSpeed: VoiceSpeed by mutableStateOf(VoiceSpeed.NORMAL)
        private set
    var typewriter: Boolean by mutableStateOf(true)
        private set
    var liveEnabled: Boolean by mutableStateOf(true)
        private set
    var familyMode: Boolean by mutableStateOf(false)
        private set
    /** Títulos que entraram no catálogo desde a última atualização. */
    var novidades: List<Video> by mutableStateOf(emptyList())
        private set
    var watchlist: List<String> by mutableStateOf(emptyList())
        private set
    var chegaram: List<Pair<String, Video>> by mutableStateOf(emptyList())
        private set
    var tasteProfile: String? by mutableStateOf(null)
        private set
    /** Texto avulso da IA (resumo da série, veredito do filme). */
    var insight: String? by mutableStateOf(null)
        private set
    var insightTitulo: String? by mutableStateOf(null)
        private set
    /** Conversa ao vivo: estado da linha e o que foi dito até agora. */
    var liveStatus: String? by mutableStateOf(null)
        private set
    var liveActive: Boolean by mutableStateOf(false)
        private set
    private var live: RealtimeSession? = null
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
    /** Último título em andamento, oferecido ao abrir o app. */
    var resumeEntry: ResumeEntry? by mutableStateOf(null)
        private set
    private var openedMovie: Video? = null
    private val executor = Executors.newSingleThreadExecutor()
    // Fila própria: a arte do destaque não pode esperar atrás do carregamento do catálogo.
    private val focoExecutor = Executors.newSingleThreadExecutor()
    private val extraCache = ConcurrentHashMap<String, MovieExtra>()
    private var focoSeq = 0
    private val seriesExtraCache = ConcurrentHashMap<String, MovieExtra>()
    private var focoSerieSeq = 0
    private val filaArte = FilaArte()
    @Volatile private var buscandoArte = true
    private var artesNaoSalvas = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        resumeEntry = repo.resumeEntries().firstOrNull()
        // Republicar reforça o cartão na tela inicial da TV caso a escrita anterior tenha falhado.
        resumeEntry?.let { WatchNext.update(app, it) }
        autoLogin()
        recarregarProgresso()
        iniciarBuscaDeArte()
        hasOpenAiKey = openAi.isConfigured()
        chatSessions = repo.chatSessions()
        voiceMode = repo.voiceMode()
        openAiVoice = repo.openAiVoice()
        voiceSpeed = repo.voiceSpeed()
        typewriter = repo.typewriter()
        liveEnabled = repo.liveEnabled()
        familyMode = repo.familyMode()
        watchlist = repo.watchlist()
        tasteProfile = repo.tasteProfile()
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

    fun dismissResume() { resumeEntry = null }

    fun refreshResume() { resumeEntry = repo.resumeEntries().firstOrNull() }

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
            loaded.getOrNull()?.let { conteudo ->
                val conhecidos = repo.knownIds()
                val novos = CatalogNews.novidades(conteudo.catalog, conhecidos)
                repo.saveKnownIds(CatalogNews.idsConhecidos(conteudo.catalog))
                val esperados = Watchlist.chegaram(repo.watchlist(), conteudo.catalog)
                mainHandler.post { novidades = novos; chegaram = esperados }
            }
            // A "Seleção do dia" é 16:9 fixo: pedir a arte antes evita o cartão abrir sem ela.
            loaded.getOrNull()?.selecaoFilmes?.forEach { filaArte.pedir(it.id) }
            val result = loaded.fold(
                onSuccess = {
                    val visivel = if (repo.familyMode()) FamilyFilter.apply(it.catalog) else it.catalog
                    AppState.Home(visivel, it.featured, it.featuredSeries, it.selecaoFilmes, it.selecaoSeries, repo.userData())
                },
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
        // O que o foco na fileira já buscou serve aqui: abre sem esperar a rede.
        movieDetailExtra = extraCache[video.id]
        moviePlot = movieDetailExtra?.plot
        resumeMs = repo.resumePosition(video.streamUrl)
        executor.execute {
            val credentials = repo.savedCredentials()
            val extra = extraCache[video.id]
                ?: credentials?.let { c -> runCatching { repo.loadMovieExtra(c, video.id) }.getOrNull() }
                    ?.also { extraCache[video.id] = it }
            mainHandler.post {
                movieDetailExtra = extra
                moviePlot = extra?.plot ?: video.synopsis ?: ""
            }
        }
    }

    /** Posição salva de qualquer stream (usada pela lista de episódios). */
    fun resumePositionOf(streamUrl: String): Long = repo.resumePosition(streamUrl)

    fun isStreamWatched(streamUrl: String): Boolean = repo.isStreamWatched(streamUrl)

    /**
     * Ao terminar um filme, a Home oferece títulos parecidos. O sinal vem do disco porque a
     * TV costuma destruir esta Activity enquanto o player está em cena.
     */
    private fun recarregarProgresso() {
        progresso = repo.resumeEntries()
            .filter { it.durationMs > 0 }
            .associate { it.streamUrl to (it.positionMs.toFloat() / it.durationMs).coerceIn(0f, 1f) }
    }

    fun onPlaybackFinished() {
        recarregarProgresso()
        val home = state as? AppState.Home ?: return
        val terminado = repo.finishedStream() ?: return
        val movie = openedMovie?.takeIf { it.streamUrl == terminado }
            ?: home.catalog.movies.firstOrNull { it.streamUrl == terminado }
            ?: return
        repo.clearFinishedStream()
        recommendations = Recommendations.related(home.catalog, movie, home.userData.watched.toSet())
    }

    fun clearRecommendations() { recommendations = emptyList() }

    /**
     * Foco na fileira de séries. O detalhe da série traz todos os episódios junto, então é
     * uma resposta pesada: buscamos só a série em foco, e uma única vez por série.
     */
    fun onSeriesFocused(series: Series?) {
        focoSerieSeq++
        val seq = focoSerieSeq
        seriesFocus = series
        if (series == null) {
            seriesFocusExtra = null
            return
        }
        val emCache = seriesExtraCache[series.id]
        seriesFocusExtra = emCache
        if (emCache != null) return
        mainHandler.postDelayed({
            if (seq != focoSerieSeq) return@postDelayed
            focoExecutor.execute {
                val credentials = repo.savedCredentials() ?: return@execute
                val extra = runCatching { repo.loadSeriesExtra(credentials, series.id) }.getOrNull() ?: return@execute
                seriesExtraCache[series.id] = extra
                mainHandler.post {
                    extra.backdrop?.let { seriesArt[series.id] = it }
                    if (seq == focoSerieSeq) seriesFocusExtra = extra
                }
            }
        }, ESPERA_FOCO_SERIE)
    }

    /**
     * O cartão pede a própria arte ao aparecer. Quem busca é uma única linha de trabalho,
     * do pedido mais recente para o mais antigo, para não disparar dezenas de chamadas
     * quando a fileira passa correndo.
     */
    fun onArtNeeded(video: Video) {
        if (movieArt.containsKey(video.id)) return
        // O destaque busca o mesmo get_vod_info ao focar: se já passou por aqui, nada a pedir.
        extraCache[video.id]?.backdrop?.let { movieArt[video.id] = it; return }
        filaArte.pedir(video.id)
    }

    private fun iniciarBuscaDeArte() {
        val doDisco = repo.artes()
        if (doDisco.isNotEmpty()) {
            movieArt.putAll(doDisco)
            filaArte.resolvidos(doDisco.keys)
        }
        thread(isDaemon = true, name = "cinemora-arte") {
            while (buscandoArte) {
                val id = filaArte.proximo()
                if (id == null) {
                    Thread.sleep(ESPERA_FILA)
                    continue
                }
                val credentials = repo.savedCredentials() ?: continue
                val extra = runCatching { repo.loadMovieExtra(credentials, id) }.getOrNull() ?: continue
                // Guardado inteiro: focar esse cartão depois não gasta outra chamada.
                extraCache[id] = extra
                val arte = extra.backdrop ?: continue
                mainHandler.post {
                    movieArt[id] = arte
                    // Gravar a cada arte castigaria o disco da TV; em lotes o custo some.
                    if (++artesNaoSalvas >= LOTE_ARTE) {
                        artesNaoSalvas = 0
                        val copia = movieArt.toMap()
                        focoExecutor.execute { repo.saveArtes(copia) }
                    }
                }
            }
        }
    }

    /**
     * Foco na fileira de filmes. O detalhe (sinopse, arte 16:9, duração) só existe no
     * get_vod_info, então é buscado depois que o foco para: percorrer a fileira dispararia
     * uma chamada por cartão e o provedor derruba a conexão.
     */
    fun onMovieFocused(video: Video?) {
        focoSeq++
        val seq = focoSeq
        movieFocus = video
        if (video == null) {
            movieFocusExtra = null
            return
        }
        val emCache = extraCache[video.id]
        movieFocusExtra = emCache
        if (emCache != null) return
        mainHandler.postDelayed({
            if (seq != focoSeq) return@postDelayed
            focoExecutor.execute {
                val credentials = repo.savedCredentials() ?: return@execute
                val extra = runCatching { repo.loadMovieExtra(credentials, video.id) }.getOrNull() ?: return@execute
                extraCache[video.id] = extra
                mainHandler.post { if (seq == focoSeq) movieFocusExtra = extra }
            }
        }, ESPERA_FOCO)
    }

    fun clearMoviePlot() { moviePlot = null; movieDetailExtra = null; resumeMs = 0L }

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

    fun changeVoiceMode(mode: VoiceMode) {
        repo.setVoiceMode(mode)
        voiceMode = mode
        if (mode == VoiceMode.MUDO) speaker.stop()
    }

    fun changeFamilyMode(ativo: Boolean) {
        repo.setFamilyMode(ativo)
        familyMode = ativo
        refresh()
    }

    fun addToWatchlist(titulo: String) {
        watchlist = Watchlist.adicionar(watchlist, titulo)
        repo.saveWatchlist(watchlist)
    }

    fun removeFromWatchlist(titulo: String) {
        watchlist = Watchlist.remover(watchlist, titulo)
        repo.saveWatchlist(watchlist)
        chegaram = chegaram.filterNot { it.first.equals(titulo, ignoreCase = true) }
    }

    fun dismissChegaram() { chegaram = emptyList() }

    /** "Vale a pena?" — opinião curta com base na crítica, sem spoiler. */
    fun askVerdict(video: Video) = pedirInsight(video.title) {
        openAi.askText(
            OpenAiClient.PROMPT_VEREDITO,
            "Filme: ${video.title}. Vale a pena assistir?",
            comWeb = true,
        )
    }

    /** "Onde eu parei?" — resumo até o episódio anterior. */
    fun askRecap(series: Series, season: Int, episode: Int) =
        pedirInsight("${series.title} — até T$season E${episode - 1}") {
            openAi.askText(
                OpenAiClient.PROMPT_RESUMO,
                "Série: ${series.title}. Resuma até a temporada $season, episódio ${episode - 1}.",
                comWeb = true,
            )
        }

    private fun pedirInsight(titulo: String, bloco: () -> String) {
        if (!openAi.isConfigured()) {
            insightTitulo = titulo
            insight = "Configure a chave da OpenAI em Definições."
            return
        }
        insightTitulo = titulo
        insight = "Consultando…"
        executor.execute {
            val texto = SemLinks.limpar(runCatching(bloco).getOrElse { "Não consegui buscar agora." })
            mainHandler.post {
                insight = texto
                speaker.speak(texto, voiceMode)
            }
        }
    }

    fun clearInsight() { insight = null; insightTitulo = null; speaker.stop() }

    /** Perfil de gosto: resumo do que você assiste, usado nas recomendações. */
    fun refreshTasteProfile() {
        val home = state as? AppState.Home ?: return
        val porId = home.catalog.movies.associateBy { it.id }
        val assistidos = home.userData.watched.mapNotNull { porId[it]?.title }
        val favoritos = home.catalog.movies.filter { "m:${it.id}" in home.userData.favorites }.map { it.title }
        val base = (assistidos + favoritos).distinct().take(25)
        if (base.isEmpty()) {
            tasteProfile = "Assista ou favorite alguns títulos para eu conhecer seu gosto."
            return
        }
        tasteProfile = "Montando seu perfil…"
        executor.execute {
            val texto = runCatching {
                openAi.askText(OpenAiClient.PROMPT_PERFIL, "Títulos: " + base.joinToString("; "))
            }.getOrElse { "Não consegui montar agora." }
            mainHandler.post { tasteProfile = texto; repo.saveTasteProfile(texto) }
        }
    }

    fun changeTypewriter(ativo: Boolean) { repo.setTypewriter(ativo); typewriter = ativo }

    fun changeLiveEnabled(ativo: Boolean) {
        repo.setLiveEnabled(ativo)
        liveEnabled = ativo
        if (!ativo) stopLive()
    }

    fun stopSpeech() { speaker.stop() }

    /** Abre a conversa ao vivo: microfone aberto, resposta em áudio contínuo. */
    fun startLive() {
        if (liveActive) return
        val chave = repo.openAiKey()?.takeIf { it.isNotBlank() } ?: BuildConfig.OPENAI_API_KEY
        if (chave.isBlank()) {
            chatError = "Configure a chave da OpenAI em Definições."
            return
        }
        speaker.stop()
        liveActive = true
        liveStatus = "conectando…"
        val catalogo = (state as? AppState.Home)?.catalog
        live = RealtimeSession(
            apiKey = chave,
            instrucoes = OpenAiClient.CHAT_PROMPT + " Fale de forma curta e natural, como numa conversa.",
            voz = openAiVoice,
            onEvent = { evento -> mainHandler.post { tratarLive(evento, catalogo) } },
        ).also { it.start() }
    }

    fun stopLive() {
        live?.stop()
        live = null
        liveActive = false
        liveStatus = null
    }

    private fun tratarLive(evento: RealtimeEvent, catalogo: Catalog?) {
        when (evento) {
            RealtimeEvent.Conectado -> liveStatus = "conectado"
            RealtimeEvent.Ouvindo -> liveStatus = "ouvindo você"
            RealtimeEvent.Respondendo -> liveStatus = "respondendo"
            RealtimeEvent.Encerrado -> { liveActive = false; liveStatus = null }
            is RealtimeEvent.Erro -> { liveStatus = "erro: ${evento.mensagem}"; liveActive = false }
            // O que foi falado vira mensagem na conversa, para ficar registrado.
            is RealtimeEvent.VocêDisse -> registrarFala(ChatRole.USER, evento.texto, catalogo)
            is RealtimeEvent.ElaDisse -> registrarFala(ChatRole.ASSISTANT, evento.texto, catalogo)
        }
    }

    private fun registrarFala(role: ChatRole, texto: String, catalogo: Catalog?) {
        if (texto.isBlank()) return
        val agora = System.currentTimeMillis()
        val base = currentChat ?: ChatSession(agora.toString(), ChatStore.titleFrom(texto), agora, emptyList())
        // Da fala dela, tentamos aproveitar títulos citados para virar cartões.
        val titulos = if (role == ChatRole.ASSISTANT && catalogo != null) {
            val achados = CatalogMatcher.match(texto.split(Regex("[.,;!?]")).map { it.trim() }, catalogo)
            achados.movies.map { movieKeyOf(it.id) } + achados.series.map { seriesKeyOf(it.id) }
        } else {
            emptyList()
        }
        val atualizada = base.copy(
            messages = base.messages + ChatMessage(role, texto, titulos),
            updatedAt = agora,
        )
        currentChat = atualizada
        chatSessions = ChatStore.upsert(chatSessions, atualizada)
        repo.saveChatSessions(chatSessions)
    }

    /** Trocar de voz ou de velocidade toca uma amostra, para dar para comparar. */
    fun changeOpenAiVoice(voice: String) {
        repo.setOpenAiVoice(voice)
        openAiVoice = voice
        speaker.speak(AMOSTRA, VoiceMode.OPENAI)
    }

    fun changeVoiceSpeed(speed: VoiceSpeed) {
        repo.setVoiceSpeed(speed)
        voiceSpeed = speed
        if (voiceMode != VoiceMode.MUDO) speaker.speak(AMOSTRA, voiceMode)
    }

    /** Reler uma resposta já recebida, sem gastar nova chamada. */
    fun speakAgain(text: String) { speaker.speak(text, voiceMode) }

    /** Retoma a conversa a partir de uma pergunta antiga, descartando o que veio depois. */
    fun continueFrom(index: Int) {
        val sessao = currentChat ?: return
        val pergunta = sessao.messages.getOrNull(index)?.takeIf { it.role == ChatRole.USER } ?: return
        currentChat = sessao.copy(messages = sessao.messages.take(index))
        sendChat(pergunta.text)
    }

    fun newChat() { currentChat = null; chatError = null; speaker.stop() }

    fun openChat(session: ChatSession) { currentChat = session; chatError = null }

    fun deleteChat(session: ChatSession) {
        chatSessions = chatSessions.filterNot { it.id == session.id }
        repo.saveChatSessions(chatSessions)
        if (currentChat?.id == session.id) currentChat = null
    }

    /** Envia a pergunta, guarda a conversa e fala a resposta. */
    fun sendChat(question: String) {
        val pergunta = question.trim()
        if (pergunta.isBlank() || chatThinking) return
        if (!openAi.isConfigured()) {
            chatError = "Configure a chave da OpenAI em Definições."
            return
        }
        val catalog = (state as? AppState.Home)?.catalog
        val agora = System.currentTimeMillis()
        val base = currentChat ?: ChatSession(
            id = agora.toString(),
            title = ChatStore.titleFrom(pergunta),
            updatedAt = agora,
            messages = emptyList(),
        )
        val comPergunta = base.copy(
            messages = base.messages + ChatMessage(ChatRole.USER, pergunta, emptyList()),
            updatedAt = agora,
        )
        currentChat = comPergunta
        chatThinking = true
        chatError = null
        speaker.stop()

        executor.execute {
            // A IA não conhece o catálogo. Buscamos de duas formas: pelo que a pessoa citou
            // ("tem homem-aranha?") e pelo que a própria IA sabe atender ao pedido
            // ("filmes do Adam Sandler" -> filmografia -> cruzamos com o catálogo).
            val citados = catalog?.let { CatalogSearch.candidates(pergunta, it) }.orEmpty()
            val porTema = if (catalog == null) {
                emptyList()
            } else {
                runCatching { CatalogMatcher.match(openAi.recommend(pergunta), catalog) }
                    .map { it.movies.map(Video::title) + it.series.map(Series::title) }
                    .getOrDefault(emptyList())
            }
            val doCatalogo = (citados + porTema).distinct().take(15)
            val perfil = repo.tasteProfile()
            val paraEnvio = ChatStore.lastMessages(comPergunta.messages).toMutableList()
            if (doCatalogo.isNotEmpty()) {
                val ultima = paraEnvio.last()
                paraEnvio[paraEnvio.lastIndex] = ultima.copy(
                    text = ultima.text + "\n\n[Disponíveis no catálogo do usuário: " +
                        doCatalogo.joinToString("; ") + "]",
                )
            }
            if (!perfil.isNullOrBlank()) {
                val ultima = paraEnvio.last()
                paraEnvio[paraEnvio.lastIndex] = ultima.copy(text = ultima.text + "\n[Gosto: " + perfil + "]")
            }
            val resultado = runCatching { openAi.chat(paraEnvio) }
            mainHandler.post {
                chatThinking = false
                resultado.fold(
                    onSuccess = { reply ->
                        registrarResposta(comPergunta, reply.copy(text = SemLinks.limpar(reply.text)), catalog)
                    },
                    onFailure = { chatError = it.message ?: "Não consegui falar com a IA." },
                )
            }
        }
    }

    private fun registrarResposta(sessao: ChatSession, reply: ChatReply, catalog: Catalog?) {
        // Só sugerimos o que existe no catálogo: o resto viraria clique sem destino.
        val disponiveis = catalog?.let { CatalogMatcher.match(reply.titles, it) }
        // Pedido de filme não deve trazer série (e vice-versa).
        val titulos = buildList {
            if (reply.type != "serie") disponiveis?.movies?.forEach { add(movieKeyOf(it.id)) }
            if (reply.type != "filme") disponiveis?.series?.forEach { add(seriesKeyOf(it.id)) }
        }
        val atualizada = sessao.copy(
            messages = sessao.messages + ChatMessage(ChatRole.ASSISTANT, reply.text, titulos),
            updatedAt = System.currentTimeMillis(),
        )
        currentChat = atualizada
        chatSessions = ChatStore.upsert(chatSessions, atualizada)
        repo.saveChatSessions(chatSessions)
        speaker.speak(reply.text, voiceMode)
    }

    private fun movieKeyOf(id: String) = "m:$id"
    private fun seriesKeyOf(id: String) = "s:$id"

    fun saveOpenAiKey(key: String) { repo.saveOpenAiKey(key); hasOpenAiKey = openAi.isConfigured() }

    fun changeSortOrder(order: SortOrder) { repo.setSortOrder(order); sortOrder = order }
    fun changeCacheTtl(ttl: CacheTtl) { repo.setCacheTtl(ttl); cacheTtl = ttl }
    fun clearCache() { executor.execute { repo.clearCache() } }
    fun logout() { repo.logout(); seriesDetail = DetailState.Idle; state = AppState.Login }
    fun returnToLogin() { state = AppState.Login }

    override fun onCleared() {
        buscandoArte = false
        repo.saveArtes(movieArt.toMap())
        executor.shutdownNow()
        focoExecutor.shutdownNow()
        speaker.release()
        live?.stop()
    }

    private companion object {
        const val AMOSTRA = "Olá! É assim que eu vou falar com você no Cinemora."
        const val ESPERA_FOCO = 400L
        const val ESPERA_FOCO_SERIE = 550L
        const val ESPERA_FILA = 200L
        const val LOTE_ARTE = 12
    }
}
