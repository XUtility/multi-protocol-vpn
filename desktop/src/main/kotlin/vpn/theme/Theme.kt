package vpn.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Switchable token layer (plan 011 — Aurora Meridian).
 *
 * Every colour the client paints lives in a [PaletteColors] instance; exactly
 * two palettes exist — [Palettes.Classic] (the pre-011 look, hex-for-hex, so
 * the owner's old app is one click away forever) and [Palettes.Aurora] (the
 * luminous default). [C] keeps its historic public property names and simply
 * delegates to the active palette, so no call site had to change to get here.
 *
 * Light uses a cool canvas with the palette's own action colours; dark uses
 * layered surfaces instead of flat black. Screens only consume these tokens,
 * so switching appearance or theme never leaves a mismatched card or border.
 */

/**
 * One colour slot for one mode. All values are DATA — no `if (lightMode)`
 * branches live in here; the mode picks the whole instance.
 */
data class PaletteColors(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceLow: Color,
    val glass: Color,
    val glassStrong: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val accentDim: Color,
    val accentGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textFaint: Color,
    val success: Color,
    val successDim: Color,
    val warning: Color,
    val warningDim: Color,
    val error: Color,
    val errorDim: Color,
    val border: Color,
    val borderStrong: Color,
    val onAccent: Color,
    val titleBar: Color,
    /** Tray fallback glyph plate colour (AWT reads it through `toArgb()`). */
    val trayGlyph: Color,
    /** Tray fallback glyph ink colour. */
    val trayGlyphInk: Color,
) {
    /**
     * name → value for every colour slot. Tests walk this list instead of
     * reflection so the token contract is explicit and greppable.
     */
    fun entries(): List<Pair<String, Color>> = listOf(
        "BgTop" to bgTop, "BgMid" to bgMid, "BgBottom" to bgBottom,
        "Surface" to surface, "SurfaceHigh" to surfaceHigh, "SurfaceLow" to surfaceLow,
        "Glass" to glass, "GlassStrong" to glassStrong,
        "Accent" to accent, "Accent2" to accent2, "Accent3" to accent3,
        "AccentDim" to accentDim, "AccentGlow" to accentGlow,
        "TextPrimary" to textPrimary, "TextSecondary" to textSecondary, "TextFaint" to textFaint,
        "Success" to success, "SuccessDim" to successDim,
        "Warning" to warning, "WarningDim" to warningDim,
        "Error" to error, "ErrorDim" to errorDim,
        "Border" to border, "BorderStrong" to borderStrong,
        "OnAccent" to onAccent, "TitleBar" to titleBar,
        "TrayGlyph" to trayGlyph, "TrayGlyphInk" to trayGlyphInk,
    )
}

/**
 * One drifting light field of the Aurora backdrop, as pure data:
 * centre (fractions of width/height), radius (fraction of width), colour and
 * alpha, plus the fraction of the radius where the field has fully fallen off
 * (the mock's `transparent 64%` stop). The backdrop composable draws from
 * this list and never hardcodes a colour.
 */
data class AuroraField(
    val fx: Float,
    val fy: Float,
    val radiusFrac: Float,
    val color: Color,
    val alpha: Float,
    val falloff: Float,
)

/**
 * A full visual identity: colour data for both modes, the backdrop spec,
 * corner radii and the type families. `null` fonts mean "platform default" —
 * that is how Classic keeps its byte-for-byte look.
 */
data class Palette(
    val id: String,
    val dark: PaletteColors,
    val light: PaletteColors,
    /** Empty = no drifting light fields (Classic keeps the Carbon backdrop). */
    val auroraFields: List<AuroraField>,
    val gridColor: Color,
    val gridCellDp: Float,
    val cornerHeroDp: Float,
    val cornerCardDp: Float,
    val fontDisplay: FontFamily?,
    val fontBody: FontFamily?,
    val fontMono: FontFamily?,
)

object Palettes {
    const val CLASSIC = "classic"
    const val AURORA = "aurora"
    val ALL_IDS = setOf(CLASSIC, AURORA)
    const val DEFAULT_ID = AURORA

