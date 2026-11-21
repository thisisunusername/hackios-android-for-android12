package com.android.gudao.iphoneos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gudao.iphoneos.ui.theme.LocalIsDark
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage

/**
 * Android 12 / 12L 的页面壳。
 *
 * Glasmio 的顶栏（`GlasmioTopBar` → `GlasmioToolbarActionGroup` → `GlasmioToolbarActionHost`）
 * 会无条件 `remember { NavigationMaterialRenderer() }`，而它的构造函数是
 * `new android.graphics.RuntimeShader(...)`。API 31/32 上这个类的签名是另一个（无 `(String)`），
 * 于是启动即 `NoSuchMethodError`：
 *
 * ```
 * No direct method <init>(Ljava/lang/String;)V in class Landroid/graphics/RuntimeShader;
 *   at NavigationMaterialRenderer.<init>(NavigationMaterialRenderer.kt:94)
 *   at IosToolbarActionGroupKt.GlasmioToolbarActionHost(IosToolbarActionGroup.kt:435)
 *   at IosToolbarsKt.GlasmioTopBar(IosToolbars.kt:116)
 * ```
 *
 * `reduceTransparency` 管不到这条路径（它不看 appearance）。所以在 31/32 上索性不走 Glasmio
 * 的页面壳，换成纯 Compose 的等价布局：标题 + 右侧操作区 + LazyColumn。
 * 32 以上一切照旧，一行行为都不变。
 */
@Composable
fun IosPinnedPage(
    title: String,
    bottomPadding: Dp,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        GlasmioPinnedListPage(
            title = title,
            bottomPadding = bottomPadding,
            subtitle = subtitle,
            modifier = modifier,
            actions = actions,
            content = content,
        )
        return
    }

    val dark = LocalIsDark.current
    val fg = if (dark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, color = fg, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, color = fg.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
            content = content,
        )
    }
}
