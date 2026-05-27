
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyShipments, postShipment, getBidsForShipment, awardBid } from '../api/api';
import TrackingMap from '../components/TrackingMap';

export default function ShipperDashboard() {
  const navigate   = useNavigate();
  const token      = localStorage.getItem('token');
  const email      = localStorage.getItem('email');

  const [shipments,       setShipments]       = useState([]);
  const [bids,            setBids]            = useState({});      // { shipmentId: [BidResponse] }
  const [trackingId,      setTrackingId]      = useState(null);    // shipmentId to show on map
  const [loading,         setLoading]         = useState(false);
  const [postForm,        setPostForm]        = useState({ origin:'', destination:'', weightKg:'', description:'' });
  const [postError,       setPostError]       = useState('');
  const [expandedId,      setExpandedId]      = useState(null);

  useEffect(() => {
    if (!token) { navigate('/login'); return; }
    fetchShipments();
  }, [token]);

  const fetchShipments = async () => {
    try {
      const { data } = await getMyShipments();
      setShipments(data);
    } catch { navigate('/login'); }
  };

  const handlePost = async (e) => {
    e.preventDefault();
    setPostError(''); setLoading(true);
    try {
      await postShipment({ ...postForm, weightKg: parseFloat(postForm.weightKg) });
      setPostForm({ origin:'', destination:'', weightKg:'', description:'' });
      fetchShipments();
    } catch (err) {
      const d = err.response?.data;
      setPostError(d?.details ? Object.values(d.details).join(' | ') : d?.message || 'Error posting shipment');
    } finally { setLoading(false); }
  };

  const loadBids = async (shipmentId) => {
    if (expandedId === shipmentId) { setExpandedId(null); return; }
    setExpandedId(shipmentId);
    try {
      const { data } = await getBidsForShipment(shipmentId);
      setBids(prev => ({ ...prev, [shipmentId]: data }));
    } catch (err) {
      setBids(prev => ({ ...prev, [shipmentId]: [] }));
    }
  };

  const handleAward = async (shipmentId, bidId) => {
    if (!window.confirm('Award this bid? All other bids will be rejected.')) return;
    try {
      await awardBid(shipmentId, bidId);
      fetchShipments();
      const { data } = await getBidsForShipment(shipmentId);
      setBids(prev => ({ ...prev, [shipmentId]: data }));
    } catch (err) {
      alert(err.response?.data?.message || 'Award failed');
    }
  };

  const logout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const statusColor = { OPEN:'#38a169', AWAITING_PICKUP:'#d69e2e', IN_TRANSIT:'#3182ce', DELIVERED:'#6b46c1', CANCELLED:'#e53e3e' };

  return (
    <div style={styles.page}>
      {/* Header */}
      <div style={styles.header}>
        <span>🚚 Shipper Dashboard — <strong>{email}</strong></span>
        <button style={styles.logoutBtn} onClick={logout}>Logout</button>
      </div>

      <div style={styles.body}>
        {/* Post Shipment Form */}
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>📦 Post New Freight Load</h3>
          {postError && <p style={styles.error}>{postError}</p>}
          <form onSubmit={handlePost} style={styles.form}>
            <div style={styles.row}>
              <input style={styles.input} placeholder="Origin city" required
                value={postForm.origin} onChange={e => setPostForm({...postForm, origin: e.target.value})} />
              <input style={styles.input} placeholder="Destination city" required
                value={postForm.destination} onChange={e => setPostForm({...postForm, destination: e.target.value})} />
            </div>
            <div style={styles.row}>
              <input style={styles.input} type="number" step="0.01" placeholder="Weight (kg)" required
                value={postForm.weightKg} onChange={e => setPostForm({...postForm, weightKg: e.target.value})} />
              <input style={styles.input} placeholder="Description (optional)"
                value={postForm.description} onChange={e => setPostForm({...postForm, description: e.target.value})} />
            </div>
            <button style={styles.btn} type="submit" disabled={loading}>
              {loading ? 'Posting…' : 'Post Load'}
            </button>
          </form>
        </div>

        {/* Shipment List */}
        <div style={styles.card}>
          <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem'}}>
            <h3 style={styles.cardTitle}>📋 My Shipments ({shipments.length})</h3>
            <button style={styles.refreshBtn} onClick={fetchShipments}>↻ Refresh</button>
          </div>

          {shipments.length === 0 && <p style={styles.empty}>No shipments yet. Post your first load above.</p>}

          {shipments.map(s => (
            <div key={s.id} style={styles.shipmentCard}>
              <div style={styles.shipmentHeader}>
                <div>
                  <span style={styles.shipmentId}>#{s.id}</span>
                  <strong>{s.origin} → {s.destination}</strong>
                  <span style={styles.weight}>{s.weightKg} kg</span>
                </div>
                <div style={styles.shipmentActions}>
                  <span style={{...styles.badge, background: statusColor[s.status] || '#718096'}}>{s.status}</span>
                  <button style={styles.smallBtn} onClick={() => loadBids(s.id)}>
                    {expandedId === s.id ? 'Hide Bids' : 'View Bids'}
                  </button>
                  {s.status === 'IN_TRANSIT' && (
                    <button style={{...styles.smallBtn, background:'#3182ce', color:'#fff'}}
                      onClick={() => setTrackingId(s.id)}>📍 Track</button>
                  )}
                </div>
              </div>

              {/* Bids list */}
              {expandedId === s.id && (
                <div style={styles.bidsSection}>
                  <h4 style={{margin:'0.5rem 0', color:'#4a5568'}}>Bids received:</h4>
                  {(bids[s.id] || []).length === 0 && <p style={styles.empty}>No bids yet.</p>}
                  {(bids[s.id] || []).map(b => (
                    <div key={b.id} style={{...styles.bidRow, background: b.accepted ? '#f0fff4' : '#fff'}}>
                      <div>
                        <strong>₹{b.proposedPrice.toLocaleString()}</strong>
                        <span style={{marginLeft:'0.5rem', color:'#718096', fontSize:'0.85rem'}}>{b.carrierEmail}</span>
                        {b.note && <span style={{marginLeft:'0.5rem', color:'#a0aec0', fontSize:'0.8rem'}}>"{b.note}"</span>}
                      </div>
                      <div>
                        {b.accepted
                          ? <span style={{color:'#38a169', fontWeight:600}}>✅ Awarded</span>
                          : s.status === 'OPEN'
                            ? <button style={styles.awardBtn} onClick={() => handleAward(s.id, b.id)}>Award</button>
                            : <span style={{color:'#a0aec0', fontSize:'0.8rem'}}>Rejected</span>
                        }
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {s.awardedCarrierEmail && (
                <div style={styles.awardedCarrier}>
                  🚛 Awarded to: <strong>{s.awardedCarrierEmail}</strong>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Live Tracking Map */}
        {trackingId && (
          <div style={styles.card}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem'}}>
              <h3 style={styles.cardTitle}>📍 Live Tracking — Shipment #{trackingId}</h3>
              <button style={styles.logoutBtn} onClick={() => setTrackingId(null)}>✕ Close</button>
            </div>
            <TrackingMap shipmentId={trackingId} token={token} />
          </div>
        )}
      </div>
    </div>
  );
}

const styles = {
  page:          { minHeight:'100vh', background:'#f0f4f8', fontFamily:'system-ui, sans-serif' },
  header:        { display:'flex', justifyContent:'space-between', alignItems:'center', padding:'1rem 2rem', background:'#2d3748', color:'#fff' },
  logoutBtn:     { padding:'0.4rem 1rem', background:'#e53e3e', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer' },
  body:          { maxWidth:'900px', margin:'0 auto', padding:'1.5rem', display:'flex', flexDirection:'column', gap:'1.5rem' },
  card:          { background:'#fff', borderRadius:'12px', padding:'1.5rem', boxShadow:'0 2px 8px rgba(0,0,0,0.08)' },
  cardTitle:     { margin:'0 0 1rem', color:'#2d3748', fontSize:'1.1rem' },
  form:          { display:'flex', flexDirection:'column', gap:'0.75rem' },
  row:           { display:'flex', gap:'0.75rem' },
  input:         { flex:1, padding:'0.6rem 0.9rem', border:'1px solid #e2e8f0', borderRadius:'6px', fontSize:'0.9rem' },
  btn:           { padding:'0.7rem', background:'#3182ce', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontWeight:600 },
  refreshBtn:    { padding:'0.4rem 0.9rem', background:'#edf2f7', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.85rem' },
  shipmentCard:  { border:'1px solid #e2e8f0', borderRadius:'8px', padding:'1rem', marginBottom:'0.75rem' },
  shipmentHeader:{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:'0.5rem' },
  shipmentId:    { color:'#a0aec0', fontSize:'0.8rem', marginRight:'0.5rem' },
  weight:        { color:'#718096', fontSize:'0.85rem', marginLeft:'0.5rem' },
  shipmentActions:{ display:'flex', alignItems:'center', gap:'0.5rem', flexWrap:'wrap' },
  badge:         { padding:'2px 10px', borderRadius:'12px', color:'#fff', fontSize:'0.8rem', fontWeight:600 },
  smallBtn:      { padding:'0.35rem 0.75rem', background:'#edf2f7', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.8rem' },
  bidsSection:   { marginTop:'0.75rem', paddingTop:'0.75rem', borderTop:'1px solid #f0f4f8' },
  bidRow:        { display:'flex', justifyContent:'space-between', alignItems:'center', padding:'0.5rem 0.75rem', borderRadius:'6px', border:'1px solid #e2e8f0', marginBottom:'0.5rem' },
  awardBtn:      { padding:'0.3rem 0.75rem', background:'#38a169', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.8rem' },
  awardedCarrier:{ marginTop:'0.5rem', color:'#4a5568', fontSize:'0.85rem' },
  error:         { color:'#c53030', background:'#fff5f5', border:'1px solid #feb2b2', borderRadius:'6px', padding:'0.5rem', fontSize:'0.85rem', marginBottom:'0.5rem' },
  empty:         { color:'#a0aec0', fontSize:'0.9rem', textAlign:'center', padding:'1rem 0' },
};