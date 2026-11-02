# TorrentViewer · 种子查看器

一个用来查看 `.torrent` 种子文件详细信息的 Android 应用。

Kotlin + Jetpack Compose（Material3），单 Activity + 手写 bencode 解析，无第三方种子解析依赖。

该项目完全由DeepSeek-V4.1-Flash生成
项目灵感来源是115网盘APP无法直接打开我手机上的.torrent种子文件进行云下载，于是就让大肥鱼写了这么个软件来解决我的痛点

## 功能

- **文件管理器**：启动即进入，可逐级浏览外部存储，支持多选、复制、剪切、粘贴
- **批量选择**：全选 / 全选种子 / 反选 / 取消
- **批量转磁力**：多选 `.torrent` 后一键解析，结果分「成功 / 失败」两段展示，右侧带滚动条，可一键复制全部成功磁力链接
- **种子详情**：名称、总大小、文件数、分块大小与数量、Info Hash、私有标记、创建时间/工具、备注、Tracker 列表、文件列表、磁力链接
- **底部固定操作栏**：详情弹窗的「转磁力并复制」「打开磁力链接」始终固定，不随内容滚动
- **外部调用**：从 MT 管理器等文件管理器打开 `.torrent` 时，只弹出半透明解析弹窗，关闭后直接回到调用方应用
- **设置**：莫奈取色开关、跟随系统深色开关（可手动指定明暗）、权限检查、弹窗模糊压暗开关
- **磁力跳转**：`打开磁力链接` 通过 `ACTION_VIEW` 调起系统中支持 `magnet:` 的应用

## 环境要求

- JDK 17 ~ 21（Gradle 8.13 不支持 JDK 22+）
- Android SDK：compileSdk 36
- 可选：Android Studio（打开工程目录即可）

## 构建

```bash
./gradlew assembleDebug
```

Windows 也可直接双击 `build.bat`。

产物：`app/build/outputs/apk/debug/app-debug.apk`

> `gradle-wrapper.properties` 默认使用腾讯云镜像加速（国内下载更快）。
> 如在海外，可改回官方地址：
> `distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip`

## 安装

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次启动会引导授予「所有文件访问权限」（Android 11+），授权后即可浏览 `/storage/emulated/0`。

## 技术要点

- **bencode 解析**：手写严格模式解析器，保留每个节点的原始字节区间
- **Info Hash**：必须对 `info` 字典的**原始字节**做 SHA-1，不能重新编码后再算
- **磁力链接**：`magnet:?xt=urn:btih:<hex>&dn=<name>&tr=<tracker>`，参数按 RFC 3986 百分号编码
- **外部调用**：`TorrentOpenActivity` 使用半透明主题只承载弹窗；对「直接把 Intent 打给启动页」的文件管理器（如 MT 管理器）在 `MainActivity` 做了重定向
- **弹窗模糊**：App 内用 `Modifier.blur`；外部调用走 Android 12+ 系统窗口模糊（`setBackgroundBlurRadius`），设备不支持时降级为压暗

## 已知限制

- 外部调用时的模糊依赖系统跨窗口模糊能力，部分模拟器（如 MuMu）与旧版本系统不渲染，此时只有压暗效果
- 暂不支持 v2-only 种子（BEP 52）的磁力链接

## 目录结构

```
app/src/main/java/com/torrentviewer/app/
├── MainActivity.kt              # 权限门禁、外部 Intent 接收与重定向、弹窗状态
├── TorrentOpenActivity.kt       # 外部打开 .torrent 时只显示弹窗的透明 Activity
├── torrent/
│   ├── Bencode.kt               # bencode 解析
│   ├── TorrentParser.kt         # 元数据提取 + Info Hash + 磁力链接生成
│   └── TorrentLoader.kt         # 从 File / Uri 读取并解析
├── file/FileOps.kt              # 复制、移动、重名保护
├── settings/SettingsState.kt    # SharedPreferences 设置
├── ui/                          # 文件管理器、各类弹窗、主题
└── util/Formats.kt              # 大小、时间格式化
```

## License

未指定。如需开源发布，可自行添加 LICENSE（如 MIT / Apache-2.0）。
