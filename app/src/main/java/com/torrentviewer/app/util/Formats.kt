package com.torrentviewer.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes < 0) return "未知"
    if (bytes < 1024) return "$bytes B"
    var value = bytes.toDouble()
    var index = -1
    val units = listOf("KB", "MB", "GB", "TB", "PB")
    while (value >= 1024.0 && index < units.size - 1) {
        value /= 1024.0
        index++
    }
    return if (value >= 100) "%.0f %s".format(value, units[index])
    else "%.2f %s".format(value, units[index])
}

fun formatDateTime(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0) return "未知"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochSeconds * 1000))
}

fun formatFileTime(millis: Long): String {
    if (millis <= 0) return ""
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}
