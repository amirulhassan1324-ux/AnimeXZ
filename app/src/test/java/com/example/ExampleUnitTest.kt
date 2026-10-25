package com.example

import com.example.config.VideoConfig
import com.example.data.api.FallbackData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun videoConfig_returnsValidSource() {
        val source = VideoConfig.getEpisodeSource(153518, 1)
        assertNotNull(source)
        assertTrue(source.url.startsWith("http"))
        assertTrue(source.type == "mp4" || source.type == "m3u8")
    }

    @Test
    fun fallbackData_hasFeaturedAndTrending() {
        val featured = FallbackData.getFeaturedHero()
        assertNotNull(featured)
        assertEquals("Black Clover Season 2", featured.title)

        val trending = FallbackData.getTrending()
        assertTrue(trending.isNotEmpty())
        assertEquals(6, trending.size)

        val popular = FallbackData.getPopular()
        assertTrue(popular.isNotEmpty())

        val topRated = FallbackData.getTopRated()
        assertTrue(topRated.isNotEmpty())
    }
}
