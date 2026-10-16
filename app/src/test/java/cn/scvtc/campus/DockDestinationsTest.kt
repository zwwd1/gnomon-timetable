package cn.scvtc.campus

import org.junit.Assert.assertEquals
import org.junit.Test

class DockDestinationsTest {
    @Test fun newInstallAndPreviousDefaultsUseThreeDestinations() {
        listOf(null, "courses,settings", "courses,grades,sync,settings").forEach {
            assertEquals(listOf("grades", "courses", "settings"), restoredDockDestinations(it, 0))
        }
    }

    @Test fun explicitCustomChoicesAndNewSchemaKeepTheirOrder() {
        assertEquals(listOf("sync", "settings"), restoredDockDestinations("sync,settings", 0))
        assertEquals(listOf("courses", "settings"), restoredDockDestinations("courses,settings", 2))
    }

    @Test fun damagedPreferenceStillLeavesThePersonalCenterReachable() {
        assertEquals(DefaultDockDestinations, restoredDockDestinations("unknown,grades", 2))
        assertEquals(listOf("grades", "settings"), restoredDockDestinations("grades,grades,unknown,settings", 2))
    }
}
