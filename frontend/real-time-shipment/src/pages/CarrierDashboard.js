import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOpenShipments, getMyBids, submitBid, confirmPickup, confirmDelivery, sendGpsLocation } from '../api/api';


export default function CarrierDashboard() {
  const navigate = useNavigate();
  const token    = localStorage.getItem('token');
  const email    = localStorage.getItem('email');

  const [openShipments, setOpenShipments] = useState([]);
  const [myBids,        setMyBids]        = useState([]);
  const [bidForms,      setBidForms]      = useState({});  // { shipmentId: { proposedPrice, note } }
  const [activeTab,     setActiveTab]     = useState('board');
  const [simShipmentId, setSimShipmentId] = useState('');
  const [simStatus,     setSimStatus]     = useState('');

  useEffect(() => {
    if (!token) { navigate('/login'); return; }
    fetchBoard();
    fetchMyBids();
  }, [token]);

  const fetchBoard  = async () => {
    try { const { data } = await getOpenShipments(); setOpenShipments(data); }
    catch { navigate('/login'); }
  };
  const fetchMyBids = async () => {
    try { const { data } = await getMyBids(); setMyBids(data); }
    catch {}
  };



  const handleBid = async (shipmentId) => {
    const form = bidForms[shipmentId] || {};
    if (!form.proposedPrice) { alert('Enter a price'); return; }
    try {
      await submitBid(shipmentId, { proposedPrice: parseFloat(form.proposedPrice), note: form.note || '' });
      fetchBoard(); fetchMyBids();
      setBidForms(prev => ({ ...prev, [shipmentId]: {} }));
      alert('Bid submitted!');
    } catch (err) {
      alert(err.response?.data?.message || 'Bid failed');
    }
  };

  const handlePickup = async (shipmentId) => {
    try {
      await confirmPickup(shipmentId);
      fetchMyBids();
      alert(`Shipment #${shipmentId} is now IN_TRANSIT`);
    } catch (err) { alert(err.response?.data?.message || 'Pickup failed'); }
  };

  const handleDelivery = async (shipmentId) => {
    try {
      await confirmDelivery(shipmentId);
      fetchMyBids();
      alert(`Shipment #${shipmentId} is DELIVERED`);
    } catch (err) { alert(err.response?.data?.message || 'Delivery failed'); }
  };

  // Simulate a single GPS ping for demo purposes
  const sendPing = async () => {
    const id = parseInt(simShipmentId);
    if (!id) { setSimStatus('Enter a valid shipment ID'); return; }
    // Random coords near Bengaluru for demo
    const lat = 12.9716 + (Math.random() - 0.5) * 0.1;
    const lng = 77.5946 + (Math.random() - 0.5) * 0.1;
    try {
      await sendGpsLocation(id, { latitude: parseFloat(lat.toFixed(6)), longitude: parseFloat(lng.toFixed(6)) });
      setSimStatus(`✅ Ping sent: lat=${lat.toFixed(4)}, lng=${lng.toFixed(4)}`);
    } catch (err) {
      setSimStatus(`❌ ${err.response?.data?.message || 'Ping failed'}`);
    }
  };

  const logout = () => { localStorage.clear(); navigate('/login'); };
  const statusColor = { OPEN:'#38a169', AWAITING_PICKUP:'#d69e2e', IN_TRANSIT:'#3182ce', DELIVERED:'#6b46c1' };

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <span>🚛 Carrier Dashboard — <strong>{email}</strong></span>
        <button style={styles.logoutBtn} onClick={logout}>Logout</button>
      </div>

      {/* Tabs */}
      <div style={styles.tabs}>
        {[['board','📋 Load Board'], ['mybids','🏷️ My Bids'], ['simulate','📡 GPS Simulator']].map(([key, label]) => (
          <button key={key} style={{...styles.tab, ...(activeTab===key ? styles.tabActive : {})}}
            onClick={() => setActiveTab(key)}>{label}
          </button>
        ))}
      </div>

      <div style={styles.body}>

        {/* Load Board */}
        {activeTab === 'board' && (
          <div style={styles.card}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem'}}>
              <h3 style={styles.cardTitle}>Open Loads ({openShipments.length})</h3>
              <button style={styles.refreshBtn} onClick={fetchBoard}>↻ Refresh</button>
            </div>
            {openShipments.length === 0 && <p style={styles.empty}>No open loads available right now.</p>}
            {openShipments.map(s => (
              <div key={s.id} style={styles.shipmentCard}>
                <div style={styles.shipmentHeader}>
                  <div>
                    <span style={styles.shipmentId}>#{s.id}</span>
                    <strong>{s.origin} → {s.destination}</strong>
                    <span style={styles.weight}> · {s.weightKg} kg</span>
                    {s.description && <span style={styles.desc}> · {s.description}</span>}
                  </div>
                  <span style={{...styles.badge, background: statusColor[s.status]}}>{s.status}</span>
                </div>
                <div style={styles.bidFormRow}>
                  <input style={{...styles.input, width:'130px'}} type="number" placeholder="₹ Your price"
                    value={bidForms[s.id]?.proposedPrice || ''}
                    onChange={e => setBidForms(prev => ({ ...prev, [s.id]: { ...prev[s.id], proposedPrice: e.target.value } }))} />
                  <input style={{...styles.input, flex:1}} placeholder="Note (optional)"
                    value={bidForms[s.id]?.note || ''}
                    onChange={e => setBidForms(prev => ({ ...prev, [s.id]: { ...prev[s.id], note: e.target.value } }))} />
                  <button style={styles.bidBtn} onClick={() => handleBid(s.id)}>Submit Bid</button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* My Bids */}
        {activeTab === 'mybids' && (
          <div style={styles.card}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem'}}>
              <h3 style={styles.cardTitle}>My Submitted Bids ({myBids.length})</h3>
              <button style={styles.refreshBtn} onClick={fetchMyBids}>↻ Refresh</button>
            </div>
            {myBids.length === 0 && <p style={styles.empty}>No bids submitted yet.</p>}
            {myBids.map(b => (
              <div key={b.id} style={{...styles.shipmentCard, background: b.accepted ? '#f0fff4' : '#fff'}}>
                <div style={styles.shipmentHeader}>
                  <div>
                    <span style={styles.shipmentId}>Shipment #{b.shipmentId}</span>
                    <strong>₹{b.proposedPrice?.toLocaleString()}</strong>
                    {b.note && <span style={styles.desc}> · "{b.note}"</span>}
                  </div>
                  <div style={{display:'flex', gap:'0.5rem', alignItems:'center'}}>
                    {b.accepted
                      ? <span style={{color:'#38a169', fontWeight:600}}>✅ Awarded</span>
                      : <span style={{color:'#718096', fontSize:'0.85rem'}}>Pending</span>}
                    {b.accepted && (
                      <>
                        <button style={styles.pickupBtn} onClick={() => handlePickup(b.shipmentId)}>Confirm Pickup</button>
                        <button style={styles.deliverBtn} onClick={() => handleDelivery(b.shipmentId)}>Confirm Delivery</button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* GPS Simulator */}
        {activeTab === 'simulate' && (
          <div style={styles.card}>
            <h3 style={styles.cardTitle}>📡 GPS Ping Simulator</h3>
            <p style={{color:'#718096', fontSize:'0.9rem', marginBottom:'1rem'}}>
              Sends a simulated GPS location ping to an IN_TRANSIT shipment.
              The Shipper's dashboard map will update in real time via WebSocket.
            </p>
            <div style={styles.row}>
              <input style={{...styles.input, width:'180px'}} type="number" placeholder="Shipment ID"
                value={simShipmentId} onChange={e => setSimShipmentId(e.target.value)} />
              <button style={{...styles.btn, background:'#805ad5'}} onClick={sendPing}>📡 Send GPS Ping</button>
            </div>
            {simStatus && <p style={{marginTop:'0.75rem', color: simStatus.startsWith('✅') ? '#38a169' : '#c53030'}}>{simStatus}</p>}
            <p style={{marginTop:'1rem', color:'#a0aec0', fontSize:'0.8rem'}}>
              For full simulation (10 pings + pickup/delivery): run<br/>
              <code>node scripts/simulate-driver.js &lt;shipmentId&gt; &lt;carrierToken&gt;</code>
            </p>
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
  tabs:          { display:'flex', background:'#fff', borderBottom:'1px solid #e2e8f0', padding:'0 2rem' },
  tab:           { padding:'0.75rem 1.25rem', background:'none', border:'none', cursor:'pointer', color:'#718096', fontSize:'0.9rem' },
  tabActive:     { color:'#3182ce', borderBottom:'2px solid #3182ce', fontWeight:600 },
  body:          { maxWidth:'900px', margin:'0 auto', padding:'1.5rem' },
  card:          { background:'#fff', borderRadius:'12px', padding:'1.5rem', boxShadow:'0 2px 8px rgba(0,0,0,0.08)' },
  cardTitle:     { margin:'0 0 1rem', color:'#2d3748' },
  shipmentCard:  { border:'1px solid #e2e8f0', borderRadius:'8px', padding:'1rem', marginBottom:'0.75rem' },
  shipmentHeader:{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'0.5rem', flexWrap:'wrap', gap:'0.5rem' },
  shipmentId:    { color:'#a0aec0', fontSize:'0.8rem', marginRight:'0.5rem' },
  weight:        { color:'#718096', fontSize:'0.85rem' },
  desc:          { color:'#a0aec0', fontSize:'0.8rem' },
  badge:         { padding:'2px 10px', borderRadius:'12px', color:'#fff', fontSize:'0.8rem', fontWeight:600 },
  bidFormRow:    { display:'flex', gap:'0.5rem', alignItems:'center', marginTop:'0.5rem', flexWrap:'wrap' },
  input:         { padding:'0.55rem 0.75rem', border:'1px solid #e2e8f0', borderRadius:'6px', fontSize:'0.9rem' },
  bidBtn:        { padding:'0.55rem 1rem', background:'#3182ce', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontWeight:600, fontSize:'0.85rem' },
  pickupBtn:     { padding:'0.3rem 0.7rem', background:'#38a169', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.8rem' },
  deliverBtn:    { padding:'0.3rem 0.7rem', background:'#805ad5', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.8rem' },
  btn:           { padding:'0.6rem 1.2rem', background:'#3182ce', color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer', fontWeight:600 },
  refreshBtn:    { padding:'0.4rem 0.9rem', background:'#edf2f7', border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'0.85rem' },
  row:           { display:'flex', gap:'0.75rem', flexWrap:'wrap' },
  empty:         { color:'#a0aec0', textAlign:'center', padding:'1.5rem 0' },
};