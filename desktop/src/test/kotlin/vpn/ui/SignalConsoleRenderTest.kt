package vpn.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import vpn.core.AppSettings
import vpn.core.SplitModes
import vpn.core.VpnModes
import vpn.core.VpnStatus
import vpn.theme.MultiVpnTheme

/**
 * The dashboard has no golden image because fonts and antialiasing vary by
 * machine, but it must still actually compose at the two layouts users hit:
 * the wide desktop console and the compact stacked view. The PNGs are written
 * to build/screenshots for manual review; the assertion catches a composition
 * that falls back to a flat/blank frame.
 */
class SignalConsoleRenderTest {

    @Test
    fun `renders wide and compact signal console frames`() {
        val oldStatus = AppState.vpnStatus
        val oldSettings = AppState.settings
        val oldActive = AppState.activeConfigId
        val oldStarted = AppState.sessionStartedAt
        try {
            AppState.vpnStatus = VpnStatus.CONNECTED
            AppState.activeConfigId = "render-fixture"
            AppState.sessionStartedAt = System.currentTimeMillis() - 754_000L
            AppState.settings = AppSettings(
                mode = VpnModes.TUN,
                dnsLeakProtection = true,
                splitMode = SplitModes.INCLUDE,
                splitApps = listOf("chrome.exe", "code.exe", "slack.exe"),
            )

            renderFrame("wide", 1280, 760)
            renderFrame("compact", 430, 820)
        } finally {
            AppState.vpnStatus = oldStatus
            AppState.settings = oldSettings
            AppState.activeConfigId = oldActive
            AppState.sessionStartedAt = oldStarted
        }
    }

    private fun renderFrame(name: String, width: Int, height: Int) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
        ) {
            MultiVpnTheme {
                ProvideLayout(width.toFloat()) {
                    SignalConsoleScreen(
                        errorMessage = "",
                        onRetry = {},
                        onShowLog = {},
                        onDismissError = {},
                        onOpenPicker = {},
                        onOpenMode = {},
                        onOpenApps = {},
                    )
                }
            }
        }
        try {
            val image = scene.render()
            assertEquals(width, image.width)
            assertEquals(height, image.height)
            val data = image.encodeToData(EncodedImageFormat.PNG)
                ?: error("Signal Console rendered no PNG data")
            val out = File("build/screenshots/signal-console-$name.png")
            out.parentFile.mkdirs()
            out.writeBytes(data.bytes)
            assertTrue(data.bytes.size > 10_000, "$name render is suspiciously empty")
        } finally {
            scene.close()
        }
    }
}
