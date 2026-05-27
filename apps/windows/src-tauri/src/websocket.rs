use crate::{clipboard, history::HistoryStore};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use tauri::AppHandle;
use tokio::net::TcpListener;
use tokio::sync::mpsc;
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

pub type ClientRegistry = Arc<Mutex<Vec<mpsc::UnboundedSender<String>>>>;

pub fn create_registry() -> ClientRegistry {
    Arc::new(Mutex::new(Vec::new()))
}

pub fn broadcast_to_clients(registry: &ClientRegistry, message: &str) {
    let mut clients = registry.lock().expect("client registry lock poisoned");
    clients.retain(|sender| sender.send(message.to_string()).is_ok());
}

pub fn start_server(
    app: AppHandle,
    history: HistoryStore,
    registry: ClientRegistry,
    port: u16,
) {
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
            let registry = registry.clone();

            tauri::async_runtime::spawn(async move {
                let Ok(websocket) = tokio_tungstenite::accept_async(stream).await else {
                    return;
                };

                let (ws_sink, mut ws_source) = websocket.split();
                let (tx, mut rx) = mpsc::unbounded_channel::<String>();

                registry.lock().expect("client registry lock poisoned").push(tx.clone());

                let send_task = tokio::spawn(async move {
                    let mut sink = ws_sink;
                    while let Some(msg) = rx.recv().await {
                        if sink.send(Message::Text(msg.into())).await.is_err() {
                            break;
                        }
                    }
                });

                while let Some(message) = ws_source.next().await {
                    let Ok(Message::Text(text)) = message else {
                        continue;
                    };

                    match serde_json::from_str::<ProtocolMessage>(&text) {
                        Ok(protocol) if protocol.message_type == "clipboard.update" => {
                            if let Some(content) = protocol.content {
                                clipboard::set_last_content(&content);
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
                                    let _ = tx.send(payload);
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
                                let _ = tx.send(payload);
                            }
                        }
                        _ => {}
                    }
                }

                send_task.abort();
            });
        }
    });
}
