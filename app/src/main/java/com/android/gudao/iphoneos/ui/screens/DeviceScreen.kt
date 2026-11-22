package com.android.gudao.iphoneos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gudao.iphoneos.engine.GpuIdentity
import com.android.gudao.iphoneos.engine.QemuFailureKind
import com.android.gudao.iphoneos.state.AppUiState
import com.android.gudao.iphoneos.state.BuildUi
import com.android.gudao.iphoneos.state.DownloadUi
import com.android.gudao.iphoneos.state.QemuUi
import com.android.gudao.iphoneos.ui.Fmt
import com.android.gudao.iphoneos.ui.components.Dot
import com.android.gudao.iphoneos.ui.components.GlassActionButton
import com.android.gudao.iphoneos.ui.components.GlassBanner
import com.android.gudao.iphoneos.ui.components.GlassKey
import com.android.gudao.iphoneos.ui.components.GlassPanel
import com.android.gudao.iphoneos.ui.components.Glyph
import com.android.gudao.iphoneos.ui.components.HeroCard
import com.android.gudao.iphoneos.ui.components.IosGlyph
import com.android.gudao.iphoneos.ui.components.IosProgressBar
import com.android.gudao.iphoneos.ui.components.MetricRow
import com.android.gudao.iphoneos.ui.components.SectionLabel
import com.android.gudao.iphoneos.ui.components.StatusChip
import com.android.gudao.iphoneos.ui.theme.LocalIosAccents
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import com.android.gudao.iphoneos.ui.components.IosPinnedPage
import com.android.gudao.iphoneos.ui.plain.GlasmioAlertButtonRole
import com.android.gudao.iphoneos.ui.plain.GlasmioAlertButtonSpec
import com.android.gudao.iphoneos.ui.plain.GlasmioAlertDialog
import com.android.gudao.iphoneos.ui.plain.GlasmioButton
import com.android.gudao.iphoneos.ui.plain.GlasmioGroupedList
import com.android.gudao.iphoneos.ui.plain.GlasmioListRow
import com.android.gudao.iphoneos.ui.plain.GlasmioSectionTitle

data class DeviceActions(
    val start: () -> Unit,
    val stop: () -> Unit,
    val retry: () -> Unit,
    val compile: () -> Unit,
    val openLogs: () -> Unit,
    val openLibrary: () -> Unit,
    val openScreen: () -> Unit,
    val ignoreStale: () -> Unit,
    val forceCleanup: () -> Unit,
    /** 顶栏的端口按钮：跳到设置页的 MCP 服务区块看地址、令牌与状态。 */
    val openMcpSettings: () -> Unit,
    /** 系统渲染后端：true = Adreno（Turnip/Zink 直通 GPU），false = OSMesa 软件光栅化。 */
    val setGpuBackend: (Boolean) -> Unit,
)

