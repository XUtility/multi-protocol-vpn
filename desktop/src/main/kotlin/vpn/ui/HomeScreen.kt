package vpn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import vpn.core.InstalledApp
import vpn.core.Links
import vpn.core.SplitModes
import vpn.core.SshService
import vpn.core.TrafficStats
import vpn.core.VpnConfig
import vpn.core.VpnModes
import vpn.core.VpnService
import vpn.core.VpnStatus
import vpn.theme.C
import vpn.core.Preflight
import vpn.core.ProxyPorts

@Composable
fun HomeScreen() {
    val state = AppState
    var showPicker by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showSplitPicker by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }

    SignalConsoleScreen(
        errorMessage = state.lastError,
        onRetry = { state.connectActive() },
        onShowLog = { showLogDialog = true },
        onDismissError = { state.lastError = "" },
        onOpenPicker = { showPicker = true },
        onOpenMode = { showModeSheet = true },
        onOpenApps = { showSplitPicker = true },
    )

    if (showPicker) ConfigPickerDialog(onDismiss = { showPicker = false })
    if (showLogDialog) ServerLogDialog(onDismiss = { showLogDialog = false })
    if (showSplitPicker) SplitAppsDialog(onDismiss = { showSplitPicker = false })
    if (showModeSheet) {
        ModeDialog(
            onDismiss = { showModeSheet = false },
            onManageApps = {
                showModeSheet = false
                showSplitPicker = true
            },
        )
    }
}

