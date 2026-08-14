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
    fun update(context: Context, entry: ResumeEntry) {
        runCatching {
            val builder = WatchNextProgram.Builder()
                .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setTitle(entry.title)
                .setLastPlaybackPositionMillis(entry.positionMs.toInt())
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setInternalProviderId(entry.id)
                .setIntentUri(Uri.parse(resumeUri(context, entry.id)))
            if (entry.durationMs > 0) builder.setDurationMillis(entry.durationMs.toInt())
            entry.posterUrl?.let { builder.setPosterArtUri(Uri.parse(it)) }

            val values: ContentValues = builder.build().toContentValues()
            val existente = findId(context, entry.id)
            if (existente == null) {
                context.contentResolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, values)
            } else {
                context.contentResolver.update(
                    ContentUris.withAppendedId(TvContractCompat.WatchNextPrograms.CONTENT_URI, existente),
                    values, null, null,
                )
            }
        }
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
