package com.xiaomanjun.sleepdownschedule.glass

import org.junit.Assert.*
import org.junit.Test

class GlassAppearancePersistenceTest {
    @Test fun upgradeWithoutGlassPreferenceKeepsOriginalRenderer() {
        assertFalse(GlassAppearance.decode(null).customized)
        assertFalse(GlassAppearance.decode("").customized)
    }

    @Test fun selectingAndResettingLiquidKeepTheOriginalComponentMaterials() {
        val selected = GlassAppearance().copy(customized = true)
        assertFalse(selected.usesMaterialOverride)
        val edited = selected.update(selected.current.copy(blur = 18f))
        assertTrue(edited.usesMaterialOverride)
        assertFalse(GlassAppearance.decode(edited.update(GlassParameters.defaults(GlassStyle.LIQUID)).encode()).usesMaterialOverride)
    }

    @Test fun gaussianAndSoftActivateWithoutChangingLiquidParameters() {
        val original = GlassAppearance()
        GlassStyle.entries.filter { it != GlassStyle.LIQUID }.forEach { style ->
            val selected = original.copy(style = style, customized = true)
            assertTrue(selected.usesMaterialOverride)
            assertEquals(original.current, selected.presets.getValue(GlassStyle.LIQUID))
        }
    }

    @Test fun threePresetsSurviveStorageAndThemeDoesNotOwnTheChoice() {
        val liquid = GlassAppearance().update(GlassParameters(blur = 3f, lensAmount = 0f))
        val gaussian = liquid.copy(style = GlassStyle.GAUSSIAN).update(GlassParameters.defaults(GlassStyle.GAUSSIAN).copy(blur = 41f))
        val soft = gaussian.copy(style = GlassStyle.SOFT).update(GlassParameters.defaults(GlassStyle.SOFT).copy(noise = .02f, brightness = -.08f))
        val restored = GlassAppearance.decode(soft.encode())
        assertEquals(soft, restored)
        assertEquals(3f, restored.presets.getValue(GlassStyle.LIQUID).blur)
        assertEquals(41f, restored.presets.getValue(GlassStyle.GAUSSIAN).blur)
        assertEquals(0f, restored.presets.getValue(GlassStyle.LIQUID).lensAmount)
    }

    @Test fun resettingOnePresetDoesNotOverwriteOtherStylesOrEntrySnapshot() {
        val entry = GlassAppearance().update(GlassParameters(blur = 11f))
        val preview = entry.copy(style = GlassStyle.SOFT).update(GlassParameters.defaults(GlassStyle.SOFT).copy(blur = 52f))
        val reset = preview.update(GlassParameters.defaults(GlassStyle.SOFT))
        assertEquals(11f, reset.presets.getValue(GlassStyle.LIQUID).blur)
        assertEquals(32f, reset.current.blur)
        assertFalse(entry.presets.getValue(GlassStyle.SOFT).blur == preview.current.blur)
        assertEquals(entry, GlassAppearance.decode(entry.encode()))
    }
}
