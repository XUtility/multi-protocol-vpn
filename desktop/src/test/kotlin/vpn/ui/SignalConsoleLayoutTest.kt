package vpn.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Signal Console is a dense dashboard: route browser, live tunnel control and
 * telemetry compete for one window. These tests pin the responsive contract so
 * the centre console always keeps a usable width instead of being crushed by
 * fixed side panels.
 */
class SignalConsoleLayoutTest {

    @Test
    fun `wide content keeps all three columns`() {
        val m = signalConsoleMetricsFor(1100f)

        assertEquals(SignalConsoleMode.THREE_COLUMN, m.mode)
        assertEquals(250f, m.routeWidth.value)
        assertEquals(290f, m.telemetryWidth.value)
        assertTrue(m.minimumCenterWidth.value >= 360f)
        assertTrue(250f + 290f + (m.gap.value * 2f) + m.minimumCenterWidth.value <= 1100f)
    }

    @Test
    fun `medium content collapses telemetry below the console`() {
        val m = signalConsoleMetricsFor(820f)

        assertEquals(SignalConsoleMode.TWO_COLUMN, m.mode)
        assertEquals(232f, m.routeWidth.value)
        assertEquals(0f, m.telemetryWidth.value)
        assertTrue(m.minimumCenterWidth.value >= 420f)
        assertTrue(232f + m.gap.value + m.minimumCenterWidth.value <= 820f)
    }

    @Test
    fun `compact content stacks every panel`() {
        val m = signalConsoleMetricsFor(430f)

        assertEquals(SignalConsoleMode.STACKED, m.mode)
        assertEquals(0f, m.routeWidth.value)
        assertEquals(0f, m.telemetryWidth.value)
        assertEquals(0f, m.minimumCenterWidth.value)
    }

    @Test
    fun `boundaries and defensive widths pick stable modes`() {
        assertEquals(SignalConsoleMode.STACKED, signalConsoleMetricsFor(0f).mode)
        assertEquals(SignalConsoleMode.STACKED, signalConsoleMetricsFor(-20f).mode)
        assertEquals(SignalConsoleMode.STACKED, signalConsoleMetricsFor(679f).mode)
        assertEquals(SignalConsoleMode.TWO_COLUMN, signalConsoleMetricsFor(680f).mode)
        assertEquals(SignalConsoleMode.TWO_COLUMN, signalConsoleMetricsFor(999f).mode)
        assertEquals(SignalConsoleMode.THREE_COLUMN, signalConsoleMetricsFor(1000f).mode)
    }

    @Test
    fun `padding and gaps shrink monotonically as the window narrows`() {
        val stacked = signalConsoleMetricsFor(430f)
        val two = signalConsoleMetricsFor(820f)
        val three = signalConsoleMetricsFor(1100f)

        assertTrue(stacked.contentPadding.value <= two.contentPadding.value)
        assertTrue(two.contentPadding.value <= three.contentPadding.value)
        assertTrue(stacked.gap.value <= two.gap.value)
        assertTrue(two.gap.value <= three.gap.value)
    }
}
