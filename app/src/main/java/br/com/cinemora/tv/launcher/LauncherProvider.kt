package br.com.cinemora.tv.launcher

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import br.com.cinemora.tv.data.ImageUrls
import br.com.cinemora.tv.data.LocalStore
import br.com.cinemora.tv.model.Catalog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

/**
 * Entrega a um launcher (o Free My TV) as fileiras de Filmes e Séries e as capas, lidas do
 * catálogo que o app já guarda em disco. Só sai o que serve para o cartão: id, nome, ano, nota
 * e a imagem. Usuário, senha e links de stream ficam aqui dentro.
 *
 *  content://br.com.cinemora.tv.launcher/rows/movie   (ou /rows/series)
 *  content://br.com.cinemora.tv.launcher/search/movie?q=texto   (ou /search/series)
 *  content://br.com.cinemora.tv.launcher/cover/movie/{id}   (ou /cover/series/{id})
 *
 * Abrir um título: cinemora://br.com.cinemora.tv/movie/{id} (ResumeActivity).
 */
class LauncherProvider : ContentProvider() {
    private val http by lazy {
        OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()
    }
    @Volatile private var cached: Catalog? = null
    @Volatile private var cachedAt = 0L

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        val seg = uri.pathSegments
        if (seg.size != 2 || (seg[0] != "rows" && seg[0] != "search")) return cursor
        val store = LocalStore(context ?: return cursor)
        if (store.credentials() == null) return cursor    // saiu da conta: nada para mostrar
        val catalog = catalog() ?: return cursor
        if (seg[0] == "search") {
            val q = uri.getQueryParameter("q").orEmpty().take(80)
            val itens = when (seg[1]) {
                OpenLink.MOVIE -> LauncherRows.searchMovies(catalog, q, store.hideAdult(), store.familyMode())
                OpenLink.SERIES -> LauncherRows.searchSeries(catalog, q, store.hideAdult(), store.familyMode())
                else -> return cursor
            }
            itens.forEach { cursor.addRow(arrayOf<Any?>(0, q, it.id, it.title, it.year, it.rating)) }
            return cursor
        }
        val rows = when (seg[1]) {
            OpenLink.MOVIE -> LauncherRows.movies(catalog, store.hideAdult(), store.familyMode(), store.sortOrder())
            OpenLink.SERIES -> LauncherRows.series(catalog, store.hideAdult(), store.familyMode(), store.sortOrder())
            else -> return cursor
        }
        rows.forEachIndexed { i, row ->
            row.items.forEach { cursor.addRow(arrayOf<Any?>(i, row.title, it.id, it.title, it.year, it.rating)) }
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val seg = uri.pathSegments
        if (mode != "r" || seg.size != 3 || seg[0] != "cover") throw FileNotFoundException(uri.toString())
        val kind = seg[1]
        val id = seg[2]
        if (!id.matches(Regex("[\\w.-]+"))) throw FileNotFoundException(uri.toString())
        val dir = File(context!!.cacheDir, "launcher-covers").apply { mkdirs() }
        val file = File(dir, "$kind-$id.img")
        if (!file.exists() || file.length() == 0L) {
            val catalog = catalog() ?: throw FileNotFoundException("sem catálogo")
            val url = ImageUrls.card(LauncherRows.coverOf(catalog, kind, id)) ?: throw FileNotFoundException("sem capa")
            http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body
                if (!resp.isSuccessful || body == null) throw FileNotFoundException("HTTP ${resp.code}")
                val tmp = File(dir, "$kind-$id.tmp")
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                tmp.renameTo(file)
            }
            trim(dir)
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    /** O catálogo em disco, relido só quando o arquivo muda (o JSON é grande). */
    private fun catalog(): Catalog? {
        val ctx = context ?: return null
        val file = File(ctx.filesDir, "catalog.json")
        val at = file.lastModified()
        if (cached == null || at != cachedAt) {
            cached = LocalStore(ctx).cachedCatalog()
            cachedAt = at
        }
        return cached
    }

    /** As capas mais antigas saem quando a pasta passa de 400. */
    private fun trim(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size > 400) files.take(files.size - 400).forEach { it.delete() }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int = 0

    companion object {
        val COLUMNS = arrayOf("row", "row_title", "id", "title", "year", "rating")
    }
}
