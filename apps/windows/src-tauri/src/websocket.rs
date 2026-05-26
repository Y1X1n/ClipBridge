use crate::{clipboard, history::HistoryStore};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use tauri::AppHandle;
use tokio::net::TcpListener;
use tokio_tungstenite::tungstenite::Message;

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
struct ProtocolMessage {
    version: u8,
    #[serde(rename = "type")]
    message_type: String,
    message_id: String,
    from_device_id: String,
    to_device_id: Option<String>,
    content_type: Option<String>,
    content: Option<String>,
    timestamp: i64,
}

pub fn start_server(app: AppHandle, history: HistoryStore, port: u16) {
    tauri::async_runtime::spawn(async move {
        let address = format!("0.0.0.0:{port}");
        let listener = match TcpListener::bind(&address).await {
            Ok(listener) => listener,
            Err(error) => {
                eprintln!("failed to bind websocket server on {address}: {error}");
                return;
            }
        };

        loop {
            let Ok((stream, _)) = listener.accept().await else {
                continue;
            };
            let app = app.clone();
            let history = history.clone();

            tauri::async_runtime::spawn(async move {
                let Ok(mut websocket) = tokio_tungstenite::accept_async(stream).await else {
                    return;
                };

                while let Some(message) = websocket.next().await {
                    let Ok(Message::Text(text)) = message else {
                        continue;
                    };

                    match serde_json::from_str::<ProtocolMessage>(&text) {
                        Ok(protocol) if protocol.message_type == "clipboard.update" => {
                            if let Some(content) = protocol.content {
                                history.add_received(protocol.from_device_id.clone(), content.clone());
                                let _ = clipboard::write_text(&app, &content);

                                let ack = ProtocolMessage {
                                    version: 1,
                                    message_type: "clipboard.ack".to_string(),
                                    message_id: uuid::Uuid::new_v4().to_string(),
                                    from_device_id: "windows".to_string(),
                                    to_device_id: Some(protocol.from_device_id),
                                    content_type: Some("text".to_string()),
                                    content: None,
                                    timestamp: chrono::Utc::now().timestamp_millis(),
                                };

                                if let Ok(payload) = serde_json::to_string(&ack) {
                                    let _ = websocket.send(Message::Text(payload.into())).await;
                                }
                            }
                        }
                        Ok(protocol) if protocol.message_type == "device.hello" => {
                            let ack = ProtocolMessage {
                                version: 1,
                                message_type: "pairing.confirm".to_string(),
                                message_id: uuid::Uuid::new_v4().to_string(),
                                from_device_id: "windows".to_string(),
                                to_device_id: Some(protocol.from_device_id),
                                content_type: None,
                                content: None,
                                timestamp: chrono::Utc::now().timestamp_millis(),
                            };

                            if let Ok(payload) = serde_json::to_string(&ack) {
                                let _ = websocket.send(Message::Text(payload.into())).await;
                            }
                        }
                        _ => {}
                    }
                }
            });
        }
    });
}
