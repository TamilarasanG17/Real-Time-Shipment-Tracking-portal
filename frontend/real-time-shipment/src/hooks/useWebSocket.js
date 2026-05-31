import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:9090/ws';

export function useWebSocket() {
  const clientRef = useRef(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,           // Auto-reconnect after 5s if dropped
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('[WS] Connected to STOMP broker');
        setIsConnected(true);
      },

      onDisconnect: () => {
        console.log('[WS] Disconnected from STOMP broker');
        setIsConnected(false);
      },

      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  /**
   * Subscribe to a STOMP topic. Returns an unsubscribe function.
   */
  const subscribe = useCallback((topic, callback) => {
    const client = clientRef.current;
    if (!client || !client.connected) {
      console.warn('[WS] Cannot subscribe - not connected yet');
      return () => {};
    }

    const subscription = client.subscribe(topic, callback);
    return () => subscription.unsubscribe();
  }, []);

  /**
   * Send a GPS update from the simulated driver app.
   * Destination: /app/tracking/{shipmentId}
   */
  const sendGpsUpdate = useCallback((shipmentId, lat, lng) => {
    const client = clientRef.current;
    if (!client?.connected) return;

    client.publish({
      destination: `/app/tracking/${shipmentId}`,
      body: JSON.stringify({ latitude: lat, longitude: lng, shipmentId }),
    });
  }, []);

  return { isConnected, subscribe, sendGpsUpdate, client: clientRef.current };
}