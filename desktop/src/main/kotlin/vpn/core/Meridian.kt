package vpn.core

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * The Meridian pulse's travel time — pure, total, monotonic (plan 011 §5.4).
 *
 * "Pulse speed is throughput": the light travelling the arc from YOU to EXIT
 * takes [IDLE_SECONDS] when the tunnel is up but quiet, compressing on a log
 * scale toward [FLOOR_SECONDS] as received bytes/s grow. Lives in `vpn.core`
 * (not `vpn.theme`) because that is where Kover actually measures — the
 * coverage config excludes `vpn.theme`.
 *
 * Contract (pinned by MeridianPulseTest):
 *  - disconnected → `null` (draw the hairline arc, animate nothing);
 *  - connected → always within [FLOOR_SECONDS, IDLE_SECONDS], never NaN;
 *  - non-increasing as `rxPerSec` grows; total for any Long, no throws.
 */

/** Travel seconds for a connected-but-idle link (slow crawl). */
const val MERIDIAN_IDLE_SECONDS: Double = 9.0

/** Floor travel seconds at (or above) full-speed throughput. */
const val MERIDIAN_FLOOR_SECONDS: Double = 1.1

/** rx bytes/s at which the pulse reaches the floor; above this it stays clamped. */
const val MERIDIAN_FULL_SPEED_RX: Long = 25_000_000L

/**
 * @param rxPerSec received bytes per second (values <= 0 read as "no traffic").
 * @param linkUp whether the tunnel is connected.
 * @return the pulse travel duration, or `null` when there is nothing to animate.
 */
fun meridianPulseSeconds(rxPerSec: Long, linkUp: Boolean): Double? {
    if (!linkUp) return null
    if (rxPerSec <= 0L) return MERIDIAN_IDLE_SECONDS
    // Log-scale compression from IDLE at 1 B/s to FLOOR at FULL_SPEED_RX.
    val span = ln(MERIDIAN_FULL_SPEED_RX.toDouble()) // > 0, constant
    val t = (ln(rxPerSec.toDouble()) / span).coerceIn(0.0, 1.0)
    return min(MERIDIAN_IDLE_SECONDS, max(MERIDIAN_FLOOR_SECONDS, MERIDIAN_IDLE_SECONDS - t * (MERIDIAN_IDLE_SECONDS - MERIDIAN_FLOOR_SECONDS)))
}