@Composable
private fun ConfigPickerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { AppTextButton("Close", onDismiss, color = C.TextSecondary) },
        title = { Text("Choose a config", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (AppState.configs.isEmpty()) {
                    Text(
                        "No configs yet. Add a server in the Servers tab and run Setup, " +
                            "or paste a share link in Configs.",
                        color = C.TextSecondary,
                        fontSize = 12.5.sp,
                    )
                } else {
                    AppState.configs.forEach { cfg ->
                        val selected = cfg.id == AppState.activeConfigId
                        Surface(
                            onClick = {
                                AppState.selectConfig(cfg.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) C.Accent.copy(alpha = 0.16f) else C.Glass,
                            border = if (selected) {
                                androidx.compose.foundation.BorderStroke(1.dp, C.Accent)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, C.Border)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        cfg.name,
                                        color = C.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                    )
                                    Text(
                                        "${cfg.serverIp}  ·  ${Links.label(cfg.protocol, cfg.awgVersion)}",
                                        color = C.TextSecondary,
                                        fontSize = 11.sp,
                                    )
                                }
                                AppState.latency[cfg.id]?.let { ms ->
                                    LatencyPill(ms, false, cfg.id in AppState.pinging)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = C.Surface,
        titleContentColor = C.TextPrimary,
    )
}

/** Fetches and shows the strongSwan log of the server behind the active config. */
@Composable
fun ServerLogDialog(onDismiss: () -> Unit) {
    var log by remember { mutableStateOf("Fetching server log…") }
    LaunchedEffect(Unit) {
        val config = AppState.activeConfig
        val server = config?.let { cfg -> AppState.servers.firstOrNull { it.ip == cfg.serverIp } }
        log = if (server == null) {
            "No server with SSH credentials matches config " +
                "'${config?.name ?: "?"}'. The log can only be fetched for servers added in the Servers tab."
        } else {
            withContext(Dispatchers.IO) { SshService.fetchStrongswanLog(server) }
        }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(log) { scroll.scrollTo(scroll.maxValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { AppTextButton("Close", onDismiss, color = C.TextSecondary) },
        title = { Text("Server log", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Surface(color = Color(0xFF080C16), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        log,
                        color = Color(0xFFA5F3D0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(11.dp).height(320.dp).verticalScroll(scroll),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AppTextButton(
                        "Copy to clipboard",
                        {
                            runCatching {
                                java.awt.Toolkit.getDefaultToolkit()
                                    .systemClipboard
                                    .setContents(
                                        java.awt.datatransfer.StringSelection(log),
                                        null,
                                    )
                            }
                        },
                        color = C.TextSecondary,
                    )
                }
            }
        },
        containerColor = C.Surface,
        titleContentColor = C.TextPrimary,
    )
}

// ------------------------------------------------------------------
// Traffic mode + split tunneling (home screen)
// ------------------------------------------------------------------

// REMOVED: ModeSummaryCard.
//
// The bottom "Traffic mode" row was the same setting as the header's Mode +
// Apps buttons, and the two renderings actively disagreed: this card said
// "Full-system TUN · split (2)" while the details card said "Split (only 2
// app(s) tunneled)" — full-system TUN and split tunneling are different
// things. [ModeDialog] is still reachable from the header's Mode button, which
// is now the only place the mode is shown or changed.

@Composable
private fun ModeDialog(onDismiss: () -> Unit, onManageApps: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { AppTextButton("Done", onDismiss) },
        title = { Text("Traffic mode", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ModeAndSplitControls(onManageApps = onManageApps)
            }
        },
        containerColor = C.Surface,
        titleContentColor = C.TextPrimary,
    )
}

@Composable
private fun ModeAndSplitControls(onManageApps: () -> Unit) {
    val settings = AppState.settings
    val busy = AppState.connectedOrBusy
    // Intent: the split switch reflects the saved mode even before any app is
    // picked, so the picker stays reachable. Actual per-app routing (engine)
    // only kicks in once apps are selected (splitActive).
    val splitOn = settings.splitMode != SplitModes.OFF
    val splitActive = splitOn && settings.splitApps.isNotEmpty()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SegmentedChip("TUN", settings.mode == VpnModes.TUN, Modifier.weight(1f)) {
                if (!busy) AppState.setMode(VpnModes.TUN)
            }
            SegmentedChip("Proxy only", settings.mode == VpnModes.PROXY_ONLY, Modifier.weight(1f)) {
                if (!busy) AppState.setMode(VpnModes.PROXY_ONLY)
            }
            SegmentedChip("System proxy", settings.mode == VpnModes.SYSTEM_PROXY, Modifier.weight(1f)) {
                if (!busy) AppState.setMode(VpnModes.SYSTEM_PROXY)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            when (settings.mode) {
                VpnModes.TUN -> "A virtual adapter captures ALL system traffic and routes it through the VPN (needs admin)."
                VpnModes.PROXY_ONLY -> "Runs a local proxy but leaves the system proxy untouched — only apps configured manually to use it are routed."
                else -> "Sets the Windows system proxy so apps that honor it go through the VPN (no admin)."
            },
            color = C.TextFaint,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
        )
        if (settings.mode == VpnModes.TUN && !Preflight.isElevated(Preflight.isWindows())) {
            Spacer(Modifier.height(6.dp))
            Text(
                "\u26a0 Administrator rights required \u2014 start MultiVPN with 'Run as administrator' " +
                    "or pick System proxy / Proxy only.",
                color = C.Warning,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
            )
        }
        if (busy) {
            Spacer(Modifier.height(6.dp))
            Text("Disconnect first to change the mode.", color = C.Warning, fontSize = 11.sp)
        }

        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
        Spacer(Modifier.height(13.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Split tunneling", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = C.TextPrimary)
                Text(
                    when {
                        // Windows cannot attribute a plain local-proxy
                        // connection to its process — per-app routing is
                        // only real while the TUN engine runs.
                        !SplitModes.allowedInMode(settings.mode) -> "Unavailable in Proxy-only mode"
                        splitActive -> "Per-app routing is active"
                        splitOn -> "Pick apps to enable routing"
                        else -> "Tunnel only selected applications"
                    },
                    fontSize = 11.sp,
                    color = C.TextSecondary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = splitOn,
                enabled = SplitModes.allowedInMode(settings.mode) || splitOn,
                onCheckedChange = { on ->
                    if (!busy && (on == false || SplitModes.allowedInMode(settings.mode))) {
                        AppState.setSplitMode(
                            if (on) {
                                settings.splitMode.takeIf { it != SplitModes.OFF } ?: SplitModes.INCLUDE
                            } else {
                                SplitModes.OFF
                            },
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = C.Accent,
                    checkedThumbColor = C.OnAccent,
                    uncheckedTrackColor = C.SurfaceHigh,
                    uncheckedThumbColor = C.TextSecondary,
                ),
            )
        }

        if (splitOn) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SegmentedChip("Include", settings.splitMode == SplitModes.INCLUDE, Modifier.weight(1f)) {
                    if (!busy) AppState.setSplitMode(SplitModes.INCLUDE)
                }
                SegmentedChip("Exclude", settings.splitMode == SplitModes.EXCLUDE, Modifier.weight(1f)) {
                    if (!busy) AppState.setSplitMode(SplitModes.EXCLUDE)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { onManageApps() },
                shape = RoundedCornerShape(14.dp),
                color = C.Accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, C.Accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    Icon(Icons.Filled.Apps, null, tint = C.Accent2, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Apps (${settings.splitApps.size})",
                            color = C.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        )
                        Text(
                            if (settings.splitMode == SplitModes.INCLUDE) {
                                "Only the checked apps are tunneled"
                            } else {
                                "Everything tunnels except the checked apps"
                            },
                            color = C.TextFaint,
                            fontSize = 10.5.sp,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.TextFaint)
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "Per-app routing uses the full-system TUN component: one UAC prompt on connect, " +
                    "and the Windows system proxy itself stays OFF — the selected apps are routed " +
                    "through the VPN by the tunnel adapter, all other traffic goes direct exactly " +
                    "as if no VPN existed.",
                color = C.TextFaint,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                "Turn it on to route per application — pick apps with their icons, " +
                    "then choose Include (only those are tunneled) or Exclude (all but those).",
                color = C.TextFaint,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun SplitAppsDialog(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>().apply { addAll(AppState.settings.splitApps) } }
    LaunchedEffect(Unit) { AppState.loadInstalledApps() }

    val scannedKeys = remember(AppState.installedApps) {
        AppState.installedApps.mapNotNull { it.exeName }.map { it.lowercase() }.toSet()
    }
    // Selected processes that came from the user (not found in the scanner).
    val customSelected = selected.filter {
        val k = it.lowercase()
        k !in scannedKeys
    }.sorted()

    val q = query.trim().lowercase()
    val filtered = AppState.installedApps.filter { app ->
        q.isEmpty() ||
            app.name.lowercase().contains(q) ||
            (app.exeName?.lowercase()?.contains(q) == true)
    }
    val pseudoCustom = customSelected.map { name ->
        InstalledApp(key = "custom:$name", name = name, exeName = name, iconSource = null)
    }
    val rows = pseudoCustom + filtered

    fun toggle(proc: String) {
        if (proc in selected) selected.remove(proc) else selected.add(proc)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton("Done", {
                AppState.setSplitApps(selected.toList())
                onDismiss()
            })
        },
        dismissButton = { AppTextButton("Cancel", onDismiss, color = C.TextSecondary) },
        title = { Text("Choose apps for split tunneling", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search installed apps", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = C.TextPrimary,
                        unfocusedTextColor = C.TextPrimary,
                        focusedContainerColor = C.Glass,
                        unfocusedContainerColor = C.Glass,
                        focusedBorderColor = C.Accent,
                        unfocusedBorderColor = C.BorderStrong,
                        focusedLabelColor = C.Accent2,
                        unfocusedLabelColor = C.TextSecondary,
                        cursorColor = C.Accent2,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            AppState.appsLoading && AppState.installedApps.isEmpty() -> "Scanning installed apps…"
                            AppState.appsMessage.isNotEmpty() -> AppState.appsMessage
                            else -> "${AppState.installedApps.size} apps found"
                        },
                        color = C.TextFaint,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (AppState.installedApps.isNotEmpty()) {
                        AppTextButton(
                            "Refresh",
                            { AppState.loadInstalledApps(force = true) },
                            color = C.TextSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Add by process name (e.g. chrome.exe)", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = C.TextPrimary,
                            unfocusedTextColor = C.TextPrimary,
                            focusedContainerColor = C.Glass,
                            unfocusedContainerColor = C.Glass,
                            focusedBorderColor = C.Accent,
                            unfocusedBorderColor = C.BorderStrong,
                            focusedLabelColor = C.Accent2,
                            unfocusedLabelColor = C.TextSecondary,
                            cursorColor = C.Accent2,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    AppButton(
                        "Add",
                        {
                            val t = customName.trim()
                            // Duplicate process names would produce duplicate
                            // LazyColumn keys ("custom:<name>") and crash.
                            if (t.isNotEmpty() && selected.none { it.equals(t, ignoreCase = true) }) {
                                selected.add(t)
                            }
                            customName = ""
                        },
                        compact = true,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (AppState.appsLoading && AppState.installedApps.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                            color = C.Accent2,
                        )
                    }
                } else {
                    LazyColumn(Modifier.height(340.dp).fillMaxWidth()) {
                        items(rows, key = { it.key }) { app ->
                            val proc = app.exeName
                            AppPickerRow(
                                app = app,
                                checked = proc != null && proc in selected,
                                onClick = { if (proc != null) toggle(proc) },
                            )
                        }
                        if (rows.isEmpty()) {
                            item {
                                Text(
                                    "No apps match. Try the search or add a process name manually.",
                                    color = C.TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = C.Surface,
        titleContentColor = C.TextPrimary,
    )
}

@Composable
private fun AppPickerRow(app: InstalledApp, checked: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (checked) C.Accent.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            AppIconImage(app, 30.dp)
            Spacer(Modifier.width(11.dp))
            Text(
                app.name,
                color = C.TextPrimary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (checked) {
                Icon(Icons.Filled.CheckCircle, null, tint = C.Accent2, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.Circle, null, tint = C.TextFaint, modifier = Modifier.size(18.dp))
            }
        }
    }
}
