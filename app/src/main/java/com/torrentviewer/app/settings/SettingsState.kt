package com.torrentviewer.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用设置，用 SharedPreferences 持久化。
 * 字段都是 Compose State，修改后界面自动重组。
 */
class SettingsState(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 莫奈取色（Android 12+ 动态取色） */
    var dynamicColor by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
        private set

    /** 是否跟随系统深色模式 */
    var followSystemDark by mutableStateOf(prefs.getBoolean(KEY_FOLLOW_SYSTEM_DARK, true))
        private set

    /** 不跟随系统时，手动选择的深色开关 */
    var manualDark by mutableStateOf(prefs.getBoolean(KEY_MANUAL_DARK, false))
        private set

    /** 解析弹窗弹出时是否模糊 + 压暗背景 */
    var blurDialogBackground by mutableStateOf(prefs.getBoolean(KEY_BLUR_DIALOG, true))
        private set

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
    }

    fun updateFollowSystemDark(value: Boolean) {
        followSystemDark = value
        prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_DARK, value).apply()
    }

    fun updateManualDark(value: Boolean) {
        manualDark = value
        prefs.edit().putBoolean(KEY_MANUAL_DARK, value).apply()
    }

    fun updateBlurDialogBackground(value: Boolean) {
        blurDialogBackground = value
        prefs.edit().putBoolean(KEY_BLUR_DIALOG, value).apply()
    }

    private companion object {
        const val PREFS_NAME = "torrent_viewer_settings"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_FOLLOW_SYSTEM_DARK = "follow_system_dark"
        const val KEY_MANUAL_DARK = "manual_dark"
        const val KEY_BLUR_DIALOG = "blur_dialog_background"
    }
}