@Composable
fun DeviceScreen(
    state: AppUiState,
    actions: DeviceActions,
    modifier: Modifier = Modifier,
) {
    val accents = LocalIosAccents.current
    val qemu = state.qemu
    val running = qemu is QemuUi.Running
    val phase = (qemu as? QemuUi.Running)?.phase

    IosPinnedPage(
        title = "设备",
        bottomPadding = 144.dp,
        subtitle = "iPhone X · iOS 16.5 (20F66)",
        modifier = modifier.testTag("screen_device"),
        actions = {
            GlasmioButton(
                onClick = actions.openMcpSettings,
                size = null,
                emphasis = GlassEmphasis.Regular,
            ) {
                IosGlyph(Glyph.Port, size = 16.dp)
            }
            Spacer(Modifier.width(8.dp))
            GlasmioButton(onClick = actions.openLogs, size = null, emphasis = GlassEmphasis.Regular) {
                IosGlyph(Glyph.Terminal, size = 16.dp)
            }
        },
    ) {
        when (qemu) {
            is QemuUi.Failed -> item {
                GlassBanner(
                    title = qemu.failure.title,
                    message = buildString {
                        append(qemu.failure.detail)
                        qemu.failure.hint?.let { append("\n\n").append(it) }
                    },
                    tint = accents.red,
                    icon = Glyph.Power,
                    // A missing image is not a retry problem: the next useful action is on the
                    // 资源 page, so the banner sends the user there instead of failing again.
                    primaryLabel = when {
                        qemu.failure.kind == QemuFailureKind.MissingPayload -> "去资源页"
                        qemu.failure.kind == QemuFailureKind.AssetsMissing -> "重新部署"
                        qemu.failure.retryable -> "重试"
                        else -> null
                    },
                    onPrimary = when {
                        qemu.failure.kind == QemuFailureKind.MissingPayload -> actions.openLibrary
                        qemu.failure.kind == QemuFailureKind.AssetsMissing -> actions.retry
                        qemu.failure.retryable -> actions.retry
                        else -> null
                    },
                    secondaryLabel = if (qemu.failure.kind == QemuFailureKind.PortInUse) "强制清理" else "查看日志",
                    onSecondary = if (qemu.failure.kind == QemuFailureKind.PortInUse) actions.forceCleanup else actions.openLogs,
                )
            }

            is QemuUi.StaleImages -> item {
                GlassBanner(
                    title = "镜像版本不一致",
                    message = "公共目录里的 root.apfs 是第 ${qemu.foundRev} 版，本机脚本需要第 ${qemu.expectedRev} 版。" +
                        "直接启动会跑旧镜像：iOS 照样开机，但看不到这次的改动。",
                    tint = accents.orange,
                    icon = Glyph.Circle,
                    primaryLabel = "编译镜像",
                    onPrimary = actions.compile,
                    secondaryLabel = "仍然启动",
                    onSecondary = actions.ignoreStale,
                )
            }

            else -> Unit
        }

        item {
            HeroCard(Modifier.testTag("device_hero")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        text = when (qemu) {
                            is QemuUi.Running -> "运行中"
                            is QemuUi.Starting -> "启动中"
                            is QemuUi.Failed -> "启动失败"
                            is QemuUi.StaleImages -> "待重编"
                            QemuUi.Stopped -> "已停止"
                        },
                        tint = when (qemu) {
                            is QemuUi.Running -> accents.green
                            is QemuUi.Starting -> accents.orange
                            is QemuUi.Failed -> accents.red
                            is QemuUi.StaleImages -> accents.orange
                            QemuUi.Stopped -> LocalContentColor.current.copy(alpha = 0.6f)
                        },
                        icon = Glyph.Circle,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (running) Fmt.elapsed(qemu.sinceEpochMs) else "TCG · 无 GPU",
                        style = IosTypography.footnote,
                        color = LocalContentColor.current.copy(alpha = 0.55f),
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        running -> phase?.label ?: "正在引导 iOS"
                        qemu is QemuUi.Starting -> qemu.step
                        else -> "在这个设备上启动 iOS"
                    },
                    style = IosTypography.title2,
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        running -> "串口日志驱动的阶段判定，下面会随启动推进"
                        qemu is QemuUi.Starting -> "以下步骤都在后台进行，界面保持可操作"
                        else -> "下载固件 → 编译镜像 → 启动 QEMU。全部在本机完成。"
                    },
                    style = IosTypography.footnote,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                )

                Spacer(Modifier.height(16.dp))
                IosProgressBar(
                    fraction = phase?.fraction ?: if (qemu is QemuUi.Starting) 0.06f else 0f,
                    tint = if (running) accents.blue else LocalContentColor.current.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(10.dp))
                StageDots(stage = phase?.stage ?: 0, active = running, tint = accents.blue)

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlasmioButton(
                        onClick = {
                            if (running) actions.stop() else actions.start()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_power"),
                        emphasis = GlassEmphasis.Prominent,
                    ) {
                        IosGlyph(Glyph.Power, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (running) "停止" else "启动 iOS",
                            style = IosTypography.headline,
                            color = if (running) accents.red else LocalContentColor.current,
                        )
                    }
                    GlasmioButton(
                        onClick = actions.openScreen,
                        modifier = Modifier.weight(1f).testTag("btn_open_screen"),
                    ) {
                        IosGlyph(Glyph.Screen, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("实时画面", style = IosTypography.headline)
                    }
                }
            }
        }

        if (running) {
            item { SectionLabel("引导阶段") }
            item {
                GlassPanel {
                    phase?.notes?.takeIf { it.isNotEmpty() }?.forEachIndexed { i, note ->
                        if (i > 0) Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Dot(if (phase.stuck) accents.red else accents.green, 5.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                note,
                                style = IosTypography.caption1,
                                color = LocalContentColor.current.copy(alpha = 0.7f),
                            )
                        }
                    }
                    if (phase == null || phase.notes.isEmpty()) {
                        Text(
                            "正在等待串口输出…",
                            style = IosTypography.caption1,
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        item { SectionLabel("驱动") }
        item {
            val adreno = state.settings.experimentalGpu
            val gpu = state.gpu
            var notice by remember { mutableStateOf<Pair<String, String>?>(null) }
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "OSMesa（CPU 软件光栅化）",
                    subtitle = "任何设备都能跑，画面慢但最稳",
                    leading = { IosGlyph(Glyph.Cpu, size = 20.dp, tint = accents.blue) },
                    trailing = {
                        if (!adreno) {
                            IosGlyph(Glyph.Check, size = 18.dp, tint = accents.blue)
                        }
                    },
                    showTopSeparator = false,
                    onClick = if (adreno && !running) {
                        { actions.setGpuBackend(false) }
                    } else {
                        null
                    },
                )
                GlasmioListRow(
                    title = "Adreno（Turnip + Zink 直通 GPU）",
                    subtitle = when {
                        !state.experimentalGpuAvailable -> "本 APK 未打入 GPU 运行时"
                        gpu.isAdreno && !gpu.isTestedModel ->
                            "检测到 ${gpu.summary} · 未实测，可强行启用"
                        gpu.isAdreno -> "检测到 ${gpu.summary} · 固定 2 核，虚拟网络强制开启"
                        else -> "需要高通 Adreno（6xx/7xx/8xx），本机识别为 ${gpu.summary}"
                    },
                    leading = { IosGlyph(Glyph.Cpu, size = 20.dp, tint = accents.purple) },
                    trailing = {
                        if (adreno) {
                            IosGlyph(Glyph.Check, size = 18.dp, tint = accents.blue)
                        }
                    },
                    onClick = if (!adreno && !running) {
                        {
                            when {
                                !state.experimentalGpuAvailable -> notice =
                                    "缺少 GPU 运行时" to
                                        "当前 APK 是软件模拟版，没有随包携带 remoteGL/Turnip。" +
                                        "请安装 GPU 测试包（构建时加 -PgpuTest=true）。"
                                !gpu.isAdreno -> notice =
                                    "这台设备不是高通 Adreno" to
                                        ("识别结果：${gpu.summary}。" +
                                            "remoteGL 桥只挂了 freedreno 一条 Vulkan 驱动（Turnip），" +
                                            "非 Adreno 的 GPU 请留在 OSMesa。")
                                !gpu.isTestedModel -> notice =
                                    "Adreno ${gpu.model} 未实测" to
                                        ("上游只在高通 830（8xx）上做过真机验收。Turnip 的 a6xx/a7xx 后端" +
                                            "在代码里是通的（642 属于 a6xx），但这条 remoteGL 桥没在 6xx 上跑过，" +
                                            "可能出现黑屏、花屏或直接崩。跑不动就切回 OSMesa，切换不影响镜像。")
                                else -> actions.setGpuBackend(true)
                            }
                        }
                    } else {
                        null
                    },
                )
            }
            val tip = notice
            if (tip != null) {
                val confirmable = tip.first == "Adreno ${gpu.model} 未实测"
                GlasmioAlertDialog(
                    visible = true,
                    onDismissRequest = { notice = null },
                    title = tip.first,
                    message = tip.second,
                    buttons = if (confirmable) {
                        listOf(
                            GlasmioAlertButtonSpec("取消", { notice = null }, GlasmioAlertButtonRole.Cancel),
                            GlasmioAlertButtonSpec(
                                "仍要启用",
                                {
                                    notice = null
                                    actions.setGpuBackend(true)
                                },
                            ),
                        )
                    } else {
                        listOf(
                            GlasmioAlertButtonSpec("知道了", { notice = null }, GlasmioAlertButtonRole.Cancel),
                        )
                    },
                )
            }
        }
        if (state.settings.experimentalGpu) {
            item {
                Text(
                    "Adreno 后端会固定 2 核 MTTCG 并强制开启网络；启动中/运行中不可切换。",
                    color = accents.purple.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        item { SectionLabel("环境") }
        item {
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "处理器核心",
                    detail = when {
                        !state.settings.mttcg -> "${state.settings.smp} 核 · 单线程 TCG（排障基线）"
                        state.settings.smp > com.android.gudao.iphoneos.Constants.SMP_STABLE_MAX ->
                            "${state.settings.smp} 核 ⚠ 超出稳定档，会自动降核"
                        else -> "${state.settings.smp} 核 · MTTCG 并行"
                    },
                    systemName = null,
                    leading = { IosGlyph(Glyph.Cpu, size = 20.dp, tint = accents.blue) },
                    showTopSeparator = false,
                )
                GlasmioListRow(
                    title = "虚拟网络",
                    detail = if (state.settings.netEnabled) "已开启" else "关闭",
                    leading = { IosGlyph(Glyph.Network, size = 20.dp, tint = accents.green) },
                )
                GlasmioListRow(
                    title = "镜像版本",
                    detail = if (state.artifacts.imageRevMatches) "第 ${state.artifacts.imageRev} 版" else "需重编",
                    leading = { IosGlyph(Glyph.Doc, size = 20.dp, tint = accents.purple) },
                )
                GlasmioListRow(
                    title = "固件",
                    detail = if (state.artifacts.firmwarePresent) Fmt.bytes(state.artifacts.firmwareBytes) else "未下载",
                    leading = { IosGlyph(Glyph.Folder, size = 20.dp, tint = accents.orange) },
                )
                GlasmioListRow(
                    title = "运行日志",
                    detail = "${state.logCount} 行",
                    leading = { IosGlyph(Glyph.Terminal, size = 20.dp, tint = accents.teal) },
                    onClick = actions.openLogs,
                )
                GlasmioListRow(
                    title = "资源下载",
                    detail = downloadSubtitle(state.download),
                    leading = { IosGlyph(Glyph.Download, size = 20.dp, tint = accents.blue) },
                    onClick = actions.openLibrary,
                )
            }
        }

        item {
            Text(
                "HackiOS 在 Android 上以 TCG 软件模拟运行 iOS 16.5。首次启动约 2–3 分钟出现锁屏，期间可随时查看实时画面。",
                style = IosTypography.caption1,
                color = LocalContentColor.current.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun StageDots(stage: Int, active: Boolean, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..7) {
            val done = stage >= i && active
            Box(
                Modifier
                    .size(if (done) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) tint else LocalContentColor.current.copy(alpha = if (active) 0.18f else 0.1f)
                    )
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (active) "阶段 ${stage.coerceAtLeast(1)}/7" else "未开始",
            style = IosTypography.caption2,
            color = LocalContentColor.current.copy(alpha = 0.45f),
        )
    }
}

@Composable
fun BuildSummary(build: BuildUi, modifier: Modifier = Modifier) {
    when (build) {
        is BuildUi.Running -> Column(modifier) {
            Text(build.phase, style = IosTypography.subheadline)
            Text(build.detail.ifBlank { "进行中" }, style = IosTypography.caption1)
        }

        else -> Unit
    }
}

/** One-line download state for the "资源下载" row, so the home page never grows a second card. */
private fun downloadSubtitle(d: DownloadUi): String = when (d) {
    DownloadUi.Idle -> "未开始"
    DownloadUi.Checking -> "检查中"
    is DownloadUi.Running -> "下载中 ${(d.fraction * 100).toInt()}%"
    is DownloadUi.Merging -> "合并中 ${(d.fraction * 100).toInt()}%"
    is DownloadUi.Verifying -> "校验中 ${(d.fraction * 100).toInt()}%"
    is DownloadUi.Paused -> "已暂停 · ${Fmt.bytes(d.resumeBytes)}"
    is DownloadUi.Ready -> "已就绪"
    is DownloadUi.Failed -> "下载失败"
}
