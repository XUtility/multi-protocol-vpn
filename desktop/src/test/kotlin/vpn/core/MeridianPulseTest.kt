package vpn.core

import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Plan 011 §6.3 — meridianPulseSeconds must be TOTAL, MONOTONIC and CLAMPED.
 *
 * The composable calls this every recomposition with whatever the traffic
 * sampler reports, so a NaN, a negative or a throw would propagate straight
 * into an animation spec and break the frame loop.
 */
class MeridianPulseTest {

    @Test
    fun `disconnected link has no pulse`() {
        assertNull(meridianPulseSeconds(0, linkUp = false))
        assertNull(meridianPulseSeconds(1_000_000, linkUp = false))
        assertNull(meridianPulseSeconds(-5, linkUp = false))
        assertNull(meridianPulseSeconds(Long.MAX_VALUE, linkUp = false))
    }

    @Test
    fun `connected idle link crawls at the idle period`() {
        assertEquals(MERIDIAN_IDLE_SECONDS, meridianPulseSeconds(0, linkUp = true))
        assertEquals(MERIDIAN_IDLE_SECONDS, meridianPulseSeconds(-42, linkUp = true))
        assertEquals(MERIDIAN_IDLE_SECONDS, meridianPulseSeconds(Long.MIN_VALUE, linkUp = true))
    }

    @Test
    fun `every connected rate lands inside the clamped band`() {
        val rates = longArrayOf(
            0, 1, 2, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
            5_000_000, 25_000_000, 100_000_000, Long.MAX_VALUE, -1, Long.MIN_VALUE,
        )
        for (r in rates) {
            val s = meridianPulseSeconds(r, linkUp = true)!!
            assertTrue(
                s >= MERIDIAN_FLOOR_SECONDS - 1e-9 && s <= MERIDIAN_IDLE_SECONDS + 1e-9 &&
                    !s.isNaN() && s.isFinite(),
                "rx=$r gave $s, outside [$MERIDIAN_FLOOR_SECONDS, $MERIDIAN_IDLE_SECONDS]",
            )
        }
    }

    @Test
    fun `pulse never slows down as throughput grows (monotonic non-increasing)`() {
        var previous = meridianPulseSeconds(0, linkUp = true)!!
        var r = 1L
        while (r > 0) { // stops on overflow at the top of the Long range
            val next = meridianPulseSeconds(r, linkUp = true)!!
            assertTrue(
                next <= previous + 1e-12,
                "monotonicity broken between rx=${r / 2} ($previous) and rx=$r ($next)",
            )
            previous = next
            r *= 2
        }
        assertEquals(MERIDIAN_FLOOR_SECONDS, meridianPulseSeconds(Long.MAX_VALUE, linkUp = true)!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun `band edges are the documented constants`() {
        assertEquals(9.0, MERIDIAN_IDLE_SECONDS, absoluteTolerance = 1e-9)
        assertEquals(1.1, MERIDIAN_FLOOR_SECONDS, absoluteTolerance = 1e-9)
        assertTrue(MERIDIAN_FULL_SPEED_RX > 0)
        // Half a decade of headroom below full speed must still be strictly slower.
        val half = meridianPulseSeconds(MERIDIAN_FULL_SPEED_RX / 10, linkUp = true)!!
        val full = meridianPulseSeconds(MERIDIAN_FULL_SPEED_RX, linkUp = true)!!
        assertTrue((half - full).absoluteValue > 0.01, "log mapping collapsed: half=$half full=$full")
    }
}
