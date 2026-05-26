import { invoke } from '@tauri-apps/api/core';
import { useEffect, useState } from 'react';
import './styles.css';
import type { HistoryItem, PairingInfo } from './protocol';

function formatTime(timestamp: number) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(timestamp));
}

export default function App() {
  const [pairingInfo, setPairingInfo] = useState<PairingInfo | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [status, setStatus] = useState('Booting LAN node...');

  async function refreshHistory() {
    const items = await invoke<HistoryItem[]>('get_history');
    setHistory(items);
  }

  useEffect(() => {
    async function boot() {
      const info = await invoke<PairingInfo>('get_pairing_info');
      setPairingInfo(info);
      setStatus(`LAN socket open · ${info.host}:${info.port}`);
      await refreshHistory();
    }

    boot().catch((error) => {
      setStatus(String(error));
    });

    const timer = window.setInterval(refreshHistory, 1500);
    return () => window.clearInterval(timer);
  }, []);

  return (
    <main className="shell">
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Local link layer / no cloud</p>
          <h1>ClipBridge</h1>
          <p className="subtitle">A private clipboard relay for your Windows desk and Android pocket.</p>
        </div>
        <div className="signal-panel" aria-label="Local service status">
          <span className="pulse" />
          <div>
            <small>Node status</small>
            <strong>{status}</strong>
          </div>
        </div>
      </section>

      <section className="grid">
        <article className="card pairing-card">
          <div className="card-heading">
            <span>01</span>
            <h2>Pair Android</h2>
          </div>
          <div className="qr-frame">
            <div className="qr-placeholder" aria-label="Pairing QR placeholder">
              <span />
              <span />
              <span />
              <b>PAIR</b>
            </div>
          </div>
          {pairingInfo ? (
            <div className="pairing-details">
              <p><span>LAN address</span><code>{pairingInfo.host}:{pairingInfo.port}</code></p>
              <p><span>Pairing code</span><code>{pairingInfo.pairingCode}</code></p>
              <p><span>Expires</span><code>{formatTime(pairingInfo.expiresAt)}</code></p>
            </div>
          ) : (
            <p className="empty">Preparing pairing payload...</p>
          )}
        </article>

        <article className="card controls-card">
          <div className="card-heading">
            <span>02</span>
            <h2>Relay rules</h2>
          </div>
          <div className="toggle-row"><span>Windows clipboard watch</span><strong>Next slice</strong></div>
          <div className="toggle-row"><span>Android inbound lane</span><strong>Open</strong></div>
          <div className="toggle-row"><span>Write inbound text</span><strong>Armed</strong></div>
          <div className="route-map" aria-hidden="true">
            <i>WIN</i>
            <em />
            <i>LAN</i>
            <em />
            <i>DROID</i>
          </div>
        </article>
      </section>

      <section className="card history-card">
        <div className="section-title">
          <div className="card-heading">
            <span>03</span>
            <h2>Transfer log</h2>
          </div>
          <button onClick={refreshHistory}>Poll now</button>
        </div>
        {history.length === 0 ? (
          <p className="empty">No packets yet. Connect Android, then send a text payload across the LAN.</p>
        ) : (
          <ul className="history-list">
            {history.map((item) => (
              <li key={item.id}>
                <div>
                  <strong>{item.direction === 'received' ? 'Inbound from' : 'Outbound to'} {item.sourceDevice}</strong>
                  <time>{formatTime(item.timestamp)}</time>
                </div>
                <p>{item.content}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