    /** Today's look, captured verbatim from the pre-011 `object C`. */
    val Classic = Palette(
        id = CLASSIC,
        dark = PaletteColors(
            bgTop = Color(0xFF0B1020),
            bgMid = Color(0xFF10172B),
            bgBottom = Color(0xFF080C18),
            surface = Color(0xFF151D32),
            surfaceHigh = Color(0xFF1D2942),
            surfaceLow = Color(0xFF10182A),
            glass = Color(0x14FFFFFF),
            glassStrong = Color(0x20FFFFFF),
            accent = Color(0xFF4F46E5),
            accent2 = Color(0xFF7C3AED),
            accent3 = Color(0xFF06B6D4),
            accentDim = Color(0xFFA5B4FC),
            accentGlow = Color(0x304F46E5),
            textPrimary = Color(0xFFF7F8FC),
            textSecondary = Color(0xFFB6C0D4),
            textFaint = Color(0xFF7F8BA3),
            success = Color(0xFF16A368),
            successDim = Color(0x2416A368),
            warning = Color(0xFFD97706),
            warningDim = Color(0x24D97706),
            error = Color(0xFFDC3E5A),
            errorDim = Color(0x24DC3E5A),
            border = Color(0xFF27334E),
            borderStrong = Color(0xFF3A496B),
            onAccent = Color(0xFFFFFFFF),
            titleBar = Color(0xFF0A0F1D),
            trayGlyph = Color(0xFF22D3EE),
            trayGlyphInk = Color(0xFF101114),
        ),
        light = PaletteColors(
            bgTop = Color(0xFFF7F8FC),
            bgMid = Color(0xFFEEF1F8),
            bgBottom = Color(0xFFFAFBFF),
            surface = Color(0xFFFFFFFF),
            surfaceHigh = Color(0xFFF1F3FA),
            surfaceLow = Color(0xFFF8F9FD),
            glass = Color(0x140E1B4D),
            glassStrong = Color(0x1F0E1B4D),
            accent = Color(0xFF4F46E5),
            accent2 = Color(0xFF7C3AED),
            accent3 = Color(0xFF06B6D4),
            accentDim = Color(0xFF4338CA),
            accentGlow = Color(0x304F46E5),
            textPrimary = Color(0xFF172033),
            textSecondary = Color(0xFF52607A),
            textFaint = Color(0xFF8490A8),
            success = Color(0xFF16A368),
            successDim = Color(0x2416A368),
            warning = Color(0xFFD97706),
            warningDim = Color(0x24D97706),
            error = Color(0xFFDC3E5A),
            errorDim = Color(0x24DC3E5A),
            border = Color(0xFFE1E5F0),
            borderStrong = Color(0xFFC6CDDF),
            onAccent = Color(0xFFFFFFFF),
            titleBar = Color(0xFFFFFFFF),
            trayGlyph = Color(0xFF22D3EE),
            trayGlyphInk = Color(0xFF101114),
        ),
        auroraFields = emptyList(),
        gridColor = Color(0xFF4F46E5).copy(alpha = 0.045f),
        gridCellDp = 36f,
        cornerHeroDp = 24f,
        cornerCardDp = 16f,
        fontDisplay = null,
        fontBody = null,
        fontMono = null,
    )

