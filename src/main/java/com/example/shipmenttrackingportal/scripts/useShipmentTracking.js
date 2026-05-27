
import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:9090/ws';
const RECONNECT_DELAY_MS = 5000;

export function useShipmentTracking(shipmentId, token) {
    const [location, setLocation] = useState(null);
    const [connected, setConnected] = useState(false);
    const [error, setError] = useState(null);
    const clientRef = useRef(null);

    const connect = useCallback(() => {
        if (!shipmentId || !token) return;

    
        if (clientRef.current?.active) {
            clientRef.current.deactivate();
        }

        const client = new Client({
            // SockJS factory — provides WebSocket with HTTP fallback
            webSocketFactory: () => new SockJS(WS_URL),

            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },

            reconnectDelay: RECONNECT_DELAY_MS,

            onConnect: () => {
                setConnected(true);
                setError(null);
                console.log(`[WS] Connected. Subscribing to /topic/shipment/${shipmentId}`);

                client.subscribe(`/topic/shipment/${shipmentId}`, (message) => {
                    try {
                        const broadcast = JSON.parse(message.body);
                        console.log('[WS] Received:', broadcast);

                        // Update location state → triggers map marker re-render
                        setLocation({
                            latitude: broadcast.latitude,
                            longitude: broadcast.longitude,
                            timestamp: broadcast.timestamp,
                            status: broadcast.status,
                        });
                    } catch (e) {
                        console.error('[WS] Failed to parse message:', e);
                    }
                });
            },

            onDisconnect: () => {
                setConnected(false);
                console.log('[WS] Disconnected.');
            },

            onStompError: (frame) => {
                setConnected(false);
                setError(`WebSocket error: ${frame.headers?.message || 'Unknown error'}`);
                console.error('[WS] STOMP error:', frame);
            },

            onWebSocketError: (event) => {
                setConnected(false);
                setError('Network error — retrying...');
                console.error('[WS] WebSocket error:', event);
            },
        });

        client.activate();
        clientRef.current = client;
    }, [shipmentId, token]);

    useEffect(() => {
        connect();

        // Cleanup: deactivate on unmount or when shipmentId/token changes
        return () => {
            if (clientRef.current?.active) {
                clientRef.current.deactivate();
                console.log('[WS] Cleaned up connection.');
            }
        };
    }, [connect]);

    return { location, connected, error };
}
