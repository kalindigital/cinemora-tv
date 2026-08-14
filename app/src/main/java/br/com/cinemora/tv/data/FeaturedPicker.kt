package br.com.cinemora.tv.data

import kotlin.random.Random

/** Escolhe o título em destaque. A semente é o timestamp do cache: estável dentro do
 * mesmo cache e muda quando o catálogo é atualizado do servidor. */
object FeaturedPicker {
    fun <T> pick(items: List<T>, seed: Long): T? =
        if (items.isEmpty()) null else items[Random(seed).nextInt(items.size)]
}
