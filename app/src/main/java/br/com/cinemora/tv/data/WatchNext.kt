package br.com.cinemora.tv.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram

/**
 * Linha "Continuar assistindo" da tela inicial da Android TV: o título em andamento vira
 * um cartão do sistema que retoma a reprodução sem passar pelo app.
 */
object WatchNext {
    /** Última tentativa, exibida em Definições (não há como ver o log da TV daqui). */
    var lastStatus: String = "ainda não publicado"
        private set

    fun update(context: Context, entry: ResumeEntry) {
        // O launcher descarta cartões "continuar" sem progresso real: sem duração ou em 0s
        // não há o que continuar, então nem publicamos.
        if (entry.durationMs <= 0 || entry.positionMs <= 0) {
            lastStatus = "aguardando progresso (posição ${entry.positionMs / 1000}s, duração ${entry.durationMs / 1000}s)"
            return
        }
        val resultado = runCatching {
            val builder = WatchNextProgram.Builder()
                .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setTitle(entry.title)
                .setLastPlaybackPositionMillis(entry.positionMs.toInt())
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setInternalProviderId(entry.id)
                .setContentId(entry.id)
                .setIntentUri(Uri.parse(resumeUri(context, entry.id)))
            if (entry.durationMs > 0) builder.setDurationMillis(entry.durationMs.toInt())
            // A capa é retrato 2:3; sem declarar isso o launcher espera 16:9 e ignora o cartão.
            entry.posterUrl?.let {
                builder.setPosterArtUri(Uri.parse(it))
                    .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER)
            }

            val values: ContentValues = builder.build().toContentValues()
            // A consulta é isolada: quando ela falhava, a exceção abortava a inserção
            // inteira e o cartão nunca era criado.
            val existente = runCatching { findId(context, entry.id) }.getOrNull()
            if (existente == null) {
                context.contentResolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, values)
                    ?: error("o sistema recusou a inserção")
            } else {
                context.contentResolver.update(
                    ContentUris.withAppendedId(TvContractCompat.WatchNextPrograms.CONTENT_URI, existente),
                    values, null, null,
                )
            }
        }
        val hora = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        lastStatus = resultado.fold(
            // Relemos a fila: se a linha está lá e o cartão não aparece, o filtro é do launcher.
            onSuccess = { "gravado às $hora — ${contarCartoes(context)} na fila do sistema" },
            onFailure = { "falhou: ${it::class.java.simpleName} — ${it.message.orEmpty().take(120)}" },
        )
    }

    fun remove(context: Context, id: String) {
        runCatching {
            val existente = findId(context, id) ?: return
            context.contentResolver.delete(
                ContentUris.withAppendedId(TvContractCompat.WatchNextPrograms.CONTENT_URI, existente),
                null, null,
            )
        }
    }

    /** O cartão aponta para um id interno; a URL do stream fica guardada no app. */
    private fun resumeUri(context: Context, id: String) = "cinemora://${context.packageName}/resume/$id"

    /** Quantos cartões nossos o sistema realmente guardou. */
    private fun contarCartoes(context: Context): String = runCatching {
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI, null, null, null, null,
        ) ?: return "sem acesso à fila"
        cursor.use { "${it.count} cartão(ões)" }
    }.getOrElse { "leitura falhou (${it::class.java.simpleName})" }

    private fun findId(context: Context, internalId: String): Long? {
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI, null, null, null, null,
        ) ?: return null
        cursor.use {
            while (it.moveToNext()) {
                val program = WatchNextProgram.fromCursor(it)
                if (program.internalProviderId == internalId) return program.id
            }
        }
        return null
    }

    fun intentFilterUri(packageName: String): String = "cinemora://$packageName"
}

/** Ponto de entrada do cartão da tela inicial. */
object ResumeLink {
    fun idFrom(intent: Intent): String? = intent.data?.lastPathSegment?.takeIf { it.isNotBlank() }
}
