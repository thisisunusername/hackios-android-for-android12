package com.android.gudao.iphoneos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gudao.iphoneos.ui.theme.LocalIosAccents
import com.android.gudao.iphoneos.ui.theme.LocalIsDark
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/**
 * 底部标签栏的 Android 12 兜底。
 *
 * `GlasmioNavigationBar` 内部会 `rememberMorphingNavigationGlass(...)`，那条路径不看
 * `GlassAppearance`，而是直接构造 `GlassMorphRenderer`，后者在 `<init>` 里就是
 * `new android.graphics.RuntimeShader(shader)`。API 31/32 的 `RuntimeShader` 没有 `(String)`
 * 构造，于是启动崩在：
 *
 * ```
 * NoSuchMethodError: No direct method <init>(Ljava/lang/String;)V in class Landroid/graphics/RuntimeShader;
 *   at GlassMorphRenderer.<init>(GlassMorphRenderer.kt:144)
 *   at rememberMorphingNavigationGlass(GlassMorphRenderer.kt:829)
 *   at GlasmioNavigationBar(GlassBottomBar.kt:653)
 * ```
 *
 * 32 以上原样用 Glasmio 版本；31/32 换成纯 Compose 的标签栏：实色底、四等分、选中项染强调色。
 */
@Composable
fun <T> IosTabBar(
    items: List<GlasmioTabItem<T>>,
    selectedKey: T,
    onSelected: (T) -> Unit,
    onExpand: () -> Unit,
    compactProgress: State<Float>,
    compactSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        GlasmioNavigationBar(
            items = items,
            selectedKey = selectedKey,
            onSelected = onSelected,
            onExpand = onExpand,
            compactProgress = compactProgress,
            compactSize = compactSize,
            modifier = modifier,
        )
        return
    }

    val dark = LocalIsDark.current
    val accents = LocalIosAccents.current
    val barBg = if (dark) Color(0xF21C1C1E) else Color(0xF2F9F9FB)
    val inactive = if (dark) Color(0xFF8E8E93) else Color(0xFF8A8A8E)

    Row(
        modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            Box(
                Modifier
                    .weight(1f)
                    .clickable { onSelected(item.key) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .background(
                            if (selected) accents.blue.copy(alpha = 0.14f) else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        color = if (selected) accents.blue else inactive,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
