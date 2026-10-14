package vpn.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vpn.core.Links
import vpn.core.Preflight
import vpn.core.ProxyPorts
import vpn.core.SingBox
import vpn.core.SplitModes
import vpn.core.TrafficStats
import vpn.core.VpnConfig
import vpn.core.VpnModes
import vpn.core.VpnStatus

private object SC {
    val Bg = Color(0xFF0B0F12)
    val Panel = Color(0xFF11171B)
    val PanelLow = Color(0xFF0C1114)
    val PanelHigh = Color(0xFF1B2226)
    val Border = Color(0xFF2D373C)
    val BorderStrong = Color(0xFF4A565B)
    val Text = Color(0xFFEDF2F2)
    val TextMuted = Color(0xFFA5B0B4)
    val TextFaint = Color(0xFF748187)
    val Live = Color(0xFFC8F26A)
    val Action = Color(0xFFFF6B35)
    val Warning = Color(0xFFF5B82E)
    val Error = Color(0xFFFF8A80)
    val CyanLike = Color(0xFF9BE7FF)
}

@Composable
internal fun SignalConsoleScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onShowLog: () -> Unit,
    onDismissError: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenMode: () -> Unit,
    onOpenApps: () -> Unit,
) {
    val state = AppState
    val onToggle: () -> Unit = {
        when (state.vpnStatus) {
            VpnStatus.CONNECTED -> state.disconnectActive()
            VpnStatus.DISCONNECTED, VpnStatus.ERROR -> state.connectActive()
            VpnStatus.CONNECTING -> state.cancelConnect()
            VpnStatus.DISCONNECTING -> Unit
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(SC.Bg),
    ) {
        val metrics = signalConsoleMetricsFor(maxWidth.value)
        Column(
            Modifier
                .fillMaxSize()
                .padding(metrics.contentPadding),
        ) {
            SignalHeader(
                state = state,
                metrics = metrics,
                onToggle = onToggle,
                onOpenPicker = onOpenPicker,
                onOpenMode = onOpenMode,
                onOpenApps = onOpenApps,
            )
            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(metrics.gap))
                SignalErrorStrip(
                    message = errorMessage,
                    onRetry = onRetry,
                    onShowLog = onShowLog,
                    onDismiss = onDismissError,
                )
            }
            Spacer(Modifier.height(metrics.gap))

            when (metrics.mode) {
                SignalConsoleMode.THREE_COLUMN -> Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.gap),
                ) {
                    RouteBrowserPanel(
                        state = state,
                        onSelect = state::selectConfig,
                        onChoose = onOpenPicker,
                        modifier = Modifier.width(metrics.routeWidth).fillMaxHeight(),
                    )
                    ConnectionConsolePanel(
                        state = state,
                        onToggle = onToggle,
                        onPickConfig = onOpenPicker,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    TelemetryPanel(
                        state = state,
                        modifier = Modifier.width(metrics.telemetryWidth).fillMaxHeight(),
                    )
                }

                SignalConsoleMode.TWO_COLUMN -> Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.gap),
                ) {
                    RouteBrowserPanel(
                        state = state,
                        onSelect = state::selectConfig,
                        onChoose = onOpenPicker,
                        modifier = Modifier.width(metrics.routeWidth).fillMaxHeight(),
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ConnectionConsolePanel(
                            state = state,
                            onToggle = onToggle,
                            onPickConfig = onOpenPicker,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(metrics.gap))
                        TelemetryPanel(state = state, modifier = Modifier.fillMaxWidth())
                    }
                }

                SignalConsoleMode.STACKED -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    RouteBrowserPanel(
                        state = state,
                        onSelect = state::selectConfig,
                        onChoose = onOpenPicker,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 320.dp),
                    )
                    Spacer(Modifier.height(metrics.gap))
                    ConnectionConsolePanel(
                        state = state,
                        onToggle = onToggle,
                        onPickConfig = onOpenPicker,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(metrics.gap))
                    TelemetryPanel(state = state, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SignalHeader(
    state: AppState,
    metrics: SignalConsoleMetrics,
    onToggle: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenMode: () -> Unit,
    onOpenApps: () -> Unit,
) {
    val stacked = metrics.mode == SignalConsoleMode.STACKED
    val cfg = state.activeConfig
    val status = state.vpnStatus
    val subtitle = cfg?.let {
        "${Links.label(it.protocol, it.awgVersion)} / ${it.name} / ${it.serverIp}"
    } ?: "No route selected"
    val actionText = when (status) {
        VpnStatus.CONNECTED -> "Disconnect"
        VpnStatus.CONNECTING -> "Cancel"
        VpnStatus.DISCONNECTING -> "Stopping"
        else -> "Connect"
    }
    val actionIcon = if (status == VpnStatus.CONNECTING) Icons.Filled.Close else Icons.Filled.Power
    val actionColor = when (status) {
        VpnStatus.CONNECTED -> SC.Action
        VpnStatus.CONNECTING -> SC.Warning
        VpnStatus.DISCONNECTING -> SC.TextFaint
        else -> SC.Live
    }

    if (stacked) {
        Column(Modifier.fillMaxWidth()) {
            SignalTitleBlock(status = status, subtitle = subtitle, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SignalSmallButton("Routes", Icons.Filled.Public, onOpenPicker, Modifier.weight(1f))
                SignalSmallButton("Mode", Icons.Filled.Tune, onOpenMode, Modifier.weight(1f))
                SignalSmallButton("Apps", Icons.Filled.Apps, onOpenApps, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            SignalPrimaryButton(
                text = actionText,
                icon = actionIcon,
                color = actionColor,
                onClick = onToggle,
                enabled = status != VpnStatus.DISCONNECTING,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignalTitleBlock(
                status = status,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SignalSmallButton("Routes", Icons.Filled.Public, onOpenPicker)
                SignalSmallButton("Mode", Icons.Filled.Tune, onOpenMode)
                SignalSmallButton("Apps", Icons.Filled.Apps, onOpenApps)
                SignalPrimaryButton(
                    text = actionText,
                    icon = actionIcon,
                    color = actionColor,
                    onClick = onToggle,
                    enabled = status != VpnStatus.DISCONNECTING,
                )
            }
        }
    }
}

@Composable
private fun SignalTitleBlock(status: VpnStatus, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.widthIn(max = 560.dp)) {
        Text(
            "Route control",
            color = SC.Text,
            fontSize = 26.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SignalStatusDot(status)
            Spacer(Modifier.width(7.dp))
            Text(
                statusLine(status),
                color = statusColor(status),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                subtitle,
                color = SC.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SignalSmallButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = SC.PanelLow,
        border = BorderStroke(1.dp, SC.Border),
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            Icon(icon, null, tint = SC.TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = SC.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SignalPrimaryButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (enabled) color else SC.PanelHigh
    val content = if (enabled) SC.Bg else SC.TextFaint
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(7.dp),
        color = background,
        border = if (enabled) null else BorderStroke(1.dp, SC.Border),
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
        ) {
            Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun SignalErrorStrip(
    message: String,
    onRetry: () -> Unit,
    onShowLog: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = SC.Error.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, SC.Error.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
        ) {
            Icon(Icons.Filled.ErrorOutline, null, tint = SC.Error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Connection failed", color = SC.Error, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    message,
                    color = SC.TextMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButtonCompact("Log", onShowLog, SC.TextMuted)
            TextButtonCompact("Retry", onRetry, SC.Action)
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, "Dismiss", tint = SC.TextFaint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TextButtonCompact(text: String, onClick: () -> Unit, color: Color) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        modifier = Modifier.defaultMinSize(minHeight = 30.dp),
    ) {
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SignalStatusDot(status: VpnStatus) {
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(statusColor(status)),
    )
}

private fun statusLine(status: VpnStatus): String = when (status) {
    VpnStatus.CONNECTED -> "SECURED"
    VpnStatus.CONNECTING -> "CONNECTING"
    VpnStatus.DISCONNECTING -> "CLOSING"
    VpnStatus.ERROR -> "FAILED"
    VpnStatus.DISCONNECTED -> "STANDBY"
}

private fun statusColor(status: VpnStatus): Color = when (status) {
    VpnStatus.CONNECTED -> SC.Live
    VpnStatus.CONNECTING, VpnStatus.DISCONNECTING -> SC.Warning
    VpnStatus.ERROR -> SC.Error
    VpnStatus.DISCONNECTED -> SC.TextFaint
}

private fun compactModeName(mode: String): String = when (mode) {
    VpnModes.TUN -> "TUN / full system"
    VpnModes.SYSTEM_PROXY -> "System proxy"
    else -> "Proxy only"
}

@Composable
private fun SignalPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SC.Panel,
        border = BorderStroke(1.dp, SC.Border),
        modifier = modifier,
    ) {
        Column(content = content)
    }
}

@Composable
private fun RouteBrowserPanel(
    state: AppState,
    onSelect: (String) -> Unit,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.configs, query) {
        val q = query.trim().lowercase()
        state.configs.filter { cfg ->
            q.isEmpty() ||
                cfg.name.lowercase().contains(q) ||
                cfg.serverIp.lowercase().contains(q) ||
                Links.label(cfg.protocol, cfg.awgVersion).lowercase().contains(q)
        }
    }

    SignalPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalPanelLabel("ROUTES")
                Text(
                    state.configs.size.toString(),
                    color = SC.Live,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search routes", color = SC.TextFaint, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = SC.TextFaint, modifier = Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(7.dp),
                colors = signalTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            if (state.configs.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Text(
                        "No routes yet.",
                        color = SC.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Choose a config or add one from the Servers tab.",
                        color = SC.TextFaint,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    SignalPrimaryButton(
                        text = "Choose route",
                        icon = Icons.Filled.Public,
                        color = SC.Live,
                        onClick = onChoose,
                        enabled = true,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(filtered, key = { it.id }) { cfg ->
                        RouteListItem(
                            config = cfg,
                            selected = cfg.id == state.activeConfigId,
                            latency = state.warmLatency[cfg.id] ?: state.latency[cfg.id],
                            failed = cfg.id in state.latencyFailed,
                            pinging = cfg.id in state.pinging,
                            onClick = { onSelect(cfg.id) },
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No route matches this search.",
                                color = SC.TextFaint,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteListItem(
    config: VpnConfig,
    selected: Boolean,
    latency: Int?,
    failed: Boolean,
    pinging: Boolean,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(
        if (selected) SC.BorderStrong else Color.Transparent,
        tween(180),
        label = "routeBorder",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) SC.PanelHigh else Color.Transparent,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) SC.Live else SC.PanelLow),
            ) {
                Icon(
                    protocolIcon(config.protocol),
                    null,
                    tint = if (selected) SC.Bg else SC.Warning,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    config.name,
                    color = SC.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${Links.label(config.protocol, config.awgVersion)} / ${config.serverIp}",
                    color = SC.TextFaint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(7.dp))
            RouteLatency(latency = latency, failed = failed, pinging = pinging)
        }
    }
}

@Composable
private fun RouteLatency(latency: Int?, failed: Boolean, pinging: Boolean) {
    val (text, color) = when {
        pinging -> "..." to SC.TextFaint
        failed -> "fail" to SC.Error
        latency == null -> "N/A" to SC.TextFaint
        else -> "${latency}ms" to when (vpn.core.LatencyGrade.of(latency)) {
            vpn.core.LatencyGrade.Grade.GOOD -> SC.Live
            vpn.core.LatencyGrade.Grade.FAIR -> SC.Warning
            vpn.core.LatencyGrade.Grade.POOR -> SC.Error
        }
    }
    Text(
        text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SignalPanelLabel(text: String) {
    Text(
        text,
        color = SC.TextFaint,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun signalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SC.Text,
    unfocusedTextColor = SC.Text,
    focusedContainerColor = SC.PanelLow,
    unfocusedContainerColor = SC.PanelLow,
    focusedBorderColor = SC.Live,
    unfocusedBorderColor = SC.Border,
    cursorColor = SC.Live,
)

@Composable
private fun ConnectionConsolePanel(
    state: AppState,
    onToggle: () -> Unit,
    onPickConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cfg = state.activeConfig
    SignalPanel(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .signalGridBackground(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(18.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SignalPanelLabel(if (cfg == null) "NO ROUTE" else "ROUTE / ${cfg.name.uppercase()}")
                    Text(
                        if (state.vpnStatus == VpnStatus.CONNECTED) "LIVE" else "CONTROL",
                        color = if (state.vpnStatus == VpnStatus.CONNECTED) SC.Live else SC.TextFaint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                SignalDial(status = state.vpnStatus, onToggle = onToggle)
                Spacer(Modifier.height(15.dp))
                SignalFactsRow(state = state, config = cfg, onPickConfig = onPickConfig)
                Spacer(Modifier.weight(1f))
                SignalConsoleFooter(
                    status = state.vpnStatus,
                    config = cfg,
                    onPickConfig = onPickConfig,
                )
            }
        }
    }
}

private fun Modifier.signalGridBackground(): Modifier = this.background(
    Brush.linearGradient(listOf(SC.PanelHigh.copy(alpha = 0.42f), SC.PanelLow)),
).border(1.dp, SC.Border, RoundedCornerShape(10.dp))

@Composable
private fun SignalDial(status: VpnStatus, onToggle: () -> Unit) {
    val connected = status == VpnStatus.CONNECTED
    val busy = status == VpnStatus.CONNECTING || status == VpnStatus.DISCONNECTING
    val tone by animateColorAsState(statusColor(status), tween(260), label = "dialTone")
    val transition = rememberInfiniteTransition(label = "signalDial")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "dialPulse",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "dialSweep",
    )
    val dialSize = 218.dp
    val innerSize = 148.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(dialSize)) {
        Canvas(Modifier.fillMaxSize()) {
            val ringWidth = 15.dp.toPx()
            drawCircle(color = SC.PanelLow, radius = size.minDimension / 2f - ringWidth / 2f)
            when {
                connected -> drawCircle(
                    color = tone.copy(alpha = 0.15f + 0.08f * pulse),
                    radius = size.minDimension / 2f - ringWidth / 2f,
                    style = Stroke(width = ringWidth),
                )
                busy -> {
                    drawCircle(
                        color = SC.Border,
                        radius = size.minDimension / 2f - ringWidth / 2f,
                        style = Stroke(width = ringWidth),
                    )
                    drawArc(
                        color = tone,
                        startAngle = sweep - 90f,
                        sweepAngle = 112f,
                        useCenter = false,
                        style = Stroke(width = ringWidth, cap = StrokeCap.Round),
                    )
                }
                else -> drawCircle(
                    color = SC.BorderStrong,
                    radius = size.minDimension / 2f - ringWidth / 2f,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawCircle(
                color = SC.Border.copy(alpha = 0.68f),
                radius = innerSize.toPx() / 2f + 13.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Surface(
            onClick = onToggle,
            enabled = status != VpnStatus.DISCONNECTING,
            shape = CircleShape,
            color = SC.Panel,
            border = BorderStroke(1.dp, SC.BorderStrong),
            modifier = Modifier.size(innerSize),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    if (status == VpnStatus.CONNECTING) Icons.Filled.Close else Icons.Filled.Power,
                    null,
                    tint = tone,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    statusLine(status),
                    color = tone,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                )
                Spacer(Modifier.height(5.dp))
                if (status == VpnStatus.CONNECTED && AppState.sessionStartedAt > 0L) {
                    SignalSessionTimer(AppState.sessionStartedAt, color = SC.TextFaint, fontSize = 10.sp)
                } else {
                    Text(
                        when (status) {
                            VpnStatus.CONNECTING -> "tap to cancel"
                            VpnStatus.DISCONNECTING -> "tearing down"
                            VpnStatus.ERROR -> "open error log"
                            else -> "tap to connect"
                        },
                        color = SC.TextFaint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalFactsRow(
    state: AppState,
    config: VpnConfig?,
    onPickConfig: () -> Unit,
) {
    val latency = config?.let { state.warmLatency[it.id] ?: state.latency[it.id] }
    val failed = config != null && config.id in state.latencyFailed
    val pinging = config != null && config.id in state.pinging
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignalFactChip(Icons.Filled.Public, config?.serverIp ?: "No route", onClick = onPickConfig)
        SignalFactChip(
            config?.let { protocolIcon(it.protocol) } ?: Icons.Filled.Security,
            config?.let { Links.label(it.protocol, it.awgVersion) } ?: "Protocol",
            onClick = onPickConfig,
        )
        SignalFactChip(
            Icons.Filled.Speed,
            when {
                pinging -> "Pinging"
                failed -> "No response"
                latency == null -> "No ping"
                else -> "$latency ms"
            },
            color = when {
                failed -> SC.Error
                latency == null -> SC.TextMuted
                else -> when (vpn.core.LatencyGrade.of(latency)) {
                    vpn.core.LatencyGrade.Grade.GOOD -> SC.Live
                    vpn.core.LatencyGrade.Grade.FAIR -> SC.Warning
                    vpn.core.LatencyGrade.Grade.POOR -> SC.Error
                }
            },
            onClick = onPickConfig,
        )
    }
}

@Composable
private fun SignalFactChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = SC.Text,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = SC.PanelHigh,
        border = BorderStroke(1.dp, SC.Border),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = if (color == SC.Text) SC.TextMuted else color,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SignalConsoleFooter(
    status: VpnStatus,
    config: VpnConfig?,
    onPickConfig: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                when (status) {
                    VpnStatus.CONNECTED -> "Disconnect tears down this route and restores the previous network state."
                    VpnStatus.CONNECTING -> "Cancel aborts the current handshake and releases any partial tunnel state."
                    VpnStatus.DISCONNECTING -> "Cleanup is in progress. The next route can be started when this closes."
                    VpnStatus.ERROR -> "Fix the route or inspect the log before retrying."
                    VpnStatus.DISCONNECTED -> "Select a route, then connect when you are ready."
                },
                color = SC.TextFaint,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (config != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${compactModeName(AppState.settings.mode)} / ${AppState.settings.splitApps.size} split app(s)",
                    color = SC.TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        SignalSmallButton("Details", Icons.Filled.Dns, onPickConfig)
    }
}

@Composable
private fun SignalSessionTimer(startedAt: Long, color: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    var now by remember(startedAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val total = ((now - startedAt) / 1000L).coerceAtLeast(0L)
    Text(
        "%02d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TelemetryPanel(state: AppState, modifier: Modifier = Modifier) {
    val sample = state.traffic
    val rate = state.trafficRate
    val connected = state.vpnStatus == VpnStatus.CONNECTED
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(state.sessionStartedAt) {
        if (state.sessionStartedAt == 0L) history.clear()
    }
    LaunchedEffect(rate) {
        if (rate != null) {
            history.add((rate.rxPerSec + rate.txPerSec).toFloat().coerceAtLeast(0f))
            while (history.size > 28) history.removeAt(0)
        }
    }

    SignalPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(15.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalPanelLabel("LIVE TELEMETRY")
                Text(
                    if (connected) "HEALTHY" else "IDLE",
                    color = if (connected) SC.Live else SC.TextFaint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TelemetryMetric(
                    label = if (sample?.perDirection == true) "DOWNLOAD" else "TRANSFER",
                    value = sample?.let { TrafficStats.formatBytes(it.rx) } ?: "0 B",
                    rate = rate?.let { TrafficStats.formatRate(it.rxPerSec) },
                    color = SC.Live,
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetric(
                    label = if (sample?.perDirection == true) "UPLOAD" else "PROCESS",
                    value = if (sample?.perDirection == true) {
                        sample?.let { TrafficStats.formatBytes(it.tx) } ?: "0 B"
                    } else {
                        sample?.source?.name?.replace('_', ' ') ?: "NO DATA"
                    },
                    rate = if (sample?.perDirection == true) rate?.let { TrafficStats.formatRate(it.txPerSec) } else null,
                    color = SC.Action,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            TrafficHistoryChart(history = history, connected = connected)
            Spacer(Modifier.height(13.dp))
            SignalPanelLabel("SESSION CHECKS")
            Spacer(Modifier.height(9.dp))
            TelemetryFact("Route", state.activeConfig?.name ?: "none")
            TelemetryFact("Protocol", state.activeConfig?.let { Links.label(it.protocol, it.awgVersion) } ?: "none")
            TelemetryFact("Mode", compactModeName(AppState.settings.mode))
            TelemetryFact("DNS", dnsSummary(state))
            TelemetryFact(
                "Split",
                when (AppState.settings.splitMode) {
                    SplitModes.INCLUDE -> "include / ${AppState.settings.splitApps.size} app(s)"
                    SplitModes.EXCLUDE -> "exclude / ${AppState.settings.splitApps.size} app(s)"
                    else -> "off"
                },
            )
            TelemetryFact("Proxy ports", proxySummary(state.activeConfig))
        }
    }
}

@Composable
private fun TelemetryMetric(
    label: String,
    value: String,
    rate: String?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SC.PanelLow,
        border = BorderStroke(1.dp, SC.Border),
        modifier = modifier,
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                label,
                color = SC.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                value,
                color = SC.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                rate ?: " ",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TrafficHistoryChart(history: List<Float>, connected: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SC.PanelLow,
        border = BorderStroke(1.dp, SC.Border),
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(112.dp).padding(10.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val grid = SC.Border.copy(alpha = 0.55f)
                repeat(4) { index ->
                    val y = size.height * index / 3f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                if (history.size >= 2) {
                    val max = history.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                    val step = size.width / (history.size - 1).coerceAtLeast(1)
                    var previous = Offset(0f, size.height - (history.first() / max) * size.height)
                    history.drop(1).forEachIndexed { index, value ->
                        val point = Offset(
                            x = step * (index + 1),
                            y = size.height - (value / max) * size.height,
                        )
                        drawLine(
                            color = SC.Live,
                            start = previous,
                            end = point,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        previous = point
                    }
                } else {
                    val y = size.height * 0.62f
                    drawLine(
                        color = if (connected) SC.Live.copy(alpha = 0.55f) else SC.TextFaint.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Text(
                if (history.size >= 2) "LIVE SAMPLES" else if (connected) "WAITING FOR SAMPLES" else "NO ACTIVE SESSION",
                color = SC.TextFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun TelemetryFact(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SC.TextFaint, fontSize = 10.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = SC.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun dnsSummary(state: AppState): String {
    val cfg = state.activeConfig ?: return "none"
    val mode = state.settings.mode
    val splitMode = state.settings.splitMode
    return when {
        cfg.protocol == "hysteria2" ->
            if (SingBox.dnsPinActive(state.settings.dnsLeakProtection, splitMode)) "pinned" else "system"
        mode == VpnModes.TUN ->
            if (state.settings.dnsLeakProtection && splitMode != SplitModes.INCLUDE) "pinned" else "system"
        mode == VpnModes.SYSTEM_PROXY -> "proxy-aware"
        else -> "unpinned"
    }
}

private fun proxySummary(config: VpnConfig?): String {
    if (config == null) return "none"
    val tun = AppState.settings.mode == VpnModes.TUN
    return if (tun) "SOCKS 127.0.0.1:${ProxyPorts.tunProbe}" else Preflight.endpointSummary(config.protocol)
}
