package com.torrentviewer.app.ui

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torrentviewer.app.file.FileOps
import com.torrentviewer.app.util.formatFileTime
import com.torrentviewer.app.util.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class ClipboardState(val paths: List<String>, val move: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(
    onOpenTorrent: (File) -> Unit,
    onOpenSettings: () -> Unit,
    onBatchAnalyze: (List<File>) -> Unit
) {
    val storageRoot = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(storageRoot) }
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var clipboard by remember { mutableStateOf<ClipboardState?>(null) }
    var busyText by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentDir, refreshKey) {
        loading = true
        entries = withContext(Dispatchers.IO) {
            currentDir.listFiles()
                ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
                .orEmpty()
        }
        loading = false
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2200)
            message = null
        }
    }

    BackHandler(enabled = selectionMode || currentDir != storageRoot) {
        if (selectionMode) {
            selectionMode = false
            selected = emptySet()
        } else {
            currentDir.parentFile?.let { currentDir = it }
            selected = emptySet()
        }
    }

    val relativePath = remember(currentDir, storageRoot) {
        if (currentDir == storageRoot) "内部存储"
        else "内部存储 / " + currentDir.absolutePath
            .removePrefix(storageRoot.absolutePath)
            .trimStart('/')
            .replace("/", " / ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---------- 顶部标题 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "文件",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = relativePath,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!selectionMode) {
                TextButton(onClick = {
                    selectionMode = true
                    selected = emptySet()
                }) { Text("选择", fontSize = 13.sp) }
            }
            if (currentDir != storageRoot) {
                TextButton(onClick = {
                    currentDir.parentFile?.let { currentDir = it }
                    selected = emptySet()
                    selectionMode = false
                }) { Text("上一级", fontSize = 13.sp) }
            }
            TextButton(onClick = { refreshKey++ }) { Text("刷新", fontSize = 13.sp) }
            TextButton(onClick = onOpenSettings) { Text("设置", fontSize = 13.sp) }
        }

        if (message != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(
                    text = message.orEmpty(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // ---------- 剪贴板提示 ----------
        if (clipboard != null && !selectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已${if (clipboard!!.move) "剪切" else "复制"} ${clipboard!!.paths.size} 项",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { clipboard = null }) { Text("清除", fontSize = 12.sp) }
                    TextButton(onClick = {
                        val cb = clipboard ?: return@TextButton
                        busyText = if (cb.move) "正在移动…" else "正在复制…"
                        scope.launch {
                            val errors = mutableListOf<String>()
                            withContext(Dispatchers.IO) {
                                cb.paths.forEach { path ->
                                    val src = File(path)
                                    try {
                                        when {
                                            !src.exists() -> errors += "${src.name}: 源文件不存在"
                                            FileOps.isSelfOrChild(src, currentDir) ->
                                                errors += "${src.name}: 不能粘贴到自身目录"
                                            cb.move -> FileOps.moveTo(src, currentDir)
                                            else -> FileOps.copyTo(src, currentDir)
                                        }
                                    } catch (e: Throwable) {
                                        errors += "${src.name}: ${e.message ?: "失败"}"
                                    }
                                }
                            }
                            busyText = null
                            clipboard = null
                            refreshKey++
                            message = if (errors.isEmpty()) "粘贴完成" else "部分失败：${errors.first()}"
                        }
                    }) { Text("粘贴到此处", fontSize = 12.sp) }
                }
            }
        }

        // ---------- 文件列表 ----------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize()
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "这个目录是空的",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(entries, key = { it.absolutePath }) { file ->
                            val isSelected = selected.contains(file.absolutePath)
                            FileRow(
                                file = file,
                                selectionMode = selectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectionMode) {
                                        selected = if (isSelected) selected - file.absolutePath
                                        else selected + file.absolutePath
                                    } else if (file.isDirectory) {
                                        currentDir = file
                                        selected = emptySet()
                                    } else if (file.name.endsWith(".torrent", ignoreCase = true)) {
                                        onOpenTorrent(file)
                                    } else {
                                        message = "不是 .torrent 文件，可长按进入多选"
                                    }
                                },
                                onLongClick = {
                                    selectionMode = true
                                    selected = if (isSelected) selected - file.absolutePath
                                    else selected + file.absolutePath
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 64.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---------- 底部操作栏 ----------
        if (selectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // 第一行：已选数量 + 主操作
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已选 ${selected.size} / ${entries.size} 项",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val files = selected.map { File(it) }
                                    .filter { it.isFile && it.name.endsWith(".torrent", ignoreCase = true) }
                                if (files.isEmpty()) {
                                    message = "请选择至少一个 .torrent 文件"
                                } else {
                                    onBatchAnalyze(files)
                                    selected = emptySet()
                                    selectionMode = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("转换", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                        TextButton(
                            onClick = {
                                if (selected.isEmpty()) {
                                    message = "还没有选中文件"
                                } else {
                                    clipboard = ClipboardState(selected.toList(), move = false)
                                    selected = emptySet()
                                    selectionMode = false
                                    message = "已复制到剪贴板"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("复制", fontSize = 13.sp) }
                        TextButton(
                            onClick = {
                                if (selected.isEmpty()) {
                                    message = "还没有选中文件"
                                } else {
                                    clipboard = ClipboardState(selected.toList(), move = true)
                                    selected = emptySet()
                                    selectionMode = false
                                    message = "已剪切"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("移动", fontSize = 13.sp) }
                    }

                    // 第二行：批量选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selected = entries.map { it.absolutePath }.toSet()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("全选", fontSize = 12.sp) }
                        TextButton(
                            onClick = {
                                selected = entries
                                    .filter { !it.isDirectory && it.name.endsWith(".torrent", ignoreCase = true) }
                                    .map { it.absolutePath }
                                    .toSet()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("全选种子", fontSize = 12.sp) }
                        TextButton(
                            onClick = {
                                selected = entries
                                    .map { it.absolutePath }
                                    .filterNot { selected.contains(it) }
                                    .toSet()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("反选", fontSize = 12.sp) }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                selectionMode = false
                                selected = emptySet()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("取消", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }

    // ---------- 忙碌遮罩 ----------
    if (busyText != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(busyText.orEmpty(), fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    file: File,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else Color.Transparent
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text("✓", fontSize = 12.sp, color = Color.White)
            }
            Spacer(Modifier.width(10.dp))
        }

        FileTypeIcon(
            isDirectory = file.isDirectory,
            isTorrent = !file.isDirectory && file.name.endsWith(".torrent", ignoreCase = true)
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (file.isDirectory) {
                    "文件夹"
                } else {
                    formatSize(file.length()) + " · " + formatFileTime(file.lastModified())
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (!file.isDirectory && file.name.endsWith(".torrent", ignoreCase = true)) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "种子",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun FileTypeIcon(isDirectory: Boolean, isTorrent: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = if (isTorrent) primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(38.dp)) {
        val w = size.width
        val h = size.height
        if (isDirectory) {
            drawRoundRect(
                color = primary.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.10f, h * 0.30f),
                size = Size(w * 0.80f, h * 0.52f),
                cornerRadius = CornerRadius(w * 0.10f)
            )
            drawRoundRect(
                color = primary.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.10f, h * 0.20f),
                size = Size(w * 0.34f, h * 0.16f),
                cornerRadius = CornerRadius(w * 0.06f)
            )
        } else {
            drawRoundRect(
                color = secondary.copy(alpha = 0.20f),
                topLeft = Offset(w * 0.20f, h * 0.12f),
                size = Size(w * 0.60f, h * 0.76f),
                cornerRadius = CornerRadius(w * 0.08f)
            )
            drawRoundRect(
                color = secondary.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.30f, h * 0.30f),
                size = Size(w * 0.40f, h * 0.06f),
                cornerRadius = CornerRadius(w * 0.03f)
            )
            drawRoundRect(
                color = secondary.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.30f, h * 0.44f),
                size = Size(w * 0.40f, h * 0.05f),
                cornerRadius = CornerRadius(w * 0.03f)
            )
            drawRoundRect(
                color = secondary.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.30f, h * 0.56f),
                size = Size(w * 0.26f, h * 0.05f),
                cornerRadius = CornerRadius(w * 0.03f)
            )
        }
    }
}
