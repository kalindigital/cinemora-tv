package br.com.cinemora.tv.data

/** Validade do catálogo em cache, escolhida pelo usuário em Definições. */
enum class CacheTtl(val millis: Long) {
    SIX_HOURS(6 * 60 * 60 * 1000L),
    TWELVE_HOURS(12 * 60 * 60 * 1000L),
    TWENTY_FOUR_HOURS(24 * 60 * 60 * 1000L),
}

object CachePolicy {
    /** Fresco enquanto a idade do cache for positiva e menor que a validade. */
    fun isFresh(savedAtMillis: Long, nowMillis: Long, ttl: CacheTtl): Boolean {
        val age = nowMillis - savedAtMillis
        return age in 0 until ttl.millis
    }
}
