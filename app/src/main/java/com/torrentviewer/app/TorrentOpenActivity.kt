package com.torrentviewer.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.torrentviewer.app.settings.SettingsState
import com.torrentviewer.app.torrent.TorrentLoad
import com.torrentviewer.app.torrent.TorrentLoader
import com.torrentviewer.app.ui.TorrentDetailDialog
import com.torrentviewer.app.ui.theme.TorrentViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 外部应用（MT 管理器等）打开 .torrent 时进入这个 Activity。
 * 它只负责：解析 → 弹窗展示 → 关闭后 finish() 回到调用方应用。
 * 不会展示文件管理器界面。
 */
class TorrentOpenActivity : ComponentActivity() {

    private var load by mutableStateOf<TorrentLoad?>(null)
    private var loading by mutableStateOf(true)

    private companion object {
        /** 系统窗口模糊半径（像素），Android 12+ 生效 */
        const val BLUR_RADIUS_PX = 120
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || uri == null) {
            finish()
            return
        }

        val prefs = SettingsState(this)

        setContent {
            val settings = remember { SettingsState(this@TorrentOpenActivity) }
            val darkTheme = if (settings.followSystemDark) isSystemInDarkTheme() else settings.manualDark
            TorrentViewerTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (settings.blurDialogBackground) {
                        // 在模糊之上再叠一层压暗
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.42f))
                        )
                    }
                    if (loading) {
                        LoadingDialog()
                    } else {
                        load?.let { result ->
                            TorrentDetailDialog(
                                load = result,
                                onDismiss = { finish() },
                                onCopyMagnet = { meta ->
                                    copyToClipboard(meta.magnet)
                                    toast("磁力链接已复制")
                                },
                                onOpenMagnet = { meta -> openMagnet(meta.magnet) },
                                windowBlurRadiusPx = if (settings.blurDialogBackground) 120 else 0
                            )
                        }
                    }
                }
            }
        }

        // 必须在 setContent / enableEdgeToEdge 之后再设置，否则窗口属性会被覆盖
        applyWindowBlur()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { TorrentLoader.fromUri(this@TorrentOpenActivity, uri) }
            load = result
            loading = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data ?: return
        loading = true
        load = null
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { TorrentLoader.fromUri(this@TorrentOpenActivity, uri) }
            load = result
            loading = false
        }
    }

    override fun onResume() {
        super.onResume()
        // 窗口属性可能在切换/恢复后被重置，这里重新应用一次
        applyWindowBlur()
    }

    /** Android 12+：用系统窗口模糊把调用方应用的界面糊掉（配合压暗层实现「模糊 + 压暗」） */
    private fun applyWindowBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val enabled = runCatching {
            getSharedPreferences("torrent_viewer_settings", Context.MODE_PRIVATE)
                .getBoolean("blur_dialog_background", true)
        }.getOrDefault(true)
        runCatching {
            val attributes = window.attributes
            attributes.setBlurBehindRadius(if (enabled) BLUR_RADIUS_PX else 0)
            window.attributes = attributes
        }
    }

    private fun copyToClipboard(text: String) {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("torrent", text))
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun openMagnet(magnet: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            toast("没有找到支持磁力链接的应用")
        }
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(14.dp))
                Text("正在解析…", fontSize = 14.sp)
            }
        }
    }
}
