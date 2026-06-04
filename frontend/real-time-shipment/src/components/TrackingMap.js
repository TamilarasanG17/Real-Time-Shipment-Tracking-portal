
import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useShipmentTracking } from '../hooks/useShipmentTracking';
import { getPingHistory }      from '../api/api';

// Fix Leaflet default icon broken by Webpack
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:       'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:     'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom pulsing marker icon for the live position
const liveIcon = new L.DivIcon({
  html: `<div style="
    width:18px; height:18px; background:#3182ce; border-radius:50%;
    border:3px solid #fff; box-shadow:0 0 0 3px rgba(49,130,206,0.4);
    animation:pulse 1.5s infinite;">
  </div>
  <style>@keyframes pulse {
    0%   { box-shadow:0 0 0 3px rgba(49,130,206,0.4); }
    100% { box-shadow:0 0 0 12px rgba(49,130,206,0); }
  }</style>`,
  className: '',
  iconSize:   [18, 18],
  iconAnchor: [9, 9],
});

/** Sub-component: re-centres the map whenever the position changes */
function RecenterMap({ lat, lng }) {
  const map = useMap();
  useEffect(() => {
    if (lat && lng) map.setView([lat, lng], map.getZoom(), { animate: true });
  }, [lat, lng, map]);
  return null;
}

export default function TrackingMap({ shipmentId, token }) {
  const [history,  setHistory]  = useState([]);  // [[lat,lng], ...]
  const [initPos,  setInitPos]  = useState(null); // { lat, lng } from latest REST ping

  const { location, connected, error } = useShipmentTracking(shipmentId, token);

  useEffect(() => {
    getPingHistory(shipmentId)
      .then(({ data }) => {
        const coords = data.map(p => [p.latitude, p.longitude]);
        setHistory(coords);
        if (coords.length > 0) setInitPos({ lat: coords[0][0], lng: coords[0][1] });
      })
      .catch(() => {});
  }, [shipmentId]);

  // Append new live ping to breadcrumb trail
  useEffect(() => {
    if (location?.latitude && location?.longitude) {
      setHistory(prev => [...prev, [location.latitude, location.longitude]]);
    }
  }, [location]);

  const livePos  = location?.latitude ? { lat: location.latitude, lng: location.longitude } : initPos;
  const center   = livePos ? [livePos.lat, livePos.lng] : [20.5937, 78.9629]; // India centre fallback

  return (
    <div>
      {/* Status bar */}
      <div style={styles.statusBar}>
        <span style={{...styles.dot, background: connected ? '#38a169' : '#e53e3e'}} />
        <span>{connected ? 'Live — receiving GPS updates' : 'Connecting to WebSocket…'}</span>
        {error && <span style={styles.errorText}> · {error}</span>}
        {location?.status && <span style={styles.statusChip}>{location.status}</span>}
        {location?.timestamp && (
          <span style={styles.timestamp}>Last ping: {new Date(location.timestamp).toLocaleTimeString()}</span>
        )}
      </div>

      {/* Leaflet Map */}
      <MapContainer center={center} zoom={6} style={styles.map}>
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://openstreetmap.org">OpenStreetMap</a>'
        />

        {/* Recenter map on live position */}
        {livePos && <RecenterMap lat={livePos.lat} lng={livePos.lng} />}

        {/* Live position marker */}
        {livePos && (
          <Marker position={[livePos.lat, livePos.lng]} icon={liveIcon}>
            <Popup>
              <strong>Shipment #{shipmentId}</strong><br />
              Status: {location?.status || 'IN_TRANSIT'}<br />
              lat: {livePos.lat.toFixed(5)}<br />
              lng: {livePos.lng.toFixed(5)}
            </Popup>
          </Marker>
        )}

        {/* Breadcrumb trail */}
        {history.length > 1 && (
          <Polyline positions={history} color="#3182ce" weight={3} opacity={0.7} />
        )}
      </MapContainer>

      {/* Ping counter */}
      <div style={styles.footer}>
        📍 {history.length} GPS ping{history.length !== 1 ? 's' : ''} recorded
      </div>
    </div>
  );
}

const styles = {
  statusBar: { display:'flex', alignItems:'center', gap:'0.5rem', padding:'0.5rem 0.75rem', background:'#f7fafc', borderRadius:'6px', marginBottom:'0.75rem', fontSize:'0.85rem', flexWrap:'wrap' },
  dot:       { width:10, height:10, borderRadius:'50%', flexShrink:0 },
  errorText: { color:'#c53030' },
  statusChip:{ background:'#3182ce', color:'#fff', padding:'1px 8px', borderRadius:'10px', fontSize:'0.8rem', fontWeight:600 },
  timestamp: { color:'#718096', marginLeft:'auto' },
  map:       { height:'420px', width:'100%', borderRadius:'8px', zIndex:0 },
  footer:    { marginTop:'0.5rem', color:'#718096', fontSize:'0.8rem', textAlign:'right' },
};