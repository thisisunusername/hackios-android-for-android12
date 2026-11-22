package com.android.gudao.iphoneos.engine

/**
 * 宿主 GPU 的识别结果（Adreno 后端只在高通 Adreno 8xx 上启用）。
 *
 * 解析与平台无关，纯函数，JVM 单测直接覆盖；真正读 GL_RENDERER 的是 [GpuProbe]，它在设备上
 * 用一个 1×1 的 EGL pbuffer 建临时 GLES2 上下文，读完立刻销毁，不碰 Compose 的窗口。
 *
 * 为什么不用 Build.SOC_MODEL 直接推断：SoC 到 GPU 型号的映射随厂商与版本漂移，猜错会让人以为
 * 后端可用，然后在启动时才失败。宁可显示「未识别」，也不把推断当识别结果。
 */
data class GpuIdentity(
    val renderer: String = "",
    val vendor: String = "",
    val source: Source = Source.Unavailable,
    /** 识别不出来时给界面看的一行原因（例如 "SM8750 · EGL 查询失败"）。 */
    val note: String = "",
) {
    enum class Source { Egl, Unavailable }

    enum class Family(val label: String) {
        ADRENO_8XX("高通 Adreno 8xx"),
        ADRENO_OTHER("高通 Adreno 其他系列"),
        OTHER("非高通 GPU"),
        UNKNOWN("未识别"),
    }

    /** Adreno 型号（830、740…）；不是 Adreno 时为 null。 */
    val model: Int? = parseAdrenoModel(renderer, vendor)

    val family: Family = when {
        model == null && source == Source.Unavailable -> Family.UNKNOWN
        model == null -> Family.OTHER
        model in ADRENO_8XX_MIN..ADRENO_8XX_MAX -> Family.ADRENO_8XX
        else -> Family.ADRENO_OTHER
    }

    /** Adreno 8xx 系列（上游实测档）。 */
    val isAdreno8xx: Boolean get() = family == Family.ADRENO_8XX

    /**
     * 任意高通 Adreno（6xx / 7xx / 8xx）。
     *
     * Turnip 的 freedreno 后端本来就把 a6xx 一代覆盖在内（642 就属于 a6xx），上游只把 8xx 写成
     * “支持”是因为 remoteGL 那条桥只在高通 830 上做过真机验收，不是 6xx 跑不起来。
     * 所以门禁改成“是高通 Adreno 就放行，未实测型号二次确认”。
     */
    val isAdreno: Boolean get() = model != null

    /** 我们真正在真机上跑过、验收过的型号（830）。其它 8xx 只做提示，不阻挡。 */
    val isTestedModel: Boolean get() = model == TESTED_MODEL

    /** 界面上显示的一行识别结果。 */
    val summary: String
        get() = when {
            source == Source.Unavailable && renderer.isBlank() -> "未能识别 GPU" + if (note.isBlank()) "" else "（$note）"
            source == Source.Unavailable -> "未能识别 GPU（${renderer}）"
            family == Family.ADRENO_8XX -> "Adreno $model · 高通 8xx 系列"
            family == Family.ADRENO_OTHER -> "Adreno $model · 高通 6xx/7xx 系列"
            else -> "${renderer.trim().ifBlank { "未知 GPU" }} · 非高通 Adreno"
        }

    companion object {
        /** 真机实测过的型号。 */
        const val TESTED_MODEL = 830

        /** 高通 Adreno 8xx 的型号区间。 */
        const val ADRENO_8XX_MIN = 800
        const val ADRENO_8XX_MAX = 899

        /**
         * `Adreno (TM) 830` / `Adreno 740` / `QUALCOMM Adreno (TM) 618` 都归一到型号。
         * 只认紧跟在 Adreno 之后的第一个三位数；四位以上（如果有）不算，避免把驱动版本当型号。
         */
        private val adrenoPattern = Regex("(?i)adreno[^0-9]{0,16}(\\d{3})(?![0-9])")

        fun parseAdrenoModel(renderer: String, vendor: String): Int? {
            val text = if (renderer.isBlank()) vendor else renderer
            val match = adrenoPattern.find(text) ?: return null
            return match.groupValues[1].toIntOrNull()
        }

        fun of(renderer: String, vendor: String, source: Source, note: String = ""): GpuIdentity =
            GpuIdentity(renderer.trim(), vendor.trim(), source, note)
    }
}
