use rand::Rng;
use serde::Serialize;

pub const DEFAULT_PORT: u16 = 7890;

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PairingInfo {
    pub app: String,
    pub version: u8,
    pub host: String,
    pub port: u16,
    pub pairing_code: String,
    pub device_name: String,
    pub expires_at: i64,
}

pub fn create_pairing_info() -> PairingInfo {
    let host = local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());
    let code = rand::thread_rng().gen_range(100_000..999_999).to_string();

    PairingInfo {
        app: "clipbridge".to_string(),
        version: 1,
        host,
        port: DEFAULT_PORT,
        pairing_code: code,
        device_name: hostname(),
        expires_at: chrono::Utc::now().timestamp_millis() + 5 * 60 * 1000,
    }
}

fn hostname() -> String {
    std::env::var("COMPUTERNAME")
        .or_else(|_| std::env::var("HOSTNAME"))
        .unwrap_or_else(|_| "Windows PC".to_string())
}
