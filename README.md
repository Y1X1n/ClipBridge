# ClipBridge

ClipBridge is a local-first clipboard bridge for Windows and Android.

The first version is designed around:

- Windows desktop app + Android app
- LAN/P2P communication only
- No cloud service
- No account system
- No self-hosted server requirement
- Text and link clipboard sync as the initial content type

## Repository layout

```text
apps/windows   Tauri + React Windows client
apps/android   Native Kotlin Android client
docs           Product and protocol notes
```

## MVP flow

1. Windows starts a local WebSocket endpoint on the LAN.
2. Windows displays local pairing information.
3. Android connects by QR payload or manual host/port entry.
4. Android can send text to Windows.
5. Windows records received content and can write it to the system clipboard.
6. Windows clipboard monitoring and Android notifications are the next MVP slice.

## Windows development

```bash
cd apps/windows
npm install
npm run tauri dev
```

Requirements:

- Node.js
- Rust
- Tauri system prerequisites for Windows

## Android development

Open `apps/android` in Android Studio, or run Gradle from that directory once an Android SDK is configured.

```bash
cd apps/android
./gradlew assembleDebug
```

## Privacy stance

ClipBridge does not include cloud sync, analytics, accounts, or remote storage. Clipboard content should only move between devices the user has paired on their own local or virtual private network.
