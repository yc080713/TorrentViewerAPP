package com.torrentviewer.app.file

import java.io.File
import java.io.IOException

/** 基础文件操作：复制 / 移动，带重名自动加序号保护 */
object FileOps {

    private fun uniqueTarget(destDir: File, name: String): File {
        var target = File(destDir, name)
        if (!target.exists()) return target
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var i = 1
        while (true) {
            target = File(destDir, "$base ($i)$ext")
            if (!target.exists()) return target
            i++
        }
    }

    /** 复制到目标目录，返回实际落地的文件 */
    fun copyTo(src: File, destDir: File): File {
        val target = uniqueTarget(destDir, src.name)
        copyToExact(src, target)
        return target
    }

    /** 移动到目标目录，返回实际落地的文件 */
    fun moveTo(src: File, destDir: File): File {
        val target = uniqueTarget(destDir, src.name)
        if (src.renameTo(target)) return target
        // 跨磁盘分区时 renameTo 会失败，降级为复制+删除
        copyToExact(src, target)
        if (!src.deleteRecursively()) throw IOException("已复制但源文件删除失败：${src.name}")
        return target
    }

    private fun copyToExact(src: File, target: File) {
        if (src.isDirectory) {
            if (!target.exists() && !target.mkdirs()) throw IOException("无法创建目录：${target.name}")
            val children = src.listFiles()
            if (children != null) {
                for (child in children) copyToExact(child, File(target, child.name))
            }
        } else {
            target.parentFile?.mkdirs()
            src.inputStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE * 8)
                }
            }
        }
    }

    /** 防止把目录复制到自己内部 */
    fun isSelfOrChild(src: File, destDir: File): Boolean {
        val srcPath = src.canonicalPath + File.separator
        val destPath = destDir.canonicalPath + File.separator
        return destPath.startsWith(srcPath)
    }
}
