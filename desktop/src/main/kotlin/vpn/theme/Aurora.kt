package vpn.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Aurora Meridian identity, as drawing code (plan 011 §5.3/§5.4).
 *
 * Two signature layers, both driven entirely from palette tokens — nothing in
 * this file hardcodes a colour:
 *
 *  - [AuroraBackdrop]: the four drifting light fields + masked hairline grid
 *    behind the whole window;
 *  - [Meridian]: the luminous arc across the connection card whose travelling
 *    pulse IS the live throughput.
 *
 * Frame budget: the drift is ONE graphicsLayer transform on a cached display
 * list (never re-recorded per frame), and the meridian builds its path in
 * `drawWithCache` so each frame only moves one point along it. No blur, no
 * shaders — every glow is stacked strokes / layered translucent fills,
 * because Compose Desktop has no backdrop API.
 */

/** True when full-intensity motion is allowed; drives every static fallback. */
private val fullMotion: Boolean
    get() = C.animationsEnabled && C.animationLevel == "full"

/**
 * The window-wide backdrop: ground gradient + four radial light fields on a
 * 46 s drift + a hairline grid faded out toward the edges. Under reduced /
 * off motion it renders the same frame at drift = 0 — static, still lit.
 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        // Ground: the mock's vertical abyss gradient.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(C.BgTop, C.BgMid, C.BgBottom))),
        )
        val fields = if (C.lightMode) emptyList() else C.AuroraFields
        if (fields.isNotEmpty()) {
            val drift = if (fullMotion) {
                val t = rememberInfiniteTransition(label = "auroraDrift")
                // 46 s, ease-in-out, ping-pong — the mock's `drift` keyframe.
                t.animateFloat(
                    0f, 1f,
                    infiniteRepeatable(
                        tween(46_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                        RepeatMode.Reverse,
                    ),
                    label = "drift",
                ).value
            } else 0f
            // ONE transform for the whole field group (translate + uniform
            // scale), applied to a cached layer — per-frame cost is a matrix,
            // not a re-record. Scale starts above 1 so the -12% CSS inset of
            // the mock (fields bleeding past the window) is preserved.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1.08f + 0.10f * drift
                        scaleX = s
                        scaleY = s
                        translationX = -0.035f * size.width * drift
                        translationY = 0.025f * size.height * drift
                    }
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        // Brushes are built once per size change, never per frame.
                        val brushes = fields.map { f ->
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to f.color.copy(alpha = f.alpha),
                                    f.falloff to f.color.copy(alpha = 0f),
                                    1f to Color.Transparent,
                                ),
                                center = Offset(f.fx * w, f.fy * h),
                                radius = (f.radiusFrac * w).coerceAtLeast(1f),
                            )
                        }
                        onDrawBehind {
                            brushes.forEach { drawRect(it) }
                        }
                    },
            )
        }
        // Hairline grid (44 dp cells, ~2.8 % white) with a radial mask fading
        // it out toward the edges — one masked layer, recorded once, no
        // animation, so the mask costs nothing per frame.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val cell = C.GridCellDp.dp.toPx()
                    val grid = C.GridColor
                    val mask = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Black,
                            0.55f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.34f),
                        radius = (size.width * 0.75f).coerceAtLeast(1f),
                    )
                    onDrawBehind {
                        val canvas = drawContext.canvas
                        canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                        var gx = 0f
                        while (gx < size.width) {
                            drawLine(grid, Offset(gx, 0f), Offset(gx, size.height), 1f)
                            gx += cell
                        }
                        var gy = 0f
                        while (gy < size.height) {
                            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1f)
                            gy += cell
                        }
                        // DstIn keeps the grid where the mask is opaque and
                        // erases it toward the edges, inside this layer only.
                        drawRect(mask, blendMode = BlendMode.DstIn)
                        canvas.restore()
                    }
                },
        )
    }
}

/**
 * The signature arc: a cubic horizon from YOUR end of the tunnel to the exit
 * node, drawn as a 1.6 dp gradient stroke over two wider low-alpha strokes
 * (stacked glow, no blur), node dots with translucent halos, YOU/EXIT
 * micro-labels, and — while the link is up — a light pulse whose travel time
 * is [pulseSeconds] (see `vpn.core.meridianPulseSeconds`).
 *
 * `pulseSeconds == null` (disconnected) or reduced motion: the arc and nodes
 * stay, the pulse does not — the hairline frame.
 */
@Composable
fun Meridian(
    fromLabel: String,
    toLabel: String,
    pulseSeconds: Double?,
    modifier: Modifier = Modifier,
) {
    val travel: Float = if (pulseSeconds != null && fullMotion) {
        val t = rememberInfiniteTransition(label = "meridian")
        t.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween((pulseSeconds * 1000).toInt().coerceAtLeast(100), easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "pulseTravel",
        ).value
    } else 0f
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // Normalised cubic from the mock: M4,166 C150,78 400,32 616,94 in a 620x176 box.
            val p0 = Offset(0.0065f * size.width, 0.943f * size.height)
            val c1 = Offset(0.242f * size.width, 0.443f * size.height)
            val c2 = Offset(0.645f * size.width, 0.182f * size.height)
            val p3 = Offset(0.994f * size.width, 0.534f * size.height)
            val arc = Path().apply {
                moveTo(p0.x, p0.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p3.x, p3.y)
            }
            val stroke = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to C.Accent3, // teal — you
                    0.52f to C.Accent, // azure — the middle distance
                    1f to C.Accent2, // violet — the exit
                ),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
            )
            val live = pulseSeconds != null
            if (live) {
                // Stacked strokes = the glow, no blur API exists on desktop.
                drawPath(arc, stroke, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round), alpha = 0.06f)
                drawPath(arc, stroke, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round), alpha = 0.12f)
                drawPath(arc, stroke, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round), alpha = 0.9f)
            } else {
                drawPath(arc, stroke, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round), alpha = 0.35f)
            }
            // End nodes + translucent halos (flat discs, not blurred glows).
            drawCircle(C.Accent3.copy(alpha = 0.22f), radius = 11.dp.toPx(), center = p0)
            drawCircle(C.Accent3, radius = 4.dp.toPx(), center = p0)
            drawCircle(C.Accent2.copy(alpha = 0.26f), radius = 13.dp.toPx(), center = p3)
            drawCircle(C.Accent2, radius = 4.5.dp.toPx(), center = p3)
            if (live && fullMotion) {
                // Parametric point on the cubic — four multiplies, no PathMeasure
                // re-walk, allocation-free per frame.
                val u = 1f - travel
                val x = u * u * u * p0.x + 3f * u * u * travel * c1.x + 3f * u * travel * travel * c2.x + travel * travel * travel * p3.x
                val y = u * u * u * p0.y + 3f * u * u * travel * c1.y + 3f * u * travel * travel * c2.y + travel * travel * travel * p3.y
                drawCircle(C.OnAccent.copy(alpha = 0.5f), radius = 7.dp.toPx(), center = Offset(x, y))
                drawCircle(C.OnAccent, radius = 2.6.dp.toPx(), center = Offset(x, y))
            }
        }
        Text(
            fromLabel,
            fontSize = 9.5.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = C.FontMono,
            color = C.TextFaint,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 26.dp),
        )
        Text(
            toLabel,
            fontSize = 9.5.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = C.FontMono,
            color = C.Accent2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 18.dp),
        )
    }
}
