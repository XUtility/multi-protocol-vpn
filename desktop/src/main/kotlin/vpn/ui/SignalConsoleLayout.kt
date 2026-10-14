package vpn.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SignalConsoleMode { THREE_COLUMN, TWO_COLUMN, STACKED }

/**
 * Space budget for the dashboard's three information surfaces. The centre
 * console is the primary control, so side panels are only shown when the
 * remaining width can still hold a usable tunnel control.
 */
data class SignalConsoleMetrics(
    val mode: SignalConsoleMode,
    val contentPadding: Dp,
    val gap: Dp,
    val routeWidth: Dp,
    val telemetryWidth: Dp,
    val minimumCenterWidth: Dp,
)

fun signalConsoleMetricsFor(widthDp: Float): SignalConsoleMetrics = when {
    widthDp >= 1000f -> SignalConsoleMetrics(
        mode = SignalConsoleMode.THREE_COLUMN,
        contentPadding = 18.dp,
        gap = 14.dp,
        routeWidth = 250.dp,
        telemetryWidth = 290.dp,
        minimumCenterWidth = 360.dp,
    )
    widthDp >= 680f -> SignalConsoleMetrics(
        mode = SignalConsoleMode.TWO_COLUMN,
        contentPadding = 14.dp,
        gap = 12.dp,
        routeWidth = 232.dp,
        telemetryWidth = 0.dp,
        minimumCenterWidth = 420.dp,
    )
    else -> SignalConsoleMetrics(
        mode = SignalConsoleMode.STACKED,
        contentPadding = 12.dp,
        gap = 10.dp,
        routeWidth = 0.dp,
        telemetryWidth = 0.dp,
        minimumCenterWidth = 0.dp,
    )
}
