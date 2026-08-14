package br.com.cinemora.tv

import br.com.cinemora.tv.data.FeaturedPicker
import br.com.cinemora.tv.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedPickerTest {
    private fun video(id: Int) = Video(id.toString(), "Filme $id", "c", null, "http://x/$id.mp4")

    @Test fun `catalogo vazio nao tem destaque`() {
        assertNull(FeaturedPicker.pick(emptyList(), seed = 123L))
    }

    @Test fun `a mesma semente escolhe sempre o mesmo destaque`() {
        val videos = (1..10).map { video(it) }
        assertEquals(FeaturedPicker.pick(videos, seed = 42L), FeaturedPicker.pick(videos, seed = 42L))
    }

    @Test fun `sementes diferentes variam o destaque`() {
        val videos = (1..10).map { video(it) }
        val escolhidos = (0L until 20L).map { FeaturedPicker.pick(videos, it)!!.id }.toSet()
        assertTrue(escolhidos.size > 1)
    }
}
