use tauri::AppHandle;
use tauri_plugin_clipboard_manager::ClipboardExt;

pub fn write_text(app: &AppHandle, content: &str) -> Result<(), String> {
    app.clipboard()
        .write_text(content.to_string())
        .map_err(|error| error.to_string())
}
