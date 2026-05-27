use crate::history::HistoryStore;
use crate::websocket::{broadcast_to_clients, ClientRegistry};
use std::sync::{LazyLock, Mutex};
use tauri::AppHandle;
use tauri_plugin_clipboard_manager::ClipboardExt;

static LAST_CONTENT: LazyLock<Mutex<String>> = LazyLock::new(|| Mutex::new(String::new()));

pub fn set_last_content(content: &str) {
    *LAST_CONTENT.lock().unwrap() = content.to_string();
}

pub fn write_text(app: &AppHandle, content: &str) -> Result<(), String> {
    app.clipboard()
        .write_text(content.to_string())
        .map_err(|error| error.to_string())
}

fn read_text(app: &AppHandle) -> Result<String, String> {
    app.clipboard()
        .read_text()
        .map_err(|error| error.to_string())
}

pub fn start_monitor(app: AppHandle, registry: ClientRegistry, history: HistoryStore) {
    tauri::async_runtime::spawn(async move {
        let mut interval = tokio::time::interval(tokio::time::Duration::from_millis(500));
        loop {
            interval.tick().await;
            let Ok(current) = read_text(&app) else {
                continue;
            };
            if current.is_empty() {
                continue;
            }
            let mut last = LAST_CONTENT.lock().unwrap();
            if current != *last {
                let content = current.clone();
                *last = current;
                drop(last);

                let envelope = serde_json::json!({
                    "version": 1,
                    "type": "clipboard.update",
                    "messageId": uuid::Uuid::new_v4().to_string(),
                    "fromDeviceId": "windows",
                    "contentType": "text",
                    "content": content,
                    "timestamp": chrono::Utc::now().timestamp_millis(),
                });

                if let Ok(payload) = serde_json::to_string(&envelope) {
                    broadcast_to_clients(&registry, &payload);
                    history.add_sent("android".to_string(), content);
                }
            }
        }
    });
}