    /**
     * Aurora Meridian (ui-designs-previews/1-aurora-meridian.png):
     * abyss ground, azure = actions, teal = verified/healthy, violet never on
     * text, rose = destructive only. Values follow the mock; the one place it
     * deliberately departs is TextFaint (#5E6E96 in the mock measures 3.8:1 on
     * the ground — below the plan's own WCAG bar, so it is raised to #7A8BB3).
     */
    val Aurora = Palette(
        id = AURORA,
        dark = PaletteColors(
            bgTop = Color(0xFF070B18),
            bgMid = Color(0xFF060913),
            bgBottom = Color(0xFF080D1C),
            surface = Color(0xFF0E1633),
            surfaceHigh = Color(0xFF141B3C),
            surfaceLow = Color(0xFF0B1226),
            glass = Color(0x14FFFFFF),
            glassStrong = Color(0x20FFFFFF),
            accent = Color(0xFF4F7CFF),
            accent2 = Color(0xFFA855F7),
            accent3 = Color(0xFF2DD4BF),
            accentDim = Color(0xFFA7C4FF),
            accentGlow = Color(0x304F7CFF),
            textPrimary = Color(0xFFEAF0FF),
            textSecondary = Color(0xFF9FB0D2),
            textFaint = Color(0xFF7A8BB3),
            success = Color(0xFF2DD4BF),
            successDim = Color(0x242DD4BF),
            warning = Color(0xFFF59E0B),
            warningDim = Color(0x24F59E0B),
            error = Color(0xFFF472B6),
            errorDim = Color(0x24F472B6),
            border = Color(0xFF1E2A4A),
            borderStrong = Color(0xFF2E3E66),
            onAccent = Color(0xFFFFFFFF),
            // rgba(8,12,26,.55) — translucent so the drifting fields read as
            // one surface through the title bar.
            titleBar = Color(0x8C080C1A),
            trayGlyph = Color(0xFF2DD4BF),
            trayGlyphInk = Color(0xFF070B18),
        ),
        light = PaletteColors(
            // Not mocked — derived: same hue family, inverted surfaces, AA on
            // text (faint measures 4.7:1 on white).
            bgTop = Color(0xFFF2F6FF),
            bgMid = Color(0xFFEAF0FB),
            bgBottom = Color(0xFFF5F8FF),
            surface = Color(0xFFFFFFFF),
            surfaceHigh = Color(0xFFE8EEFA),
            surfaceLow = Color(0xFFF2F5FC),
            glass = Color(0x140E1B4D),
            glassStrong = Color(0x1F0E1B4D),
            accent = Color(0xFF2E5FD7),
            accent2 = Color(0xFF7E22CE),
            accent3 = Color(0xFF0E9488),
            accentDim = Color(0xFF1D4ED8),
            accentGlow = Color(0x302E5FD7),
            textPrimary = Color(0xFF101A2E),
            textSecondary = Color(0xFF3D4C6B),
            textFaint = Color(0xFF64748B),
            success = Color(0xFF0D9488),
            successDim = Color(0x240D9488),
            warning = Color(0xFFB45309),
            warningDim = Color(0x24B45309),
            error = Color(0xFFDB2777),
            errorDim = Color(0x24DB2777),
            border = Color(0xFFD8E0F0),
            borderStrong = Color(0xFFB7C3DC),
            onAccent = Color(0xFFFFFFFF),
            titleBar = Color(0xFFF7FAFF),
            trayGlyph = Color(0xFF0E9488),
            trayGlyphInk = Color(0xFFF2F6FF),
        ),
        auroraFields = listOf(
            // The mock's four radial-gradients (ofl/sora png §#d1 CSS), as data.
            AuroraField(fx = 0.16f, fy = 0.24f, radiusFrac = 0.40f, color = Color(0xFF2DD4BF), alpha = 0.44f, falloff = 0.64f),
            AuroraField(fx = 0.74f, fy = 0.14f, radiusFrac = 0.36f, color = Color(0xFF4F7CFF), alpha = 0.42f, falloff = 0.66f),
            AuroraField(fx = 0.64f, fy = 0.82f, radiusFrac = 0.46f, color = Color(0xFFA855F7), alpha = 0.34f, falloff = 0.64f),
            AuroraField(fx = 0.28f, fy = 0.92f, radiusFrac = 0.24f, color = Color(0xFFF472B6), alpha = 0.26f, falloff = 0.70f),
        ),
        gridColor = Color.White.copy(alpha = 0.028f),
        gridCellDp = 44f,
        cornerHeroDp = 18f,
        cornerCardDp = 14f,
        fontDisplay = Sora,
        fontBody = Inter,
        fontMono = JetBrainsMono,
    )

    /** Unknown/garbage ids fall back to the default palette instead of throwing. */
    fun byId(id: String?): Palette = when (id) {
        CLASSIC -> Classic
        AURORA -> Aurora
        else -> byId(DEFAULT_ID)
    }
}

// Vendored OFL binaries under src/main/resources/fonts (see OFL-*.txt there).
// Loaded through the JVM classpath resource loader — the desktop `Font(path)`
// overload, the same route painterResource("multivpn-shield-m.png") already
// proves works in this app.
private val Sora = FontFamily(
    Font("fonts/Sora-SemiBold.ttf", FontWeight.SemiBold),
    Font("fonts/Sora-Bold.ttf", FontWeight.Bold),
)
private val Inter = FontFamily(
    Font("fonts/Inter-Regular.ttf", FontWeight.Normal),
    Font("fonts/Inter-Medium.ttf", FontWeight.Medium),
    Font("fonts/Inter-SemiBold.ttf", FontWeight.SemiBold),
)
private val JetBrainsMono = FontFamily(
    Font("fonts/JetBrainsMono-Medium.ttf", FontWeight.Medium),
)

/**
 * Shared visual language for the whole client. Property names are the
 * historical ones; each now delegates to the palette selected by [configure].
 */
object C {
    /** Runtime appearance switches; configured once per root recomposition. */
    var lightMode: Boolean = false
        private set
    var animationsEnabled: Boolean = true
        private set
    var animationLevel: String = "full"
        private set
    var themeId: String = Palettes.DEFAULT_ID
        private set

    fun configure(light: Boolean, animated: Boolean, level: String = "full", theme: String = Palettes.DEFAULT_ID) {
        lightMode = light
        animationsEnabled = animated
        animationLevel = level
        themeId = if (theme in Palettes.ALL_IDS) theme else Palettes.DEFAULT_ID
    }

    val palette: Palette get() = Palettes.byId(themeId)
    /** True when the Aurora Meridian identity is active. */
    val aurora: Boolean get() = palette.id == Palettes.AURORA
    val active: PaletteColors get() = if (lightMode) palette.light else palette.dark

    val BgTop: Color get() = active.bgTop
    val BgMid: Color get() = active.bgMid
    val BgBottom: Color get() = active.bgBottom

    val Surface: Color get() = active.surface
    val SurfaceHigh: Color get() = active.surfaceHigh
    val SurfaceLow: Color get() = active.surfaceLow
    val Glass: Color get() = active.glass
    val GlassStrong: Color get() = active.glassStrong

