package com.torrentviewer.app.torrent

import android.content.Context
import android.net.Uri
import java.io.File

data class TorrentLoad(
    val displayName: String,
    val meta: TorrentMeta?,
    val error: String?,
    val sourceSize: Long
) {
    val isSuccess: Boolean get() = meta != null
}

object TorrentLoader {

    /** 防止误读超大文件把内存撑爆 */
    private const val MAX_BYTES = 64L * 1024 * 1024

    fun fromFile(file: File): TorrentLoad {
        val name = file.name
        return try {
            if (file.length() > MAX_BYTES) {
                failure(name, "文件超过 64MB，拒绝解析")
            } else {
                val bytes = file.readBytes()
                TorrentLoad(name, TorrentParser.parse(bytes), null, file.length())
            }
        } catch (e: Throwable) {
            failure(name, e.message ?: e::class.java.simpleName)
        }
    }

    fun fromUri(context: Context, uri: Uri): TorrentLoad {
        val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "torrent 文件"
        return try {
            val bytes = readBytes(context, uri) ?: return failure(name, "无法读取该文件")
            if (bytes.size > MAX_BYTES) return failure(name, "文件超过 64MB，拒绝解析")
            TorrentLoad(name, TorrentParser.parse(bytes), null, bytes.size.toLong())
        } catch (e: Throwable) {
            failure(name, e.message ?: e::class.java.simpleName)
        }
    }

    private fun readBytes(context: Context, uri: Uri): ByteArray? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (!file.isFile) return null
            return file.readBytes()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun failure(name: String, message: String) = TorrentLoad(name, null, message, 0)
}
