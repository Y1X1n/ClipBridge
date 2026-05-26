# ClipBridge

## 中文

ClipBridge 是一个面向 Windows 和 Android 的本地优先剪贴板桥接工具。

首版设计目标：

- Windows 桌面端 + Android 移动端
- 仅通过局域网 / P2P 通信
- 不使用云端服务
- 不需要账号系统
- 不需要自备服务器
- 首版优先同步文本和链接

### 仓库结构

```text
apps/windows   Tauri + React Windows 客户端
apps/android   Kotlin 原生 Android 客户端
docs           产品与协议文档
```

### MVP 流程

1. Windows 在局域网内启动本地 WebSocket 服务。
2. Windows 展示可扫码配对二维码和手动连接地址。
3. Android 通过扫码或手动输入 IP/端口连接 Windows。
4. Android 可以把当前剪贴板文本发送到 Windows。
5. Windows 记录收到的内容，并可写入系统剪贴板。
6. Windows 自动监听剪贴板、Android 通知和持久化历史会在后续阶段完善。

### Windows 开发

```bash
cd apps/windows
npm install
npm run tauri dev
```

依赖：

- Node.js
- Rust
- Windows 上的 Tauri 系统依赖

### Windows 打包

```bash
cd apps/windows
npm run tauri build
```

常见产物：

```text
apps/windows/src-tauri/target/release/clipbridge.exe
apps/windows/src-tauri/target/release/bundle/
```

### Android 开发

用 Android Studio 打开 `apps/android`，或在配置好 Android SDK 和 Gradle 后运行：

```bash
cd apps/android
./gradlew assembleDebug
```

### 隐私定位

ClipBridge 不包含云同步、统计分析、账号系统或远程存储。剪贴板内容只应在用户主动配对的本地网络或虚拟私有网络设备之间传输。

---

## English

ClipBridge is a local-first clipboard bridge for Windows and Android.

The first version is designed around:

- Windows desktop app + Android app
- LAN/P2P communication only
- No cloud service
- No account system
- No self-hosted server requirement
- Text and link clipboard sync as the initial content type

### Repository layout

```text
apps/windows   Tauri + React Windows client
apps/android   Native Kotlin Android client
docs           Product and protocol notes
```

### MVP flow

1. Windows starts a local WebSocket endpoint on the LAN.
2. Windows displays a scannable pairing QR code and manual connection address.
3. Android connects by QR payload or manual host/port entry.
4. Android can send current clipboard text to Windows.
5. Windows records received content and can write it to the system clipboard.
6. Windows clipboard monitoring, Android notifications, and persistent history are planned for later slices.

### Windows development

```bash
cd apps/windows
npm install
npm run tauri dev
```

Requirements:

- Node.js
- Rust
- Tauri system prerequisites for Windows

### Windows packaging

```bash
cd apps/windows
npm run tauri build
```

Common outputs:

```text
apps/windows/src-tauri/target/release/clipbridge.exe
apps/windows/src-tauri/target/release/bundle/
```

### Android development

Open `apps/android` in Android Studio, or run Gradle from that directory once an Android SDK and Gradle are configured.

```bash
cd apps/android
./gradlew assembleDebug
```

### Privacy stance

ClipBridge does not include cloud sync, analytics, accounts, or remote storage. Clipboard content should only move between devices the user has paired on their own local or virtual private network.
