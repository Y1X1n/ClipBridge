# ClipBridge Protocol

Protocol version: `1`

ClipBridge uses JSON messages over a LAN WebSocket connection for the first MVP slice. Windows acts as the server and Android acts as the client.

## Envelope

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

## Message types

| Type | Purpose |
| --- | --- |
| `pairing.request` | Android asks Windows to pair. |
| `pairing.confirm` | Windows confirms pairing. |
| `pairing.reject` | Windows rejects pairing. |
| `device.hello` | Connected device announces itself. |
| `device.ping` | Keepalive. |
| `clipboard.update` | Clipboard text/link payload. |
| `clipboard.ack` | Receiver confirms clipboard payload. |
| `error` | Protocol or validation failure. |

## Pairing payload

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

## Security roadmap

The scaffold starts with short-lived pairing codes and local paired-device state. The next security milestone should add authenticated messages and encrypted payload transport using a device-specific shared key.
