package com.torrentviewer.app.torrent

/**
 * bencode 解析后的节点。
 * 每个节点保留自己在原始字节中的区间（start/end），
 * 计算 info hash 时必须用 info 字典的原始字节，不能重新编码。
 */
sealed class BNode {
    abstract val start: Int
    abstract val end: Int

    class BInteger(val value: Long, override val start: Int, override val end: Int) : BNode()

    class BString(val bytes: ByteArray, override val start: Int, override val end: Int) : BNode() {
        val text: String get() = String(bytes, Charsets.UTF_8)
    }

    class BList(val items: List<BNode>, override val start: Int, override val end: Int) : BNode()

    class BDictionary(
        val entries: List<Pair<String, BNode>>,
        override val start: Int,
        override val end: Int
    ) : BNode() {
        private val map: Map<String, BNode> = entries.toMap()
        operator fun get(key: String): BNode? = map[key]
    }
}

class BencodeException(message: String) : Exception(message)

/** 严格模式的 bencode 解析器 */
class BencodeParser(private val data: ByteArray) {

    private var pos = 0

    fun parse(): BNode = parseValue(0)

    private fun parseValue(depth: Int): BNode {
        if (depth > 256) throw BencodeException("嵌套层级过深")
        if (pos >= data.size) throw BencodeException("数据意外结束")
        return when (val c = data[pos].toInt().toChar()) {
            'i' -> parseInteger()
            'l' -> parseList(depth)
            'd' -> parseDictionary(depth)
            in '0'..'9' -> parseString()
            else -> throw BencodeException("非法字符 '$c'（位置 $pos）")
        }
    }

    private fun parseInteger(): BNode.BInteger {
        val begin = pos
        pos++ // i
        val numStart = pos
        while (pos < data.size && data[pos].toInt().toChar() != 'e') {
            val ch = data[pos].toInt().toChar()
            if (ch != '-' && ch !in '0'..'9') throw BencodeException("整数格式错误（位置 $pos）")
            pos++
        }
        if (pos >= data.size) throw BencodeException("整数缺少结束符")
        val text = String(data, numStart, pos - numStart, Charsets.US_ASCII)
        pos++ // e
        val value = text.toLongOrNull() ?: throw BencodeException("整数解析失败：$text")
        return BNode.BInteger(value, begin, pos)
    }

    private fun parseString(): BNode.BString {
        val begin = pos
        val lenStart = pos
        while (pos < data.size && data[pos].toInt().toChar() != ':') {
            val ch = data[pos].toInt().toChar()
            if (ch !in '0'..'9') throw BencodeException("字符串长度格式错误（位置 $pos）")
            pos++
        }
        if (pos >= data.size) throw BencodeException("字符串缺少分隔符")
        val lenText = String(data, lenStart, pos - lenStart, Charsets.US_ASCII)
        val len = lenText.toIntOrNull() ?: throw BencodeException("字符串长度解析失败")
        pos++ // ':'
        if (len < 0 || pos + len > data.size) throw BencodeException("字符串长度越界")
        val bytes = data.copyOfRange(pos, pos + len)
        pos += len
        return BNode.BString(bytes, begin, pos)
    }

    private fun parseList(depth: Int): BNode.BList {
        val begin = pos
        pos++ // l
        val items = ArrayList<BNode>()
        while (true) {
            if (pos >= data.size) throw BencodeException("列表缺少结束符")
            if (data[pos].toInt().toChar() == 'e') {
                pos++
                break
            }
            items.add(parseValue(depth + 1))
        }
        return BNode.BList(items, begin, pos)
    }

    private fun parseDictionary(depth: Int): BNode.BDictionary {
        val begin = pos
        pos++ // d
        val entries = ArrayList<Pair<String, BNode>>()
        while (true) {
            if (pos >= data.size) throw BencodeException("字典缺少结束符")
            if (data[pos].toInt().toChar() == 'e') {
                pos++
                break
            }
            val keyNode = parseString()
            val valueNode = parseValue(depth + 1)
            entries.add(keyNode.text to valueNode)
        }
        return BNode.BDictionary(entries, begin, pos)
    }
}
