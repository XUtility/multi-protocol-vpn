package vpn.core

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Plan 011 §6.4 — the theme id must survive a save/reload round-trip, and a
 * garbage value must fall back to the default instead of crashing the load.
 * Same backup/restore discipline as StorageTest: real settings.json in the
 * pinned build/test-data dir, original state restored afterwards.
 */
class ThemePersistenceTest {

    @AfterTest
    fun cleanup() {
        File(Storage.dataDir, "settings.json").delete()
    }

    @Test
    fun `themeId round-trips through the settings store`() {
        val original = Storage.loadSettings()
        try {
            Storage.saveSettings(original.copy(themeId = "classic"))
            val back = Storage.loadSettings()
            assertEquals("classic", back.themeId)

            Storage.saveSettings(original.copy(themeId = "aurora"))
            assertEquals("aurora", Storage.loadSettings().themeId)
        } finally {
            Storage.saveSettings(original)
        }
    }

    @Test
    fun `default for a fresh install is aurora`() {
        // New AND existing users: the field is absent from old settings.json
        // files, and kotlinx.serialization applies the default.
        assertEquals("aurora", AppSettings().themeId)
        val f = File(Storage.dataDir, "settings.json")
        val backup = if (f.exists()) f.readText() else null
        try {
            f.delete()
            assertEquals("aurora", Storage.loadSettings().themeId)
            // An old file without the key at all still loads, with the default.
            f.writeText("""{"theme":"light","proxyPort":10808}""")
            val s = Storage.loadSettings()
            assertEquals("aurora", s.themeId)
            assertEquals("light", s.theme)
        } finally {
            if (backup != null) f.writeText(backup) else f.delete()
        }
    }

    @Test
    fun `unknown or garbage theme ids fall back to the default without throwing`() {
        val original = Storage.loadSettings()
        try {
            for (garbage in listOf("nonsense", "", "Aurora", "classic ")) {
                Storage.saveSettings(original.copy(themeId = garbage))
                val back = Storage.loadSettings()
                assertEquals(
                    vpn.theme.Palettes.DEFAULT_ID, back.themeId,
                    "garbage id \"$garbage\" was not sanitised to the default",
                )
            }
        } finally {
            Storage.saveSettings(original)
        }
    }
}
