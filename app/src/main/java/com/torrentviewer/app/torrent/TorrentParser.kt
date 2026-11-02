package com.torrentviewer.app.torrent

import java.security.MessageDigest

data class TorrentFileEntry(val path: String, val length: Long)

data class TorrentMeta(
    val name: String,
    val infoHash: String,
    val totalLength: Long,
    val pieceLength: Long,
    val pieceCount: Int,
    val files: List<TorrentFileEntry>,
    val isMultiFile: Boolean,
    val trackers: List<String>,
    val comment: String?,
    val createdBy: String?,
    val creationDate: Long?,
    val isPrivate: Boolean
) {
    /** 磁力链接（v1 btih） */
    val magnet: String get() = buildMagnet(infoHash, name, trackers)
}

object TorrentParser {

    fun parse(bytes: ByteArray): TorrentMeta {
        val root = BencodeParser(bytes).parse() as? BNode.BDictionary
            ?: throw BencodeException("顶层不是字典，不是有效的 torrent 文件")

        val info = root["info"] as? BNode.BDictionary
            ?: throw BencodeException("缺少 info 字段，不是有效的 torrent 文件")

        // info hash 必须对 info 字典的原始字节做 SHA1
        val infoBytes = bytes.copyOfRange(info.start, info.end)
        val hash = sha1Hex(infoBytes)

        val name = (info["name"] as? BNode.BString)?.text?.takeIf { it.isNotBlank() } ?: "未命名"
        val pieceLength = (info["piece length"] as? BNode.BInteger)?.value ?: 0L
        val piecesBytes = (info["pieces"] as? BNode.BString)?.bytes?.size ?: 0
        val pieceCount = piecesBytes / 20

        val filesNode = info["files"] as? BNode.BList
        val files: List<TorrentFileEntry> = if (filesNode != null) {
            filesNode.items.mapNotNull { it as? BNode.BDictionary }.map { dict ->
                val segments = (dict["path"] as? BNode.BList)?.items
                    ?.mapNotNull { (it as? BNode.BString)?.text }
                    .orEmpty()
                val length = (dict["length"] as? BNode.BInteger)?.value ?: 0L
                TorrentFileEntry(segments.joinToString("/").ifBlank { "未知文件" }, length)
            }
        } else {
            val length = (info["length"] as? BNode.BInteger)?.value ?: 0L
            listOf(TorrentFileEntry(name, length))
        }

        val trackers = LinkedHashSet<String>()
        (root["announce"] as? BNode.BString)?.text?.takeIf { it.isNotBlank() }?.let { trackers.add(it) }
        (root["announce-list"] as? BNode.BList)?.items?.forEach { tier ->
            (tier as? BNode.BList)?.items?.forEach { entry ->
                (entry as? BNode.BString)?.text?.takeIf { it.isNotBlank() }?.let { trackers.add(it) }
            }
        }

        val creationDate = (root["creation date"] as? BNode.BInteger)?.value
        val isPrivate = ((info["private"] as? BNode.BInteger)?.value ?: 0L) == 1L

        return TorrentMeta(
            name = name,
            infoHash = hash,
            totalLength = files.sumOf { it.length },
            pieceLength = pieceLength,
            pieceCount = pieceCount,
            files = files,
            isMultiFile = filesNode != null,
            trackers = trackers.toList(),
            comment = (root["comment"] as? BNode.BString)?.text?.takeIf { it.isNotBlank() },
            createdBy = (root["created by"] as? BNode.BString)?.text?.takeIf { it.isNotBlank() },
            creationDate = creationDate,
            isPrivate = isPrivate
        )
    }

    private fun sha1Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
        val sb = StringBuilder(40)
        for (b in digest) sb.append("%02x".format(b.toInt() and 0xFF))
        return sb.toString()
    }
}

fun buildMagnet(infoHash: String, name: String, trackers: List<String>): String {
    val sb = StringBuilder("magnet:?xt=urn:btih:").append(infoHash)
    if (name.isNotBlank()) sb.append("&dn=").append(percentEncode(name))
    trackers.filter { it.isNotBlank() }.distinct().forEach { sb.append("&tr=").append(percentEncode(it)) }
    return sb.toString()
}

/** 按 RFC 3986 对查询参数做百分号编码（空格编码为 %20 而不是 +） */
fun percentEncode(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    val sb = StringBuilder(bytes.size * 3)
    for (byte in bytes) {
        val code = byte.toInt() and 0xFF
        val ch = code.toChar()
        val unreserved = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
            ch == '-' || ch == '.' || ch == '_' || ch == '~'
        if (unreserved) sb.append(ch) else sb.append('%').append("%02X".format(code))
    }
    return sb.toString()
}
