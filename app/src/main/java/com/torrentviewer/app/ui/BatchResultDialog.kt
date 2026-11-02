package com.torrentviewer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** 批量解析结果：成功与失败分开保存，便于分别展示和复制 */
data class BatchParseResult(
    val success: List<SuccessItem>,
    val failures: List<FailureItem>
) {
    data class SuccessItem(val fileName: String, val magnet: String)
    data class FailureItem(val fileName: String, val reason: String)

    /** 只拼接成功解析的磁力链接，一行一个，可直接粘贴进下载器 */
    val magnetsText: String get() = success.joinToString("\n") { it.magnet }
}

private val LogBg = Color(0xFF12151B)
private val LogGreen = Color(0xFFB9E3B0)
private val LogGray = Color(0xFF8B93A1)
private val LogRed = Color(0xFFE88B8B)
private val LogBlue = Color(0xFF9DBBFF)

/**
 * 批量解析结果弹窗。
 * 上半部分：成功解析的文件名 + 磁力链接；
 * 下半部分：解析失败的文件名 + 原因。
 * 右侧带滚动条指示当前滚动位置。
 */
@Composable
fun BatchResultDialog(
    result: BatchParseResult,
    onDismiss: () -> Unit,
    onCopyMagnets: () -> Unit
) {
    val listState = rememberLazyListState()
    val total = result.success.size + result.failures.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
            ApplyDialogDim()
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    DialogHeader(title = "解析结果 · 共 $total 个文件", onDismiss = onDismiss)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 日志区（带右侧滚动条）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(LogBg)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        item {
                            LogLine(
                                "成功 ${result.success.size} · 失败 ${result.failures.size}",
                                LogGray
                            )
                        }

                        if (result.success.isNotEmpty()) {
                            item { SectionHeader("成功解析（${result.success.size}）") }
                            itemsIndexed(result.success) { index, item ->
                                LogLine(
                                    "[${(index + 1).toString().padStart(2, '0')}] ${item.fileName}",
                                    Color.White
                                )
                                LogLine(item.magnet, LogGreen)
                            }
                        }

                        if (result.failures.isNotEmpty()) {
                            item { SectionHeader("解析失败（${result.failures.size}）") }
                            itemsIndexed(result.failures) { index, item ->
                                LogLine(
                                    "[${(index + 1).toString().padStart(2, '0')}] ${item.fileName}",
                                    LogRed
                                )
                                LogLine("     ${item.reason}", LogGray)
                            }
                        }
                    }

                    VerticalScrollIndicator(listState, Modifier.align(Alignment.CenterEnd))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭", fontSize = 14.sp) }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onCopyMagnets,
                        enabled = result.success.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("复制磁力链接（${result.success.size}）", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogLine(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = LogBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp)
    )
}

/** 右侧滚动条：按可见比例绘制滑块，跟随滚动位置移动 */
@Composable
private fun BoxScope.VerticalScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val info by remember { derivedStateOf { state.layoutInfo } }
    val totalItems = info.totalItemsCount
    val visibleItems = info.visibleItemsInfo.size
    val firstVisible = info.visibleItemsInfo.firstOrNull()?.index ?: 0

    if (totalItems <= 0 || visibleItems <= 0 || visibleItems >= totalItems) return

    val maxFirst = (totalItems - visibleItems).coerceAtLeast(1)
    val progress = (firstVisible.toFloat() / maxFirst).coerceIn(0f, 1f)
    val thumbFraction = (visibleItems.toFloat() / totalItems).coerceIn(0.08f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(12.dp)
    ) {
        val trackHeight = maxHeight
        val thumbHeight = trackHeight * thumbFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
                .offset(y = (trackHeight - thumbHeight) * progress)
                .width(4.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.30f))
        )
    }
}
