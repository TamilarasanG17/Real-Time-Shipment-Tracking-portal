
import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:9090/ws';
const RECONNECT_DELAY_MS = 5000;


export function useShipmentTracking(shipmentId, token) {
  const [location, setLocation] = useState(null);
  const [connected, setConnected] = useState(false);
  const [error, setError]         = useState(null);
  const clientRef = useRef(null);

  const connect = useCallback(() => {
    if (!shipmentId || !token) return;

    if (clientRef.current?.active) clientRef.current.deactivate();

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders:   { Authorization: `Bearer ${token}` },
      reconnectDelay:    RECONNECT_DELAY_MS,

      onConnect: () => {
        setConnected(true);
        setError(null);
        client.subscribe(`/topic/shipment/${shipmentId}`, (msg) => {
          try {
            const broadcast = JSON.parse(msg.body);
            setLocation({
              latitude:  broadcast.latitude,
              longitude: broadcast.longitude,
              timestamp: broadcast.timestamp,
              status:    broadcast.status,
            });
          } catch (e) {
            console.error('[WS] parse error:', e);
          }
        });
      },

      onDisconnect:    () => setConnected(false),
      onStompError:    (f) => { setConnected(false); setError(f.headers?.message || 'STOMP error'); },
      onWebSocketError:() => { setConnected(false); setError('Network error — retrying…'); },
    });

    client.activate();
    clientRef.current = client;
  }, [shipmentId, token]);

  useEffect(() => {
    connect();
    return () => { if (clientRef.current?.active) clientRef.current.deactivate(); };
  }, [connect]);

  return { location, connected, error };
}