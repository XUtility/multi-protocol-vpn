package vpn.theme

import java.awt.Font as AwtFont
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Plan 011 §6.5 — the three vendored families must be on the classpath under
 * `fonts/` and each file must genuinely parse as a font.
 *
 * Two levels of proof:
 *  1. AWT `createFont` — a real TTF parser, no native dependency;
 *  2. the exact androidx.compose desktop `Font(path)` constructor that
 *     Theme.kt itself uses, so a path typo cannot pass level 1 and still
 *     break the app.
 */
class FontResourcesTest {

    private val files = listOf(
        "fonts/Sora-SemiBold.ttf",
        "fonts/Sora-Bold.ttf",
        "fonts/Inter-Regular.ttf",
        "fonts/Inter-Medium.ttf",
        "fonts/Inter-SemiBold.ttf",
        "fonts/JetBrainsMono-Medium.ttf",
    )

    private val licenses = listOf(
        "fonts/OFL-Sora.txt",
        "fonts/OFL-Inter.txt",
        "fonts/OFL-JBMono.txt",
    )

    @Test
    fun `every vendored font file is on the classpath and parses`() {
        for (path in files) {
            val stream = javaClass.classLoader.getResourceAsStream("/$path")
                ?: javaClass.classLoader.getResourceAsStream(path)
            assertNotNull(stream, "$path is not packaged under src/main/resources")
            val bytes = stream.use { it.readBytes() }
            assertTrue(bytes.size > 10_000, "$path is suspiciously small (${bytes.size} B)")
            val font = AwtFont.createFont(AwtFont.TRUETYPE_FONT, ByteArrayInputStream(bytes))
            assertNotNull(font, "$path did not parse as a TTF")
        }
    }

    @Test
    fun `the compose Font loader accepts the exact paths Theme kt uses`() {
        // Same construction as vpn/theme/Theme.kt — must not throw.
        androidx.compose.ui.text.platform.Font("fonts/Sora-SemiBold.ttf", androidx.compose.ui.text.font.FontWeight.SemiBold)
        androidx.compose.ui.text.platform.Font("fonts/Sora-Bold.ttf", androidx.compose.ui.text.font.FontWeight.Bold)
        androidx.compose.ui.text.platform.Font("fonts/Inter-Regular.ttf", androidx.compose.ui.text.font.FontWeight.Normal)
        androidx.compose.ui.text.platform.Font("fonts/Inter-Medium.ttf", androidx.compose.ui.text.font.FontWeight.Medium)
        androidx.compose.ui.text.platform.Font("fonts/Inter-SemiBold.ttf", androidx.compose.ui.text.font.FontWeight.SemiBold)
        androidx.compose.ui.text.platform.Font("fonts/JetBrainsMono-Medium.ttf", androidx.compose.ui.text.font.FontWeight.Medium)
    }

    /**
     * usWeightClass from the OS/2 table (offset +4 in the table, big-endian
     * ushort) located through the sfnt table directory.
     */
    private fun os2WeightClass(bytes: ByteArray): Int {
        fun u16(at: Int) = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
        val numTables = u16(4)
        for (i in 0 until numTables) {
            val record = 12 + i * 16
            if (String(bytes, record, 4, Charsets.US_ASCII) == "OS/2") {
                val tableOffset = ((bytes[record + 8].toInt() and 0xFF) shl 24) or
                    ((bytes[record + 9].toInt() and 0xFF) shl 16) or
                    ((bytes[record + 10].toInt() and 0xFF) shl 8) or
                    (bytes[record + 11].toInt() and 0xFF)
                return u16(tableOffset + 4)
            }
        }
        error("no OS/2 table found")
    }

    @Test
    fun `the vendored weights carry the declared OS-2 weight classes`() {
        val expected = mapOf(
            "fonts/Sora-SemiBold.ttf" to 600,
            "fonts/Sora-Bold.ttf" to 700,
            "fonts/Inter-Regular.ttf" to 400,
            "fonts/Inter-Medium.ttf" to 500,
            "fonts/Inter-SemiBold.ttf" to 600,
            "fonts/JetBrainsMono-Medium.ttf" to 500,
        )
        for ((path, weight) in expected) {
            val stream = javaClass.classLoader.getResourceAsStream(path)
                ?: javaClass.classLoader.getResourceAsStream("/$path")
            assertNotNull(stream, path)
            val bytes = stream.use { it.readBytes() }
            // sanity: still a parseable TTF
            AwtFont.createFont(AwtFont.TRUETYPE_FONT, ByteArrayInputStream(bytes))
            assertEquals(weight, os2WeightClass(bytes), "$path usWeightClass")
        }
    }

    @Test
    fun `OFL license texts ship next to the binaries`() {
        for (path in licenses) {
            val stream = javaClass.classLoader.getResourceAsStream(path)
                ?: javaClass.classLoader.getResourceAsStream("/$path")
            assertNotNull(stream, "$path missing — OFL requires the licence text to ship")
            val text = stream.use { it.readBytes().decodeToString() }
            assertTrue(text.contains("SIL Open Font License"), "$path is not an OFL text")
        }
    }
}
