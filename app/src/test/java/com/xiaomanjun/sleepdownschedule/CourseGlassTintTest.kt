package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.glass.ui.courseGlassTintAlpha
import com.xiaomanjun.sleepdownschedule.glass.ui.courseSimpleBlurTintAlpha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseGlassTintTest {
    @Test
    fun readableTintSurvivesReducedSamplingQuality() {
        val wallpaperTint = courseGlassTintAlpha(cardAlpha = 1f, quality = 1f, hasWallpaper = true)
        val fallbackTint = courseGlassTintAlpha(cardAlpha = 1f, quality = 1f, hasWallpaper = false)

        assertTrue(wallpaperTint >= 0.8f && fallbackTint >= 0.8f)
        assertEquals(wallpaperTint, courseGlassTintAlpha(1f, .5f, true), .0001f)
        assertEquals(fallbackTint, courseGlassTintAlpha(1f, .5f, false), .0001f)
    }

    @Test
    fun noWallpaperTintStillTracksThePersonalizationSlider() {
        assertEquals(
            0.4f,
            courseGlassTintAlpha(cardAlpha = 0.4f, quality = 1f, hasWallpaper = false),
            0.0001f
        )
        assertEquals(
            0f,
            courseSimpleBlurTintAlpha(cardAlpha = 0f, quality = 1f, hasWallpaper = false),
            0.0001f
        )
    }

    @Test
    fun tintFunctionsClampOutOfRangeInputs() {
        assertEquals(1f, courseGlassTintAlpha(2f, 2f, hasWallpaper = false), 0.0001f)
        assertEquals(0f, courseGlassTintAlpha(-1f, 1f, hasWallpaper = false), 0.0001f)
        assertEquals(0.18f, courseSimpleBlurTintAlpha(2f, 2f, hasWallpaper = false), 0.0001f)
    }
}
