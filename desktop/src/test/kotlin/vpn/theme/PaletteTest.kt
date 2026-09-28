package vpn.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Plan 011 §6.1/§6.2 — the palette contract.
 *
 *  1. every colour slot carries a real value in BOTH palettes and BOTH modes;
 *  2. Aurora actually differs from Classic (the theme is not a no-op);
 *  3. Aurora text meets WCAG AA on the card surface;
 *  4. Classic is pinned hex-for-hex to the pre-011 look — the owner's safety
 *     net: selecting Classic can never drift from the app they shipped with.
 */
class PaletteTest {

    // ------------------------------------------------------------------
    // §6.1 completeness
    // ------------------------------------------------------------------

    @Test
    fun `every colour slot has a non-default value in both palettes and modes`() {
        for (p in listOf(Palettes.Classic, Palettes.Aurora)) {
            for ((mode, colors) in listOf("dark" to p.dark, "light" to p.light)) {
                for ((name, value) in colors.entries()) {
                    assertNotEquals(
                        Color.Unspecified, value,
                        "${p.id}/$mode: token $name is Unspecified",
                    )
                }
            }
        }
    }

    @Test
    fun `aurora differs from classic on at least eight tokens in each mode`() {
        for ((mode, aurora, classic) in listOf(
            Triple("dark", Palettes.Aurora.dark, Palettes.Classic.dark),
            Triple("light", Palettes.Aurora.light, Palettes.Classic.light),
        )) {
            val classicByName = classic.entries().toMap()
            val differing = aurora.entries().count { (name, value) ->
                classicByName[name] != value
            }
            assertTrue(
                differing >= 8,
                "$mode: Aurora only differs from Classic on $differing tokens (need >= 8)",
            )
        }
    }

    // ------------------------------------------------------------------
    // WCAG relative luminance + contrast (the test's own helper, per plan)
    // ------------------------------------------------------------------

    private fun luminance(c: Color): Double {
        fun ch(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }

    private fun contrast(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test
    fun `aurora text tokens meet WCAG AA on the card surface`() {
        for ((mode, colors) in listOf("dark" to Palettes.Aurora.dark, "light" to Palettes.Aurora.light)) {
            for ((name, token) in listOf(
                "TextPrimary" to colors.textPrimary,
                "TextSecondary" to colors.textSecondary,
                "TextFaint" to colors.textFaint,
            )) {
                val ratio = contrast(token, colors.surface)
                assertTrue(
                    ratio >= 4.5,
                    "aurora/$mode: $name on Surface is ${"%.2f".format(ratio)}:1, below WCAG AA 4.5:1",
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // §6.2 Classic is unchanged — exact hex pins
    // ------------------------------------------------------------------

    @Test
    fun `classic dark keeps the pre-011 hexes`() {
        val d = Palettes.Classic.dark
        // Color.value packs ARGB into the HIGH 32 bits — compare Color to Color.
        assertEquals(Color(0xFF0B1020), d.bgTop, "BgTop drifted")
        assertEquals(Color(0xFF151D32), d.surface, "Surface drifted")
        assertEquals(Color(0xFF4F46E5), d.accent, "Accent drifted")
        assertEquals(Color(0xFF16A368), d.success, "Success drifted")
        assertEquals(Color(0xFFDC3E5A), d.error, "Error drifted")
        assertEquals(Color(0xFF27334E), d.border, "Border drifted")
    }

    @Test
    fun `classic light keeps the pre-011 hexes`() {
        val l = Palettes.Classic.light
        assertEquals(Color(0xFFF7F8FC), l.bgTop)
        assertEquals(Color(0xFFFFFFFF), l.surface)
        assertEquals(Color(0xFF4F46E5), l.accent)
        assertEquals(Color(0xFF16A368), l.success)
        assertEquals(Color(0xFFE1E5F0), l.border)
        assertEquals(Color(0xFF172033), l.textPrimary)
    }

    @Test
    fun `classic grid and radii match the pre-011 drawing code`() {
        // Components.kt's Carbon backdrop drew a 36 dp grid at 4.5 % accent.
        assertEquals(Color(0xFF4F46E5).copy(alpha = 0.045f), Palettes.Classic.gridColor)
        assertEquals(36f, Palettes.Classic.gridCellDp)
        assertEquals(24f, Palettes.Classic.cornerHeroDp) // HomeScreen hero Surface
        assertEquals(16f, Palettes.Classic.cornerCardDp) // GlassCard
    }

    @Test
    fun `classic keeps platform fonts so its type is byte-identical`() {
        assertTrue(Palettes.Classic.fontDisplay == null)
        assertTrue(Palettes.Classic.fontBody == null)
        assertTrue(Palettes.Classic.fontMono == null)
        // ...and the Aurora palette is the one that brings Sora/Inter/JBM.
        assertTrue(Palettes.Aurora.fontDisplay != null)
        assertTrue(Palettes.Aurora.fontBody != null)
        assertTrue(Palettes.Aurora.fontMono != null)
    }

    @Test
    fun `aurora fields are the four mock stops within the spec'd alpha band`() {
        val f = Palettes.Aurora.auroraFields
        assertEquals(4, f.size, "the mock has exactly four light fields")
        assertTrue(f.all { it.alpha in 0.26f..0.44f }, "alpha must stay in 0.26..0.44")
        assertTrue(f.all { it.color != Color.Unspecified })
        assertTrue(Palettes.Classic.auroraFields.isEmpty(), "Classic never drifts fields")
    }

    @Test
    fun `byId falls back to the default palette on garbage`() {
        assertEquals(Palettes.Classic, Palettes.byId("classic"))
        assertEquals(Palettes.Aurora, Palettes.byId("aurora"))
        assertEquals(Palettes.Aurora, Palettes.byId("nonsense"))
        assertEquals(Palettes.Aurora, Palettes.byId(null))
    }
}
