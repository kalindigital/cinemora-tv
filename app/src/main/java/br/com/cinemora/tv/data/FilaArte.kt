package br.com.cinemora.tv.data

/**
 * Fila de busca da arte 16:9 dos cartões.
 *
 * A arte só existe no get_vod_info, uma chamada por filme, então buscamos apenas o que
 * aparece na tela. Como o usuário rola depressa, o pedido mais recente vale mais que o
 * antigo: atendemos do fim para o começo e descartamos o excedente.
 */
class FilaArte(private val limite: Int = 40) {
    private val pendentes = ArrayDeque<String>()
    private val pedidos = HashSet<String>()

    /** Devolve false quando o filme já foi pedido ou já tem arte. */
    @Synchronized
    fun pedir(id: String): Boolean {
        if (!pedidos.add(id)) return false
        pendentes.addLast(id)
        if (pendentes.size > limite) pedidos.remove(pendentes.removeFirst())
        return true
    }

    @Synchronized
    fun proximo(): String? = pendentes.removeLastOrNull()

    /** Marca o que já veio do disco para não pedir de novo ao provedor. */
    @Synchronized
    fun resolvidos(ids: Set<String>) {
        pedidos.addAll(ids)
    }
}
