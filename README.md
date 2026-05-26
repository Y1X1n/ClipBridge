# ClipBridge

ClipBridge 是一个面向 Windows + Android 的本地优先剪贴板桥接工具。它通过局域网直连完成设备配对和文本传输，不依赖云端、账号系统或自备服务器。

ClipBridge is a local-first clipboard bridge for Windows + Android. It pairs devices and transfers text over the local network without cloud services, accounts, or a self-hosted server.

## 当前能力 / Current capabilities

- Windows 桌面端：Tauri 2 + React + TypeScript + Rust。
- Android 移动端：Kotlin 原生 Android 应用。
- Windows 在局域网内启动本地 WebSocket 服务，默认端口 `7890`。
- Windows 端展示可扫描二维码，也展示手动输入用的 IP / 端口。
- Android 支持扫码配对，也支持手动输入 Windows IP / 端口。
- Android 扫码后会自动校验信息通路：连接 Windows WebSocket，发送 `device.hello`，收到 `pairing.confirm` 后显示链路校验成功。
- Android 支持发送当前剪贴板文本到 Windows。
- Android 支持从系统分享面板接收文本 / 链接并发送到 Windows。
- Windows 收到 Android 文本后记录到传输历史，并写入 Windows 系统剪贴板。
- Windows / Android UI 均使用中英双语文案。
- Android 已允许局域网 `ws://` 明文 WebSocket，用于个人热点或同一 Wi-Fi 下的本地连接。

---

## 仓库结构 / Repository layout

```text
.
├── apps/
│   ├── windows/                 Tauri + React Windows client
│   │   ├── src/                 React UI and protocol types
│   │   └── src-tauri/           Rust backend, clipboard, pairing, WebSocket server
│   └── android/                 Native Kotlin Android client
│       └── app/src/main/        Android manifest, UI, QR scanner, WebSocket client
├── docs/                        Product and protocol notes
└── .github/workflows/build.yml  GitHub Actions packaging workflow
```

---

## 使用流程 / Usage flow

### 中文

1. 启动 Windows 端 `clipbridge.exe`。
2. 确保 Windows 和 Android 在同一个局域网内，例如同一 Wi-Fi 或手机热点。
3. Windows 端会显示二维码和局域网地址。
4. Android 端点击“扫描 Windows 二维码 / Scan Windows QR”。
5. 扫码后 Android 会自动连接并校验链路。
6. Android 显示“信息通路校验成功 / Link validated”后，即可发送剪贴板文本。
7. Windows 收到文本后会写入系统剪贴板，并在传输记录中显示。

### English

1. Start `clipbridge.exe` on Windows.
2. Make sure Windows and Android are on the same LAN, such as the same Wi-Fi or phone hotspot.
3. The Windows app displays a QR code and LAN address.
4. Tap “扫描 Windows 二维码 / Scan Windows QR” on Android.
5. Android connects and validates the link automatically after scanning.
6. Once Android shows “信息通路校验成功 / Link validated”, clipboard text can be sent.
7. Windows writes received text to the system clipboard and records it in the transfer log.

---

## 开发运行 / Development

### Windows

```bash
cd apps/windows
npm install
npm run tauri dev
```

Requirements:

- Node.js
- Rust
- Tauri Windows system prerequisites

### Android

Open `apps/android` in Android Studio, or run Gradle after configuring Android SDK / JDK:

```bash
cd apps/android
./gradlew assembleDebug
```

Android key dependencies:

- OkHttp WebSocket client
- ZXing Android Embedded QR scanner
- AndroidX Core

---

## 打包 / Packaging

### GitHub Actions

The repository includes `.github/workflows/build.yml`, which builds both apps on every push:

- Windows artifact: `clipbridge-windows-exe`
- Android artifact: `clipbridge-android-debug-apk`

### Local Windows EXE

```bash
cd apps/windows
npm run tauri build
```

Useful output paths:

```text
apps/windows/src-tauri/target/release/clipbridge.exe
apps/windows/src-tauri/target/release/bundle/
```

### Local Android APK

```bash
cd apps/android
./gradlew assembleDebug
```

Useful output path:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 局域网连接排查 / LAN troubleshooting

- Windows 和 Android 必须处于同一局域网。
- 如果使用手机热点，请确认热点没有开启客户端隔离。
- Windows 防火墙需要允许 ClipBridge 监听局域网端口 `7890`。
- Android 使用 `ws://` 局域网连接；项目已配置 cleartext LAN traffic。
- 如果扫码后一直停在校验状态，可以尝试在 Android 端手动输入 Windows 显示的 IP 和端口。
- 如果 Windows IP 变化，重新打开 Windows 端或刷新二维码后再扫码。

---

## 协议摘要 / Protocol summary

Pairing QR payload:

```json
{
  "app": "clipbridge",
  "version": 1,
  "host": "192.168.1.20",
  "port": 7890,
  "pairingCode": "384921",
  "deviceName": "Windows PC",
  "expiresAt": 1710000000000
}
```

Clipboard message envelope:

```json
{
  "version": 1,
  "type": "clipboard.update",
  "messageId": "msg_001",
  "fromDeviceId": "android_001",
  "toDeviceId": "windows_001",
  "contentType": "text",
  "content": "hello world",
  "timestamp": 1710000000000
}
```

See `docs/protocol.md` for more details.

---

## 隐私定位 / Privacy stance

ClipBridge 不包含云同步、统计分析、账号系统或远程存储。剪贴板内容只在用户主动配对的本地网络设备之间传输。

ClipBridge does not include cloud sync, analytics, accounts, or remote storage. Clipboard content only moves between devices the user has explicitly paired on their local network.
