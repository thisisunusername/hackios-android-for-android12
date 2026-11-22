package com.android.gudao.iphoneos.engine

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Android 12（API 31）上不存在的 Java 9 流 API 的替身。
 *
 * 现场：Adreno 模式一点启动就整进程闪退，日志里是
 *
 * ```
 * NoSuchMethodError: No virtual method readNBytes(I)[B in class Ljava/io/InputStream;
 *   at ImageBuilder.prepareGpuRoot(ImageBuilder.kt:784)
 * ```
 *
 * `InputStream.readNBytes(int)` / `readAllBytes()` 都是 Java 9 才有的方法，Android 只有 API 33
 * 起才在 `core-oj` 里提供；本项目没开 core library desugaring，所以 31/32 上调用会抛
 * `NoSuchMethodError`——它是 `Error` 而不是 `Exception`，连 `catch (e: Exception)` 都接不住，
 * 于是直接带着整个进程走。这里用最朴素的 `read(buf)` 循环重写一份。
 */
object IoCompat {

    /** 最多读 [limit] 字节；流提前结束就返回已读到的部分。 */
    fun readAtMost(input: InputStream, limit: Int): ByteArray {
        require(limit >= 0) { "limit 不能为负" }
        val out = ByteArrayOutputStream(minOf(limit, 64 * 1024))
        val buffer = ByteArray(64 * 1024)
        var remaining = limit
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(remaining, buffer.size))
            if (read < 0) break
            out.write(buffer, 0, read)
            remaining -= read
        }
        return out.toByteArray()
    }
}
