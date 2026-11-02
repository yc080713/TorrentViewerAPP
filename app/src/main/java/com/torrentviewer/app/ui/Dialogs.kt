package com.torrentviewer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.torrentviewer.app.torrent.TorrentMeta
import com.torrentviewer.app.torrent.TorrentLoad
import com.torrentviewer.app.util.formatDateTime
import com.torrentviewer.app.util.formatSize

/**
 * 种子详情弹窗。
 * 关键点：内容区可滚动，底部两个按钮固定，不随内容滚动。
 */
@Composable
fun TorrentDetailDialog(
    load: TorrentLoad,
    onDismiss: () -> Unit,
    onCopyMagnet: (TorrentMeta) -> Unit,
    onOpenMagnet: (TorrentMeta) -> Unit,
    windowBlurRadiusPx: Int = 0
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
            ApplyDialogDim(blurRadiusPx = windowBlurRadiusPx)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.88f)
        ) {
            val meta = load.meta
            if (meta == null) {
                // 解析失败：与详情页一致，关闭按钮固定在弹窗底部
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        DialogHeader(title = load.displayName, onDismiss = onDismiss)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "解析失败",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = load.error ?: "未知错误",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("关闭", fontSize = 15.sp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        DialogHeader(title = load.displayName, onDismiss = onDismiss)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 可滚动内容
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        InfoRow("名称", meta.name)
                        InfoRow("总大小", formatSize(meta.totalLength))
                        InfoRow("文件数", "${meta.files.size} 个${if (meta.isMultiFile) "（多文件）" else "（单文件）"}")
                        InfoRow("分块大小", formatSize(meta.pieceLength))
                        InfoRow("分块数量", meta.pieceCount.toString())
                        InfoRow("Info Hash", meta.infoHash)
                        InfoRow("私有种子", if (meta.isPrivate) "是" else "否")
                        InfoRow("创建时间", formatDateTime(meta.creationDate))
                        InfoRow("创建工具", meta.createdBy ?: "未知")
                        if (meta.comment != null) InfoRow("备注", meta.comment)

                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Tracker（${meta.trackers.size}）")
                        if (meta.trackers.isEmpty()) {
                            Text("无", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            meta.trackers.forEach { tracker ->
                                Text(
                                    text = tracker,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        SectionTitle("文件列表（${meta.files.size}）")
                        meta.files.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = entry.path,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatSize(entry.length),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SectionTitle("磁力链接")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = meta.magnet,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // 固定底部按钮区
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("关闭", fontSize = 14.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { onCopyMagnet(meta) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("转磁力并复制", fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onOpenMagnet(meta) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("打开磁力链接", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogHeader(title: String, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        TextButton(onClick = onDismiss) {
            Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
