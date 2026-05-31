// src/pages/TrackingPage.js
import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { shipmentsApi, trackingApi } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';

// Fix default Leaflet marker icons in React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:       'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:     'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom truck icon
const truckIcon = L.divIcon({
  className: '',
  html: '<div style="font-size: 28px; filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.5))">🚚</div>',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

// Auto-pan map to new position
function MapPanner({ position }) {
  const map = useMap();
  useEffect(() => {
    if (position) map.panTo(position, { animate: true, duration: 1 });
  }, [position, map]);
  return null;
}

export default function TrackingPage() {
  const { shipmentId } = useParams();
  const navigate = useNavigate();
  const { isConnected, subscribe, sendGpsUpdate } = useWebSocket();

  const [shipment, setShipment] = useState(null);
  const [position, setPosition]   = useState(null);  // [lat, lng]
  const [history, setHistory]     = useState([]);     // Array of [lat, lng] for polyline trail
  const [updates, setUpdates]     = useState([]);     // Log of events
  const [simulating, setSimulating] = useState(false);
  const simInterval = useRef(null);

  // India-centric default: Bengaluru
  const DEFAULT_CENTER = [12.9716, 77.5946];
  const DEFAULT_ZOOM   = 6;

  // ── Load shipment data ─────────────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await shipmentsApi.getById(shipmentId);
        setShipment(data);

        // Try to get last known GPS position
        try {
          const { data: pos } = await trackingApi.getLastPosition(shipmentId);
          if (pos.latitude && pos.longitude) {
            const coord = [pos.latitude, pos.longitude];
            setPosition(coord);
            setHistory([coord]);
          }
        } catch { /* No position yet */ }
      } catch {
        navigate(-1);
      }
    };
    load();
  }, [shipmentId, navigate]);

  // ── WebSocket subscription ─────────────────────────────────────────────────
  useEffect(() => {
    if (!isConnected) return;

    const topic = `/topic/tracking/${shipmentId}`;
    const unsub = subscribe(topic, (msg) => {
      const payload = JSON.parse(msg.body);
      const coord   = [payload.latitude, payload.longitude];

      setPosition(coord);
      setHistory((prev) => [...prev, coord]);
      setUpdates((prev) => [
        {
          id:   Date.now(),
          time: new Date().toLocaleTimeString(),
          lat:  payload.latitude.toFixed(5),
          lng:  payload.longitude.toFixed(5),
          status: payload.status,
        },
        ...prev.slice(0, 19), // Keep last 20 updates
      ]);
    });

    return unsub;
  }, [isConnected, shipmentId, subscribe]);

  // ── GPS simulation (carrier side demo) ────────────────────────────────────
  const startSimulation = useCallback(() => {
    if (simulating) {
      clearInterval(simInterval.current);
      setSimulating(false);
      return;
    }

    // Simulate moving from Bengaluru to Chennai
    let lat = 12.9716;
    let lng = 77.5946;
    const dlat = (13.0827 - 12.9716) / 30;
    const dlng = (80.2707 - 77.5946) / 30;
    let step = 0;

    setSimulating(true);
    simInterval.current = setInterval(() => {
      if (step >= 30) {
        clearInterval(simInterval.current);
        setSimulating(false);
        return;
      }
      lat += dlat + (Math.random() - 0.5) * 0.005;
      lng += dlng + (Math.random() - 0.5) * 0.005;
      sendGpsUpdate(Number(shipmentId), lat, lng);
      step++;
    }, 2000); // Update every 2 seconds
  }, [simulating, shipmentId, sendGpsUpdate]);

  useEffect(() => () => clearInterval(simInterval.current), []);

  if (!shipment) {
    return (
      <div className="tracking-loading">
        <div className="spinner">🚚</div>
        <p>Loading shipment data...</p>
      </div>
    );
  }

  return (
    <div className="tracking-page">
      {/* Header */}
      <div className="tracking-header">
        <button className="btn-back" onClick={() => navigate(-1)}>← Back</button>
        <div className="tracking-title">
          <h2>Live Tracking · #{shipment.id}</h2>
          <span>{shipment.origin} → {shipment.destination}</span>
        </div>
        <div className="ws-status">
          <span className={`ws-dot ${isConnected ? 'connected' : 'disconnected'}`}></span>
          {isConnected ? 'Live' : 'Reconnecting...'}
        </div>
      </div>

      <div className="tracking-body">
        {/* Map */}
        <div className="map-container">
          <MapContainer
            center={position || DEFAULT_CENTER}
            zoom={position ? 9 : DEFAULT_ZOOM}
            style={{ height: '100%', width: '100%', borderRadius: '12px' }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            />

            {position && (
              <>
                <MapPanner position={position} />
                <Marker position={position} icon={truckIcon}>
                  <Popup>
                    <strong>{shipment.awardedCarrierName || 'Carrier'}</strong>
                    <br />
                    {position[0].toFixed(4)}, {position[1].toFixed(4)}
                  </Popup>
                </Marker>
                {history.length > 1 && (
                  <Polyline
                    positions={history}
                    color="#3b82f6"
                    weight={3}
                    opacity={0.7}
                    dashArray="6 4"
                  />
                )}
              </>
            )}

            {!position && (
              <div style={{
                position: 'absolute', top: '50%', left: '50%',
                transform: 'translate(-50%,-50%)', zIndex: 1000,
                background: 'rgba(0,0,0,0.7)', color: 'white',
                padding: '12px 20px', borderRadius: '8px',
              }}>
                Waiting for GPS signal...
              </div>
            )}
          </MapContainer>
        </div>

        {/* Sidebar panel */}
        <div className="tracking-sidebar">
          {/* Shipment info */}
          <div className="tracking-info-card">
            <h3>Shipment Details</h3>
            <div className="info-row"><span>Route</span>
              <strong>{shipment.origin} → {shipment.destination}</strong></div>
            <div className="info-row"><span>Weight</span>
              <strong>{shipment.weightKg} kg</strong></div>
            <div className="info-row"><span>Status</span>
              <strong className={`status-badge status-${shipment.status?.toLowerCase()}`}>
                {shipment.status}
              </strong>
            </div>
            {shipment.awardedCarrierName && (
              <div className="info-row"><span>Carrier</span>
                <strong>🚛 {shipment.awardedCarrierName}</strong></div>
            )}
            {position && (
              <div className="info-row"><span>Last GPS</span>
                <strong>{position[0].toFixed(5)}, {position[1].toFixed(5)}</strong></div>
            )}
          </div>

          {/* Simulate GPS button (carrier demo) */}
          <button
            className={`btn-simulate ${simulating ? 'simulating' : ''}`}
            onClick={startSimulation}
            disabled={!isConnected}
          >
            {simulating ? '⏹ Stop Simulation' : '▶ Simulate GPS Updates'}
          </button>
          {!isConnected && (
            <p className="ws-hint">Connecting to tracking server...</p>
          )}

          {/* Live update log */}
          <div className="update-log">
            <h4>Live Updates</h4>
            {updates.length === 0 ? (
              <p className="update-empty">No updates yet. Waiting for carrier...</p>
            ) : (
              updates.map((u) => (
                <div key={u.id} className="update-item">
                  <span className="update-time">{u.time}</span>
                  <span className="update-coords">{u.lat}, {u.lng}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}