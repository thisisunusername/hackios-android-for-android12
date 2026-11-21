package com.android.gudao.iphoneos.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.gudao.iphoneos.state.GlassStrength
import com.android.gudao.iphoneos.state.ThemeMode

/**
 * iOS 27 semantic accents. Glasmio supplies the glass materials and component geometry; these are
 * the three colours iOS itself uses for status, layered on top of the system palette.
 */
@Immutable
data class IosAccents(
    val blue: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val purple: Color,
    val teal: Color,
)

val IosAccentsDark = IosAccents(
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    orange = Color(0xFFFF9F0A),
    red = Color(0xFFFF453A),
    purple = Color(0xFFBF5AF2),
    teal = Color(0xFF64D2FF),
)

val IosAccentsLight = IosAccents(
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    orange = Color(0xFFFF9500),
    red = Color(0xFFFF3B30),
    purple = Color(0xFFAF52DE),
    teal = Color(0xFF32ADE6),
)

val LocalIosAccents = compositionLocalOf { IosAccentsDark }

val LocalIsDark = compositionLocalOf { true }

/** Current Liquid Glass strength, so the wallpaper and custom surfaces can match the theme. */
val LocalGlassStrength = compositionLocalOf { GlassStrength.Standard }

/** Extra-small / small / medium / large radii already come from Glasmio's GlassDimensions. */
val ScreenCornerRadius = 34.dp
val CardCornerRadius = 26.dp
val ChipCornerRadius = 999.dp

@Composable
fun resolveDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * Root theme: Glasmio's iOS 27 token set plus the app's accent palette.
 *
 * `reduceTransparency` stays false — the whole point is the Liquid Glass material — but the
 * wallpaper is a controlled mesh gradient rather than a photo, so the refraction has large, smooth
 * areas to bend and stays legible at every text size.
 */
@Composable
fun HackIosTheme(
    mode: ThemeMode = ThemeMode.System,
    strength: GlassStrength = GlassStrength.Standard,
    content: @Composable () -> Unit,
) {
    val dark = resolveDark(mode)
    val accents = if (dark) IosAccentsDark else IosAccentsLight

    // 0.31: 液态玻璃整体下线。Glasmio 的玻璃实现是 AGSL 优先的，`android.graphics.RuntimeShader`
    // 在 API 31/32 上虽然类在、却没有 `(String)` 构造（33 才加），而它的构造点分布在
    // navigation material / morphing nav glass / drop shadow 三条链上，多数还不受
    // `GlassAppearance` 控制。逐点绕过只会一直漏，所以这里直接给 Material3 纯色主题：
    // 着色器一个都不碰，视觉是 iOS 的实色卡片（systemGroupedBackground + 白色卡片）。
    val colorScheme = if (dark) darkColorScheme(
        primary = accents.blue,
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        onBackground = Color(0xFFF2F2F7),
        onSurface = Color(0xFFF2F2F7),
        error = accents.red,
    ) else lightColorScheme(
        primary = accents.blue,
        background = Color(0xFFF2F2F7),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1C1C1E),
        onSurface = Color(0xFF1C1C1E),
        error = accents.red,
    )

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalIosAccents provides accents,
            LocalIsDark provides dark,
            LocalGlassStrength provides strength,
            content = content,
        )
    }
}

/**
 * The sampled backdrop every glass surface refracts.
 *
 * A neutral white/grey mesh, deliberately **uncoloured**: the glass itself is pure white, so the
 * backdrop only has to provide soft luminance steps for the refraction to bend. Tinting it blue or
 * violet was what made the panels look tinted — a white surface over a blue backdrop reads blue.
 *
 * **Rendered once into a bitmap.** `GlasmioBackdropHost` re-draws this layer whenever it re-samples
 * the backdrop, and four full-screen radial gradients per redraw is a measurable cost. Baking it to
 * a half-resolution bitmap turns every redraw into a single texture blit, and gradients this soft
 * lose nothing when upscaled.
 */
@Composable
fun IosWallpaper(
    modifier: Modifier = Modifier,
    dark: Boolean,
    strength: GlassStrength = GlassStrength.Standard,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bitmap = remember(size, dark, strength) {
        if (size.width <= 0 || size.height <= 0) null
        else renderWallpaper(size.width, size.height, dark, strength)
    }
    Box(modifier.fillMaxSize().onSizeChanged { size = it }) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

/** Half-resolution bake: soft gradients lose nothing, and the blit gets 4x cheaper. */
private fun renderWallpaper(width: Int, height: Int, dark: Boolean, strength: GlassStrength): ImageBitmap {
    val w = (width / 2).coerceAtLeast(1)
    val h = (height / 2).coerceAtLeast(1)
    val boost = when (strength) {
        GlassStrength.Subtle -> 0.75f
        GlassStrength.Standard -> 1.00f
        GlassStrength.Strong -> 1.25f
    }

    val baseTop = if (dark) Color(0xFF101215) else Color(0xFFF7F8FA)
    val baseBottom = if (dark) Color(0xFF05060A) else Color(0xFFE4E7EE)
    val high = if (dark) Color(0xFF3A3F47) else Color(0xFFFFFFFF)
    val low = if (dark) Color(0xFF1B1E23) else Color(0xFFD3D8E2)

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)

    paint.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        intArrayOf(baseTop.toArgb(), baseBottom.toArgb()),
        null,
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

    fun blob(cxF: Float, cyF: Float, rF: Float, color: Color, alpha: Float) {
        paint.shader = RadialGradient(
            w * cxF, h * cyF, w * rF,
            color.copy(alpha = (alpha * boost).coerceIn(0f, 1f)).toArgb(),
            Color.Transparent.toArgb(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * cxF, h * cyF, w * rF, paint)
    }

    // Two soft steps are enough for the refraction to bite; fewer draws than four blobs too.
    blob(0.22f, 0.16f, 0.85f, high, if (dark) 0.55f else 0.85f)
    blob(0.78f, 0.82f, 0.80f, low, if (dark) 0.65f else 0.70f)

    return bmp.asImageBitmap()
}
