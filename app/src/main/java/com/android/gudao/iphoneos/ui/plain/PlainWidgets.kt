package com.android.gudao.iphoneos.ui.plain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.gudao.iphoneos.ui.theme.LocalIsDark
import io.github.neoruaa.glasmio.glass.GlassEmphasis

/**
 * Android 12 的「普通主题」组件集。
 *
 * Glasmio 0.1.x 的玻璃材质是 AGSL 优先的实现：`RuntimeShader` 在 API 31/32 上虽然类存在，
 * 但没有 `(String)` 构造（那是 33 才加的），所以只要走到它的构造就 `NoSuchMethodError`。
 * 这些构造点分布在 navigation material、morphing nav glass、drop shadow 三条链上，且多数
 * 不受 `GlassAppearance` 控制（顶栏、标签栏、导航投影各自 new 自己的渲染器），逐点打补丁
 * 只会一直往下漏。
 *
 * 这一套 shim 用 Material3 复刻同名 API，行为是「iOS 观感的实色卡片」，一个着色器都不碰。
 */

private val cardLight = Color(0xFFFFFFFF)
private val cardDark = Color(0xFF1C1C1E)
private val bgLight = Color(0xFFF2F2F7)
private val bgDark = Color(0xFF0E0E0F)

@Composable
private fun cardColor(): Color = if (LocalIsDark.current) cardDark else cardLight

@Composable
fun GlasmioCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = cardColor(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Column { content() }
    }
}

/** 分组列表的底色；Glasmio 里是调制玻璃，这里直接给页面底色。 */
fun Modifier.glasmioGroupedListBackground(shape: androidx.compose.ui.graphics.Shape): Modifier = this

@Composable
fun GlasmioSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun GlasmioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val prominent = emphasis == GlassEmphasis.Prominent
    val bg = if (prominent) accent else if (LocalIsDark.current) Color(0xFF2C2C2E) else Color(0xFFE9E9EE)
    val fg = if (prominent) Color.White else MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier.height(size ?: (if (prominent) 50.dp else 34.dp)),
        enabled = enabled,
        color = bg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
fun GlasmioIconButton(
    onClick: () -> Unit,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        color = if (LocalIsDark.current) Color(0xFF2C2C2E) else Color(0xFFE9E9EE),
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun GlasmioGroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cardColor(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Column { content() }
    }
}

@Composable
fun GlasmioListRow(
    title: String,
    subtitle: String? = null,
    detail: String? = null,
    systemName: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showTopSeparator: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        if (showTopSeparator) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                }
                if (systemName != null) {
                    Text(
                        systemName,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (detail != null) {
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
    }
}

@Composable
fun GlasmioSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    backdrop: Any? = null,
    onTintColor: Color = MaterialTheme.colorScheme.primary,
) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
}

@Composable
fun GlasmioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled,
    )
}

@Composable
fun <T> GlasmioSegmentedControl(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    Row(
        modifier
            .background(if (LocalIsDark.current) Color(0xFF2C2C2E) else Color(0xFFE9E9EE), RoundedCornerShape(9.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (key, label) ->
            val active = key == selected
            Surface(
                onClick = { onSelected(key) },
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(7.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

enum class GlasmioAlertButtonRole { Cancel, Destructive, Default }

class GlasmioAlertButtonSpec(
    val label: String,
    val action: () -> Unit,
    val role: GlasmioAlertButtonRole = GlasmioAlertButtonRole.Default,
    val enabled: Boolean = true,
)

@Composable
fun GlasmioAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    buttons: List<GlasmioAlertButtonSpec>,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(color = cardColor(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    buttons.forEach { spec ->
                        TextButton(onClick = spec.action, enabled = spec.enabled) {
                            Text(
                                spec.label,
                                color = when (spec.role) {
                                    GlasmioAlertButtonRole.Destructive -> MaterialTheme.colorScheme.error
                                    GlasmioAlertButtonRole.Cancel -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    GlasmioAlertButtonRole.Default -> MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlasmioBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    fitContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(color = cardColor(), shape = RoundedCornerShape(20.dp)) {
            Box(Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}

@Composable
fun GlasmioActionSheetContent(
    title: String,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (message != null) {
                Spacer(Modifier.height(4.dp))
                Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 12.sp)
            }
        }
        content()
    }
}

/** 二级页（带返回）的纯色版本，替代 scaffold 的 GlasmioPinnedPage。 */
@Composable
fun GlasmioPinnedPage(
    title: String,
    subtitle: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 48.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onNavigateBack,
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        content(androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp))
    }
}