    val Accent: Color get() = active.accent
    val Accent2: Color get() = active.accent2
    val Accent3: Color get() = active.accent3
    val AccentDim: Color get() = active.accentDim
    val AccentGlow: Color get() = active.accentGlow

    val TextPrimary: Color get() = active.textPrimary
    val TextSecondary: Color get() = active.textSecondary
    val TextFaint: Color get() = active.textFaint

    val Success: Color get() = active.success
    val SuccessDim: Color get() = active.successDim
    val Warning: Color get() = active.warning
    val WarningDim: Color get() = active.warningDim
    val Error: Color get() = active.error
    val ErrorDim: Color get() = active.errorDim

    val Border: Color get() = active.border
    val BorderStrong: Color get() = active.borderStrong
    val OnAccent: Color get() = active.onAccent
    val TitleBar: Color get() = active.titleBar
    val TrayGlyph: Color get() = active.trayGlyph
    val TrayGlyphInk: Color get() = active.trayGlyphInk

    /** Backdrop spec: the four drifting light fields (empty on Classic). */
    val AuroraFields: List<AuroraField> get() = palette.auroraFields
    val GridColor: Color get() = palette.gridColor
    val GridCellDp: Float get() = palette.gridCellDp

    /** Corner radii, as tokens, so shapes switch with the theme too. */
    val CornerHero: androidx.compose.ui.unit.Dp get() = palette.cornerHeroDp.dp
    val CornerCard: androidx.compose.ui.unit.Dp get() = palette.cornerCardDp.dp

    /** Type tokens. Classic keeps the platform defaults; Aurora is Sora/Inter/JBM. */
    val FontDisplay: FontFamily get() = palette.fontDisplay ?: FontFamily.Default
    val FontBody: FontFamily get() = palette.fontBody ?: FontFamily.Default
    val FontMono: FontFamily get() = palette.fontMono ?: FontFamily.Monospace

    /** Brand gradient shared by primary actions and hero accents. */
    val BrandGradient: Brush
        get() = Brush.linearGradient(listOf(Accent, Accent2))

    /** Subtle diagonal highlight used by the connection hero. */
    val HeroGradient: Brush
        get() = Brush.linearGradient(
            listOf(Accent.copy(alpha = 0.92f), Accent2.copy(alpha = 0.84f), Accent3.copy(alpha = 0.72f)),
        )
}

/** The pre-011 Material3 type scale — Classic uses it untouched. */
private val baseTypography = Typography(
    headlineSmall = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 13.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)

/**
 * Re-point the type scale at the palette's families. With both null (Classic)
 * the base instance is returned untouched — byte-for-byte today's typography.
 * Display face on headlines/titles, body face everywhere else.
 */
internal fun typographyFor(base: Typography, p: Palette): Typography {
    val body = p.fontBody ?: return base
    val display = p.fontDisplay ?: FontFamily.Default
    fun TextStyle.w(f: FontFamily) = copy(fontFamily = f)
    return base.copy(
        displayLarge = base.displayLarge.w(display),
        displayMedium = base.displayMedium.w(display),
        displaySmall = base.displaySmall.w(display),
        headlineLarge = base.headlineLarge.w(display),
        headlineMedium = base.headlineMedium.w(display),
        headlineSmall = base.headlineSmall.w(display),
        titleLarge = base.titleLarge.w(display),
        titleMedium = base.titleMedium.w(display),
        titleSmall = base.titleSmall.w(display),
        bodyLarge = base.bodyLarge.w(body),
        bodyMedium = base.bodyMedium.w(body),
        bodySmall = base.bodySmall.w(body),
        labelLarge = base.labelLarge.w(body),
        labelMedium = base.labelMedium.w(body),
        labelSmall = base.labelSmall.w(body),
    )
}

@Composable
fun MultiVpnTheme(
    light: Boolean = false,
    animations: Boolean = true,
    level: String = "full",
    theme: String = Palettes.DEFAULT_ID,
    content: @Composable () -> Unit,
) {
    C.configure(light, animations, level, theme)
    val colors = C.active
    val palette = C.palette
    val activeScheme = if (light) {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent2,
            onSecondary = colors.onAccent,
            tertiary = colors.accent3,
            background = colors.bgTop,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textSecondary,
            error = colors.error,
            onError = colors.onAccent,
            outline = colors.borderStrong,
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent2,
            onSecondary = colors.onAccent,
            tertiary = colors.accent3,
            background = colors.bgTop,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textSecondary,
            error = colors.error,
            onError = colors.onAccent,
            outline = colors.borderStrong,
        )
    }
    MaterialTheme(
        colorScheme = activeScheme,
        typography = typographyFor(baseTypography, palette),
        content = content,
    )
}
