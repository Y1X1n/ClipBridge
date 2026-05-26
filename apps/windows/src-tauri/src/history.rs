use serde::Serialize;
use std::sync::{Arc, Mutex};

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryItem {
    pub id: String,
    pub direction: String,
    pub source_device: String,
    pub content_type: String,
    pub content: String,
    pub timestamp: i64,
}

#[derive(Clone, Default)]
pub struct HistoryStore {
    items: Arc<Mutex<Vec<HistoryItem>>>,
}

impl HistoryStore {
    pub fn add_received(&self, source_device: String, content: String) {
        let mut items = self.items.lock().expect("history lock poisoned");
        items.insert(
            0,
            HistoryItem {
                id: uuid::Uuid::new_v4().to_string(),
                direction: "received".to_string(),
                source_device,
                content_type: "text".to_string(),
                content,
                timestamp: chrono::Utc::now().timestamp_millis(),
            },
        );
        items.truncate(100);
    }

    pub fn list(&self) -> Vec<HistoryItem> {
        self.items.lock().expect("history lock poisoned").clone()
    }
}
