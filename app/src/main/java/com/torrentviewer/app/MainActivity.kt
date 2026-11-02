package com.torrentviewer.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.torrentviewer.app.settings.SettingsState
import com.torrentviewer.app.torrent.TorrentLoad
import com.torrentviewer.app.torrent.TorrentLoader
import com.torrentviewer.app.ui.BatchParseResult
import com.torrentviewer.app.ui.BatchResultDialog
import com.torrentviewer.app.ui.FileBrowserScreen
import com.torrentviewer.app.ui.PermissionScreen
import com.torrentviewer.app.ui.SettingsDialog
import com.torrentviewer.app.ui.TorrentDetailDialog
import com.torrentviewer.app.ui.theme.TorrentViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var pendingUri by mutableStateOf<Uri?>(null)
    private var resumeTick by mutableIntStateOf(0)

    private val legacyPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            resumeTick++
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 有些文件管理器（如 MT 管理器）不会走系统「打开方式」解析，
        // 而是直接把 ACTION_VIEW + 文件 Uri 打给启动页。
        // 这种情况立刻转交给纯弹窗 Activity 并结束自己，避免先闪出文件管理器。
        if (redirectToDialogIfNeeded(intent)) return

        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            val settings = remember { SettingsState(this@MainActivity) }
            val darkTheme = if (settings.followSystemDark) isSystemInDarkTheme() else settings.manualDark
            TorrentViewerTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        pendingUri = pendingUri,
                        resumeTick = resumeTick,
                        settings = settings,
                        onUriConsumed = { pendingUri = null },
                        onRequestAllFilesAccess = { openAllFilesAccessSettings() },
                        onRequestLegacyPermissions = { requestLegacyPermissions() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (redirectToDialogIfNeeded(intent)) return
        readIntent(intent)
    }

    /**
     * 如果启动意图是「查看某个 torrent 文件」，转交给 TorrentOpenActivity（只有弹窗），
     * 然后结束当前 Activity，这样返回键会直接回到调用方应用。
     */
    private fun redirectToDialogIfNeeded(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val data = intent.data ?: return false
        val redirect = Intent(this, TorrentOpenActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(data, intent.type ?: "application/x-bittorrent")
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(redirect)
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    /** 从外部（MT 管理器等）打开 .torrent 时记录下 Uri */
    private fun readIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { pendingUri = it }
        }
    }

    private fun openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        } catch (_: Exception) {
        }
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } catch (_: Exception) {
        }
    }

    private fun requestLegacyPermissions() {
        val perms = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        legacyPermissionLauncher.launch(perms.toTypedArray())
    }
}

private fun hasStorageAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

@Composable
private fun AppRoot(
    pendingUri: Uri?,
    resumeTick: Int,
    settings: SettingsState,
    onUriConsumed: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRequestLegacyPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasAccess by remember { mutableStateOf(hasStorageAccess(context)) }
    var detail by remember { mutableStateOf<TorrentLoad?>(null) }
    var batchResult by remember { mutableStateOf<BatchParseResult?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(resumeTick) { hasAccess = hasStorageAccess(context) }

    // 有权限后才处理外部传入的 .torrent，避免授权页上方弹窗
    // 注意：必须先解析完再清空 pendingUri，否则清空会导致本 effect 被取消
    LaunchedEffect(pendingUri, hasAccess) {
        val uri = pendingUri ?: return@LaunchedEffect
        if (!hasAccess) return@LaunchedEffect
        val load = withContext(Dispatchers.IO) { TorrentLoader.fromUri(context, uri) }
        detail = load
        onUriConsumed()
    }

    // 解析弹窗打开时，可选地模糊并压暗背景
    val dialogVisible = detail != null || batchResult != null
    val blurActive = settings.blurDialogBackground && dialogVisible

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurActive) Modifier.blur(18.dp) else Modifier)
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                if (!hasAccess) {
                    PermissionScreen {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onRequestAllFilesAccess()
                        else onRequestLegacyPermissions()
                        // 从设置页返回时 onResume 会刷新，这里再兜底刷新一次
                        hasAccess = hasStorageAccess(context)
                    }
                } else {
                    FileBrowserScreen(
                        onOpenTorrent = { file ->
                            scope.launch {
                                detail = withContext(Dispatchers.IO) { TorrentLoader.fromFile(file) }
                            }
                        },
                        onOpenSettings = { showSettings = true },
                        onBatchAnalyze = { files ->
                            scope.launch {
                                val loads = withContext(Dispatchers.IO) {
                                    files.map { TorrentLoader.fromFile(it) }
                                }
                                val success = loads.mapNotNull { load ->
                                    load.meta?.let {
                                        BatchParseResult.SuccessItem(load.displayName, it.magnet)
                                    }
                                }
                                val failures = loads.filter { it.meta == null }.map {
                                    BatchParseResult.FailureItem(it.displayName, it.error ?: "未知错误")
                                }
                                batchResult = BatchParseResult(success, failures)
                            }
                        }
                    )
                }
            }
        }

        // 说明：背景暗化由 Dialog 自身的 scrim 完成，这里不再叠加遮罩，避免过暗
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            hasStorageAccess = hasAccess,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onRequestAllFilesAccess()
                else onRequestLegacyPermissions()
            },
            onDismiss = { showSettings = false }
        )
    }

    detail?.let { load ->
        TorrentDetailDialog(
            load = load,
            onDismiss = { detail = null },
            onCopyMagnet = { meta ->
                copyToClipboard(context, meta.magnet)
                toast(context, "磁力链接已复制")
            },
            onOpenMagnet = { meta -> openMagnet(context, meta.magnet) }
        )
    }

    batchResult?.let { result ->
        BatchResultDialog(
            result = result,
            onDismiss = { batchResult = null },
            onCopyMagnets = {
                if (result.success.isEmpty()) {
                    toast(context, "没有成功解析的磁力链接")
                } else {
                    copyToClipboard(context, result.magnetsText)
                    toast(context, "已复制 ${result.success.size} 条磁力链接")
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("torrent", text))
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

private fun openMagnet(context: Context, magnet: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        toast(context, "没有找到支持磁力链接的应用")
    }
}
