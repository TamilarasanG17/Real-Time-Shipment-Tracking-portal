// src/pages/CarrierDashboard.js
import { useState, useEffect } from 'react';
import { shipmentsApi, bidsApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function CarrierDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [view, setView] = useState('marketplace'); // 'marketplace' | 'mybids'
  const [openLoads, setOpenLoads] = useState([]);
  const [myBids, setMyBids] = useState([]);
  const [myAssignments, setMyAssignments] = useState([]);
  const [selectedLoad, setSelectedLoad] = useState(null);
  const [bidAmount, setBidAmount] = useState('');
  const [bidNotes, setBidNotes] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchOpenLoads();
    fetchMyBids();
    fetchMyAssignments();
  }, []);

  const fetchOpenLoads = async () => {
    try {
      const { data } = await shipmentsApi.getOpen();
      setOpenLoads(data);
    } catch { setError('Failed to load marketplace'); }
  };

  const fetchMyBids = async () => {
    try {
      const { data } = await bidsApi.getMyBids();
      setMyBids(data);
    } catch { /* silent */ }
  };

  const fetchMyAssignments = async () => {
    try {
      const { data } = await shipmentsApi.getMyAssignments();
      setMyAssignments(data);
    } catch { /* silent */ }
  };

  const placeBid = async (e) => {
    e.preventDefault();
    if (!selectedLoad) return;
    setLoading(true);
    try {
      await bidsApi.placeBid(selectedLoad.id, {
        amount: parseFloat(bidAmount),
        notes: bidNotes,
      });
      setBidAmount('');
      setBidNotes('');
      setSelectedLoad(null);
      fetchMyBids();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to place bid');
    } finally {
      setLoading(false);
    }
  };

  const updateDeliveryStatus = async (shipmentId, status) => {
    try {
      await shipmentsApi.updateStatus(shipmentId, status);
      fetchMyAssignments();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status');
    }
  };

  const nextStatus = { AWAITING_PICKUP: 'IN_TRANSIT', IN_TRANSIT: 'DELIVERED' };
  const nextLabel = { AWAITING_PICKUP: '🚚 Start Transit', IN_TRANSIT: '✅ Mark Delivered' };

  return (
    <div className="dashboard">
      <aside className="sidebar">
        <div className="sidebar-logo">🚚 VT Shipment</div>
        <nav className="sidebar-nav">
          <div className={`nav-item ${view === 'marketplace' ? 'active' : ''}`}
               onClick={() => setView('marketplace')}>
            🏪 Load Board
          </div>
          <div className={`nav-item ${view === 'mybids' ? 'active' : ''}`}
               onClick={() => setView('mybids')}>
            📋 My Bids
          </div>
          <div className={`nav-item ${view === 'assignments' ? 'active' : ''}`}
               onClick={() => setView('assignments')}>
            🚛 Assignments
          </div>
        </nav>
        <div className="sidebar-user">
          <div className="user-avatar">{user?.fullName?.charAt(0)}</div>
          <div>
            <div className="user-name">{user?.fullName}</div>
            <div className="user-role">Carrier</div>
          </div>
          <button className="btn-logout" onClick={logout}>↩</button>
        </div>
      </aside>

      <main className="main-content">
        {error && <div className="error-banner" onClick={() => setError('')}>{error} ✕</div>}

        {/* Marketplace View */}
        {view === 'marketplace' && (
          <>
            <div className="page-header">
              <h1>Load Board</h1>
              <span className="badge">{openLoads.length} open loads</span>
            </div>
            <div className="dashboard-grid">
              <div className="loads-list">
                {openLoads.length === 0 ? (
                  <div className="empty-state"><span>🏪</span><p>No open loads at this time.</p></div>
                ) : openLoads.map((load) => (
                  <div key={load.id}
                       className={`load-card ${selectedLoad?.id === load.id ? 'selected' : ''}`}
                       onClick={() => setSelectedLoad(load)}>
                    <div className="load-header">
                      <span className="load-route">{load.origin} → {load.destination}</span>
                      <span style={{ color: '#22c55e' }}>● OPEN</span>
                    </div>
                    <div className="load-meta">
                      <span>⚖️ {load.weightKg} kg</span>
                      <span>📦 {load.shipperName}</span>
                    </div>
                    {load.description && <p className="load-description">{load.description}</p>}
                  </div>
                ))}
              </div>

              {/* Bid Form */}
              {selectedLoad && (
                <div className="bids-panel">
                  <h3>Place Bid</h3>
                  <div className="selected-load-info">
                    <strong>{selectedLoad.origin} → {selectedLoad.destination}</strong>
                    <br />
                    {selectedLoad.weightKg} kg · Posted by {selectedLoad.shipperName}
                    {selectedLoad.description && <p>{selectedLoad.description}</p>}
                  </div>
                  <form onSubmit={placeBid} className="auth-form">
                    <div className="form-group">
                      <label>Your Price (₹)</label>
                      <input type="number" value={bidAmount}
                             onChange={(e) => setBidAmount(e.target.value)}
                             placeholder="e.g. 15000" min="1" step="100" required />
                    </div>
                    <div className="form-group">
                      <label>Notes (optional)</label>
                      <textarea value={bidNotes}
                                onChange={(e) => setBidNotes(e.target.value)}
                                placeholder="I can pickup by tomorrow morning..."
                                rows={3} />
                    </div>
                    <div className="form-actions">
                      <button type="button" className="btn-secondary"
                              onClick={() => setSelectedLoad(null)}>Cancel</button>
                      <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? 'Submitting...' : '🤝 Submit Bid'}
                      </button>
                    </div>
                  </form>
                </div>
              )}
            </div>
          </>
        )}

        {/* My Bids View */}
        {view === 'mybids' && (
          <>
            <div className="page-header"><h1>My Submitted Bids</h1></div>
            {myBids.length === 0 ? (
              <div className="empty-state"><span>📋</span><p>No bids submitted yet.</p></div>
            ) : myBids.map((bid) => (
              <div key={bid.id} className={`bid-card bid-${bid.status.toLowerCase()}`}>
                <div className="bid-header">
                  <span>Shipment #{bid.shipmentId}</span>
                  <span className="bid-amount">₹{bid.amount.toLocaleString()}</span>
                </div>
                <div className="bid-footer">
                  <span className={`bid-status bid-status-${bid.status.toLowerCase()}`}>
                    {bid.status === 'ACCEPTED' ? '✅ ACCEPTED' :
                     bid.status === 'REJECTED' ? '❌ REJECTED' : '⏳ PENDING'}
                  </span>
                </div>
              </div>
            ))}
          </>
        )}

        {/* Assignments View */}
        {view === 'assignments' && (
          <>
            <div className="page-header"><h1>My Assignments</h1></div>
            {myAssignments.length === 0 ? (
              <div className="empty-state"><span>🚛</span><p>No active assignments yet.</p></div>
            ) : myAssignments.map((load) => (
              <div key={load.id} className="load-card">
                <div className="load-header">
                  <span className="load-route">{load.origin} → {load.destination}</span>
                  <span style={{ color: '#3b82f6' }}>● {load.status}</span>
                </div>
                <div className="load-meta">
                  <span>⚖️ {load.weightKg} kg</span>
                  <span>💰 ₹{load.awardedPrice?.toLocaleString()}</span>
                </div>
                <div className="load-actions">
                  {nextStatus[load.status] && (
                    <button className="btn-sm btn-primary"
                            onClick={() => updateDeliveryStatus(load.id, nextStatus[load.status])}>
                      {nextLabel[load.status]}
                    </button>
                  )}
                  {(load.status === 'AWAITING_PICKUP' || load.status === 'IN_TRANSIT') && (
                    <button className="btn-sm btn-track"
                            onClick={() => navigate(`/track/${load.id}`)}>
                      🗺️ Open Tracking
                    </button>
                  )}
                </div>
              </div>
            ))}
          </>
        )}
      </main>
    </div>
  );
}