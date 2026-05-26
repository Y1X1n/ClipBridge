export type ContentType = 'text';

export type ProtocolMessage = {
  version: 1;
  type:
    | 'pairing.request'
    | 'pairing.confirm'
    | 'pairing.reject'
    | 'device.hello'
    | 'device.ping'
    | 'clipboard.update'
    | 'clipboard.ack'
    | 'error';
  messageId: string;
  fromDeviceId: string;
  toDeviceId?: string;
  contentType?: ContentType;
  content?: string;
  timestamp: number;
};

export type PairingInfo = {
  app: 'clipbridge';
  version: 1;
  host: string;
  port: number;
  pairingCode: string;
  deviceName: string;
  expiresAt: number;
};

export type HistoryItem = {
  id: string;
  direction: 'sent' | 'received';
  sourceDevice: string;
  contentType: ContentType;
  content: string;
  timestamp: number;
};
