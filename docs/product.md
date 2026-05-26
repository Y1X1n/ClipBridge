# ClipBridge Product Notes

## Product definition

ClipBridge is a Windows + Android LAN/P2P clipboard sync tool. It avoids cloud infrastructure, account registration, and self-hosted servers. Devices connect directly on the same local network or through a user-managed virtual LAN such as Tailscale or ZeroTier.

## MVP scope

### Windows

- Tray-capable desktop client
- Local WebSocket endpoint
- Pairing information display
- Text clipboard receive/write support
- Text clipboard monitoring in the next slice
- Local in-memory history in the first scaffold

### Android

- Manual Windows host/port connection
- Send current clipboard text to Windows
- Receive text updates from Windows
- Android share-sheet entry for text/link handoff
- Local in-memory history in the first scaffold

## Out of scope for the first version

- Cloud sync
- Account login
- Public internet traversal
- Hosted relay servers
- Images and files
- iOS/macOS
- Team sharing
- Paid plans

## Default sync behavior

- Windows can automatically send copied text to paired Android devices once clipboard monitoring is enabled.
- Android sends manually because background clipboard access is restricted on recent Android versions.
- Received Android content can be written to the Windows clipboard.
- Received Windows content should be shown on Android and copied manually by the user.
