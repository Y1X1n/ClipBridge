import { invoke } from '@tauri-apps/api/core';
import { useEffect, useState } from 'react';
import './styles.css';
import type { HistoryItem, PairingInfo } from './protocol';

type Language = 'zh' | 'en';

type Copy = {
  booting: string;
  socketOpen: (address: string) => string;
  eyebrow: string;
  subtitle: string;
  statusAria: string;
  nodeStatus: string;
  pairAndroid: string;
  qrLabel: string;
  qrText: string;
  lanAddress: string;
  pairingCode: string;
  expires: string;
  preparing: string;
  relayRules: string;
  clipboardWatch: string;
  nextSlice: string;
  androidInbound: string;
  open: string;
  writeInbound: string;
  armed: string;
  win: string;
  lan: string;
  droid: string;
  transferLog: string;
  pollNow: string;
  emptyHistory: string;
  inboundFrom: string;
  outboundTo: string;
  languageToggle: string;
};

const copy: Record<Language, Copy> = {
  zh: {
    booting: '正在启动局域网节点...',
    socketOpen: (address) => `局域网通道已开启 · ${address}`,
    eyebrow: '本地链路 / 无云端',
    subtitle: '把 Windows 桌面和 Android 手机变成一条私有剪贴板通道。',
    statusAria: '本地服务状态',
    nodeStatus: '节点状态',
    pairAndroid: '配对 Android',
    qrLabel: '配对二维码占位区',
    qrText: '配对',
    lanAddress: '局域网地址',
    pairingCode: '配对码',
    expires: '有效期至',
    preparing: '正在准备配对信息...',
    relayRules: '同步规则',
    clipboardWatch: '监听 Windows 剪贴板',
    nextSlice: '下一阶段',
    androidInbound: 'Android 接收通道',
    open: '开启',
    writeInbound: '收到内容写入剪贴板',
    armed: '已启用',
    win: '电脑',
    lan: '局域网',
    droid: '手机',
    transferLog: '传输记录',
    pollNow: '刷新',
    emptyHistory: '暂无传输记录。连接 Android 后，发送一段文本到这台 Windows。',
    inboundFrom: '来自',
    outboundTo: '发送到',
    languageToggle: 'EN',
  },
  en: {
    booting: 'Booting LAN node...',
    socketOpen: (address) => `LAN socket open · ${address}`,
    eyebrow: 'Local link layer / no cloud',
    subtitle: 'A private clipboard relay for your Windows desk and Android pocket.',
    statusAria: 'Local service status',
    nodeStatus: 'Node status',
    pairAndroid: 'Pair Android',
    qrLabel: 'Pairing QR placeholder',
    qrText: 'PAIR',
    lanAddress: 'LAN address',
    pairingCode: 'Pairing code',
    expires: 'Expires',
    preparing: 'Preparing pairing payload...',
    relayRules: 'Relay rules',
    clipboardWatch: 'Windows clipboard watch',
    nextSlice: 'Next slice',
    androidInbound: 'Android inbound lane',
    open: 'Open',
    writeInbound: 'Write inbound text',
    armed: 'Armed',
    win: 'WIN',
    lan: 'LAN',
    droid: 'DROID',
    transferLog: 'Transfer log',
    pollNow: 'Poll now',
    emptyHistory: 'No packets yet. Connect Android, then send a text payload across the LAN.',
    inboundFrom: 'Inbound from',
    outboundTo: 'Outbound to',
    languageToggle: '中',
  },
};

function formatTime(timestamp: number) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(timestamp));
}

export default function App() {
  const [language, setLanguage] = useState<Language>(() => {
    const saved = window.localStorage.getItem('clipbridge-language');
    return saved === 'en' ? 'en' : 'zh';
  });
  const t = copy[language];
  const [pairingInfo, setPairingInfo] = useState<PairingInfo | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [status, setStatus] = useState(t.booting);

  async function refreshHistory() {
    const items = await invoke<HistoryItem[]>('get_history');
    setHistory(items);
  }

  function toggleLanguage() {
    setLanguage((current) => {
      const next = current === 'zh' ? 'en' : 'zh';
      window.localStorage.setItem('clipbridge-language', next);
      return next;
    });
  }

  useEffect(() => {
    if (pairingInfo) {
      setStatus(t.socketOpen(`${pairingInfo.host}:${pairingInfo.port}`));
    } else {
      setStatus(t.booting);
    }
  }, [language, pairingInfo, t]);

  useEffect(() => {
    async function boot() {
      const info = await invoke<PairingInfo>('get_pairing_info');
      setPairingInfo(info);
      await refreshHistory();
    }

    boot().catch((error) => {
      setStatus(String(error));
    });

    const timer = window.setInterval(refreshHistory, 1500);
    return () => window.clearInterval(timer);
  }, []);

  return (
    <main className="shell" lang={language === 'zh' ? 'zh-CN' : 'en'}>
      <button className="language-switch" onClick={toggleLanguage} aria-label="Switch language">
        {t.languageToggle}
      </button>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">{t.eyebrow}</p>
          <h1>ClipBridge</h1>
          <p className="subtitle">{t.subtitle}</p>
        </div>
        <div className="signal-panel" aria-label={t.statusAria}>
          <span className="pulse" />
          <div>
            <small>{t.nodeStatus}</small>
            <strong>{status}</strong>
          </div>
        </div>
      </section>

      <section className="grid">
        <article className="card pairing-card">
          <div className="card-heading">
            <span>01</span>
            <h2>{t.pairAndroid}</h2>
          </div>
          <div className="qr-frame">
            <div className="qr-placeholder" aria-label={t.qrLabel}>
              <span />
              <span />
              <span />
              <b>{t.qrText}</b>
            </div>
          </div>
          {pairingInfo ? (
            <div className="pairing-details">
              <p><span>{t.lanAddress}</span><code>{pairingInfo.host}:{pairingInfo.port}</code></p>
              <p><span>{t.pairingCode}</span><code>{pairingInfo.pairingCode}</code></p>
              <p><span>{t.expires}</span><code>{formatTime(pairingInfo.expiresAt)}</code></p>
            </div>
          ) : (
            <p className="empty">{t.preparing}</p>
          )}
        </article>

        <article className="card controls-card">
          <div className="card-heading">
            <span>02</span>
            <h2>{t.relayRules}</h2>
          </div>
          <div className="toggle-row"><span>{t.clipboardWatch}</span><strong>{t.nextSlice}</strong></div>
          <div className="toggle-row"><span>{t.androidInbound}</span><strong>{t.open}</strong></div>
          <div className="toggle-row"><span>{t.writeInbound}</span><strong>{t.armed}</strong></div>
          <div className="route-map" aria-hidden="true">
            <i>{t.win}</i>
            <em />
            <i>{t.lan}</i>
            <em />
            <i>{t.droid}</i>
          </div>
        </article>
      </section>

      <section className="card history-card">
        <div className="section-title">
          <div className="card-heading">
            <span>03</span>
            <h2>{t.transferLog}</h2>
          </div>
          <button onClick={refreshHistory}>{t.pollNow}</button>
        </div>
        {history.length === 0 ? (
          <p className="empty">{t.emptyHistory}</p>
        ) : (
          <ul className="history-list">
            {history.map((item) => (
              <li key={item.id}>
                <div>
                  <strong>{item.direction === 'received' ? t.inboundFrom : t.outboundTo} {item.sourceDevice}</strong>
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
