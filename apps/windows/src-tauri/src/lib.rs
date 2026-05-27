mod clipboard;
mod history;
mod pairing;
mod websocket;

use history::{HistoryItem, HistoryStore};
use pairing::PairingInfo;

#[tauri::command]
fn get_pairing_info(state: tauri::State<'_, AppState>) -> PairingInfo {
    state.pairing_info.clone()
}

#[tauri::command]
fn get_history(state: tauri::State<'_, AppState>) -> Vec<HistoryItem> {
    state.history.list()
}

struct AppState {
    pairing_info: PairingInfo,
    history: HistoryStore,
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let pairing_info = pairing::create_pairing_info();
    let history = HistoryStore::default();
    let registry = websocket::create_registry();

    tauri::Builder::default()
        .plugin(tauri_plugin_clipboard_manager::init())
        .manage(AppState {
            pairing_info: pairing_info.clone(),
            history: history.clone(),
        })
        .setup(move |app| {
            websocket::start_server(
                app.handle().clone(),
                history.clone(),
                registry.clone(),
                pairing_info.port,
            );
            clipboard::start_monitor(app.handle().clone(), registry.clone(), history.clone());
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![get_pairing_info, get_history])
        .run(tauri::generate_context!())
        .expect("error while running ClipBridge");
}
