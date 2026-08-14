package br.com.cinemora.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import br.com.cinemora.tv.data.TvTrust
import okhttp3.OkHttpClient

/**
 * Cache de imagens generoso: as capas são muitas e a TV tem rede lenta. Guardar em disco
 * evita rebaixar tudo a cada abertura do app.
 */
class CinemoraApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient {
            val (socketFactory, trustManager) = TvTrust.socketFactory(this)
            OkHttpClient.Builder().sslSocketFactory(socketFactory, trustManager).build()
        }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("capas"))
                .maxSizeBytes(300L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        // O provedor não manda cabeçalhos de cache úteis; a arte de um título não muda.
        .respectCacheHeaders(false)
        .build()
}
