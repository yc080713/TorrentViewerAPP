package com.torrentviewer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.torrentviewer.app.settings.SettingsState

@Composable
fun SettingsDialog(
    settings: SettingsState,
    hasStorageAccess: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    DialogHeader(title = "设置", onDismiss = onDismiss)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    // ---------- 外观 ----------
                    SectionLabel("外观")
                    SwitchRow(
                        title = "莫奈取色",
                        subtitle = "根据壁纸生成主题配色（Android 12+）",
                        checked = settings.dynamicColor,
                        onCheckedChange = { settings.updateDynamicColor(it) }
                    )
                    SwitchRow(
                        title = "跟随系统深色模式",
                        subtitle = "关闭后可手动决定明暗",
                        checked = settings.followSystemDark,
                        onCheckedChange = { settings.updateFollowSystemDark(it) }
                    )
                    if (!settings.followSystemDark) {
                        SwitchRow(
                            title = "深色模式",
                            subtitle = "手动切换界面明暗",
                            checked = settings.manualDark,
                            onCheckedChange = { settings.updateManualDark(it) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---------- 权限 ----------
                    SectionLabel("权限")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "文件访问权限",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasStorageAccess) "已授权" else "未授权，无法浏览文件",
                                fontSize = 12.sp,
                                color = if (hasStorageAccess) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        if (!hasStorageAccess) {
                            TextButton(onClick = onRequestPermission) {
                                Text("去授权", fontSize = 13.sp)
                            }
                        } else {
                            Text("✓", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---------- 弹窗 ----------
                    SectionLabel("弹窗")
                    SwitchRow(
                        title = "模糊并压暗背景",
                        subtitle = "解析弹窗弹出时，模糊文件列表并降低亮度",
                        checked = settings.blurDialogBackground,
                        onCheckedChange = { settings.updateBlurDialogBackground(it) }
                    )

                    Spacer(Modifier.height(12.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("完成", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
