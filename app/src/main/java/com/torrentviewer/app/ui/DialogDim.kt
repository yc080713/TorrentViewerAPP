package com.torrentviewer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 调整弹窗背后遮罩的暗化程度。
 * Compose Dialog 默认遮罩偏重，会把背景压成一片灰，导致模糊效果看不出来。
 */
@Composable
fun ApplyDialogDim(amount: Float = 0.28f, blurRadiusPx: Int = 0) {
    val view = LocalView.current
    LaunchedEffect(amount, blurRadiusPx) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
        dialogWindow.setDimAmount(amount)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            runCatching { dialogWindow.setBackgroundBlurRadius(blurRadiusPx) }
        }
    }
}
