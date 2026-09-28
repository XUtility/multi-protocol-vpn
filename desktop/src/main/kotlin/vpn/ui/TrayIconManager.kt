package vpn.ui

import androidx.compose.ui.graphics.toArgb
import vpn.core.AppLog
import vpn.core.Proxy
import vpn.core.VpnService
import vpn.core.VpnStatus
import java.awt.AWTException
import java.awt.Color
import java.awt.EventQueue
import java.awt.Graphics2D
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/**
 * System tray integration (3.6.14): a "M" icon in the notification area with
 * a popup menu — show, connect/disconnect, quit — plus a status tooltip.
 *
 * AWT-owned (not Compose): the tray must outlive the Compose window state
 * and react to AppState directly. Double-click opens the window; every menu
 * action marshals onto the Compose UI thread through AppState.scope.
 */
object TrayIconManager {

    @Volatile
    private var icon: TrayIcon? = null

    @Volatile
    private var showWindow: (() -> Unit)? = null

    @Volatile
    private var quitApp: (() -> Unit)? = null

    /** Last status reported, kept so a theme change can repaint the glyph +
     *  tooltip immediately instead of waiting for the next status poll (R6). */
    @Volatile
    private var lastStatus: VpnStatus? = null

    @Volatile
    private var lastConfigName: String? = null

    /** The tray image for the CURRENT palette: brand art, or the palette-tinted
     *  fallback glyph when the packaged PNG is unavailable. */
    private fun buildIcon(): Image =
        loadBrandIcon() ?: renderIcon(vpn.theme.C.TrayGlyph.toArgb())

    /** Installs the tray icon. Safe to call twice; no-op when unsupported. */
    fun install(onShow: () -> Unit, onQuit: () -> Unit) {
        showWindow = onShow
        quitApp = onQuit
        if (!SystemTray.isSupported()) {
            AppLog.i("Tray", "system tray not supported on this platform")
            TraySettings.trayAvailable = false
            return
        }
        if (icon != null) return
        try {
            val popup = PopupMenu().apply {
                val showItem = MenuItem("Open MultiVPN")
                showItem.addActionListener { EventQueue.invokeLater { showWindow?.invoke() } }
                add(showItem)

                val toggleItem = MenuItem("Connect / Disconnect")
                toggleItem.addActionListener {
                    // P2-9 fix: this handler ran connectActive()/disconnectActive()
                    // DIRECTLY on the AWT thread — the sibling items marshal via
                    // EventQueue.invokeLater, and so must this one: connectActive
                    // writes vpnStatus and the connectJob/pollJob handles that
                    // Main-thread readers race on.
                    EventQueue.invokeLater {
                        when (AppState.vpnStatus) {
                            VpnStatus.CONNECTED -> AppState.disconnectActive()
                            VpnStatus.DISCONNECTED, VpnStatus.ERROR -> AppState.connectActive()
                            else -> Unit // mid-flight: let the in-flight flow finish
                        }
                    }
                }
                add(toggleItem)
                add("-")
                val quitItem = MenuItem("Quit (closes tunnel)")
                quitItem.addActionListener { EventQueue.invokeLater { quitApp?.invoke() } }
                add(quitItem)
            }

            // 011: the fallback glyph plate comes from the active palette
            // (AWT needs the ARGB int, not a Compose Color).
            val trayIcon = TrayIcon(buildIcon(), "MultiVPN", popup)
            trayIcon.isImageAutoSize = true
            trayIcon.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        SwingUtilities.invokeLater { showWindow?.invoke() }
                    }
                }
            })
            SystemTray.getSystemTray().add(trayIcon)
            icon = trayIcon
            // Only NOW may a close request hide the window: before this line
            // there is nothing to restore it from (see CloseBehavior).
            TraySettings.trayAvailable = true
            AppLog.i("Tray", "tray icon installed")
        } catch (e: AWTException) {
            TraySettings.trayAvailable = false
            AppLog.e("Tray", "could not install tray icon: ${e.message}")
        } catch (e: Exception) {
            TraySettings.trayAvailable = false
            AppLog.e("Tray", "tray install failed: ${e.message}")
        }
    }

    /** Reflects the connection state in the icon colour + tooltip. */
    fun updateStatus(status: VpnStatus, configName: String?) {
        val trayIcon = icon ?: return
        lastStatus = status
        lastConfigName = configName
        // 011: the status tints now read from the active palette instead of
        // four loose hexes. (The artwork itself stays the Shield-M PNG; the
        // tint only labels the tooltip text below.)
        val (_, label) = when (status) {
            VpnStatus.CONNECTED -> vpn.theme.C.Success.toArgb() to "Connected${configName?.let { " — $it" } ?: ""}"
            VpnStatus.CONNECTING, VpnStatus.DISCONNECTING ->
                vpn.theme.C.Warning.toArgb() to "Working…"
            VpnStatus.ERROR -> vpn.theme.C.Error.toArgb() to "Error — see app log"
            VpnStatus.DISCONNECTED -> vpn.theme.C.Accent3.toArgb() to "Disconnected"
        }
        SwingUtilities.invokeLater {
            runCatching {
                // Keep the approved Shield-M artwork in every state. Status is
                // communicated by the tooltip; replacing the image here used
                // to turn the tray icon back into the old plain M.
                trayIcon.setToolTip("MultiVPN — $label")
            }
        }
    }

    /**
     * R6 — repaint the tray for the CURRENT palette without waiting for the
     * next status poll. Called from the Settings → Appearance theme chips the
     * moment a look is saved: it re-derives the glyph from the active palette
     * (the brand PNG is theme-neutral, but the fallback plate and the tooltip
     * tint are not) and re-applies the last status. AWT mutation stays on the
     * EDT.
     */
    fun refresh() {
        val trayIcon = icon ?: return
        val status = lastStatus
        val configName = lastConfigName
        SwingUtilities.invokeLater {
            runCatching { trayIcon.image = buildIcon() }
            if (status != null) updateStatus(status, configName)
        }
    }

    /** Removes the icon (real quit path). */
    fun remove() {
        icon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        icon = null
        TraySettings.trayAvailable = false
    }

    /** Loads the approved Shield-M brand icon for the tray. */
    private fun loadBrandIcon(): Image? {
        val resource = TrayIconManager::class.java.getResourceAsStream("/multivpn-shield-m.png")
            ?: return null
        return resource.use { ImageIO.read(it) }
    }

    /** Draws a tiny fallback tray glyph if the packaged asset is unavailable. */
    private fun renderIcon(argb: Int): BufferedImage {
        val size = 16
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = img.createGraphics()
        g.color = Color(argb, true)
        g.fillRoundRect(0, 0, size, size, 6, 6)
        // Dark "M" glyph ink from the active palette (AWT RGBA ints; the
        // Color(Long, ...) overload does not exist).
        g.color = java.awt.Color(vpn.theme.C.TrayGlyphInk.toArgb())
        g.font = g.font.deriveFont(java.awt.Font.BOLD, 11f)
        val fm = g.fontMetrics
        val x = ((size - fm.stringWidth("M")) / 2).toInt()
        val y = (size - fm.height) / 2 + fm.ascent
        g.drawString("M", x, y)
        g.dispose()
        return img
    }
}
