import { useState, useEffect } from 'react';
import { shipmentsApi, bidsApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function ShipperDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [myLoads, setMyLoads] = useState([]);
  const [selectedLoad, setSelectedLoad] = useState(null);
  const [bids, setBids] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [form, setForm] = useState({
    origin: '', destination: '', weightKg: '', description: '',
  });

  useEffect(() => {
    fetchMyLoads();
  }, []);

  const fetchMyLoads = async () => {
    try {
      const { data } = await shipmentsApi.getMyLoads();
      setMyLoads(data);
    } catch (err) {
      setError('Failed to fetch loads');
    }
  };

  const handlePostLoad = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await shipmentsApi.create({ ...form, weightKg: parseFloat(form.weightKg) });
      setShowForm(false);
      setForm({ origin: '', destination: '', weightKg: '', description: '' });
      fetchMyLoads();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to post load');
    } finally {
      setLoading(false);
    }
  };

  const viewBids = async (shipment) => {
    setSelectedLoad(shipment);
    try {
      const { data } = await bidsApi.getBids(shipment.id);
      setBids(data);
    } catch (err) {
      setError('Failed to fetch bids');
    }
  };

  const acceptBid = async (bidId) => {
    try {
      await bidsApi.acceptBid(bidId);
      fetchMyLoads();
      viewBids(selectedLoad);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to accept bid');
    }
  };

  const cancelLoad = async (id) => {
    if (!window.confirm('Cancel this shipment?')) return;
    try {
      await shipmentsApi.cancel(id);
      fetchMyLoads();
      setSelectedLoad(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel');
    }
  };

  const statusColor = {
    OPEN: '#22c55e', AWAITING_PICKUP: '#f59e0b',
    IN_TRANSIT: '#3b82f6', DELIVERED: '#8b5cf6', CANCELLED: '#ef4444',
  };

  return (
    <div className="dashboard">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">🚚 VT Shipment</div>
        <nav className="sidebar-nav">
          <div className="nav-item active">📦 My Loads</div>
          <div className="nav-item" onClick={() => navigate(`/track/${selectedLoad?.id}`)}
               style={{ opacity: selectedLoad ? 1 : 0.4 }}>
            🗺️ Track Shipment
          </div>
        </nav>
        <div className="sidebar-user">
          <div className="user-avatar">{user?.fullName?.charAt(0)}</div>
          <div>
            <div className="user-name">{user?.fullName}</div>
            <div className="user-role">Shipper</div>
          </div>
          <button className="btn-logout" onClick={logout}>↩</button>
        </div>
      </aside>

      {/* Main content */}
      <main className="main-content">
        <div className="page-header">
          <h1>My Freight Loads</h1>
          <button className="btn-primary" onClick={() => setShowForm(true)}>
            + Post New Load
          </button>
        </div>

        {error && <div className="error-banner" onClick={() => setError('')}>{error} ✕</div>}

        {/* Post Load Form */}
        {showForm && (
          <div className="modal-overlay" onClick={() => setShowForm(false)}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
              <h2>Post a New Load</h2>
              <form onSubmit={handlePostLoad} className="auth-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>Origin City</label>
                    <input value={form.origin}
                           onChange={(e) => setForm(p => ({ ...p, origin: e.target.value }))}
                           placeholder="e.g., Mumbai" required />
                  </div>
                  <div className="form-group">
                    <label>Destination City</label>
                    <input value={form.destination}
                           onChange={(e) => setForm(p => ({ ...p, destination: e.target.value }))}
                           placeholder="e.g., Delhi" required />
                  </div>
                </div>
                <div className="form-group">
                  <label>Weight (kg)</label>
                  <input type="number" value={form.weightKg}
                         onChange={(e) => setForm(p => ({ ...p, weightKg: e.target.value }))}
                         placeholder="500" min="1" required />
                </div>
                <div className="form-group">
                  <label>Description (optional)</label>
                  <textarea value={form.description}
                            onChange={(e) => setForm(p => ({ ...p, description: e.target.value }))}
                            placeholder="Fragile goods, refrigerated, etc." rows={3} />
                </div>
                <div className="form-actions">
                  <button type="button" className="btn-secondary"
                          onClick={() => setShowForm(false)}>Cancel</button>
                  <button type="submit" className="btn-primary" disabled={loading}>
                    {loading ? 'Posting...' : 'Post Load'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        <div className="dashboard-grid">
          {/* Loads list */}
          <div className="loads-list">
            {myLoads.length === 0 ? (
              <div className="empty-state">
                <span>📦</span>
                <p>No loads posted yet. Post your first freight load!</p>
              </div>
            ) : (
              myLoads.map((load) => (
                <div
                  key={load.id}
                  className={`load-card ${selectedLoad?.id === load.id ? 'selected' : ''}`}
                  onClick={() => viewBids(load)}
                >
                  <div className="load-header">
                    <span className="load-route">
                      {load.origin} → {load.destination}
                    </span>
                    <span className="load-status"
                          style={{ color: statusColor[load.status] }}>
                      ● {load.status}
                    </span>
                  </div>
                  <div className="load-meta">
                    <span>⚖️ {load.weightKg} kg</span>
                    {load.awardedPrice && (
                      <span>💰 ₹{load.awardedPrice.toLocaleString()}</span>
                    )}
                    {load.awardedCarrierName && (
                      <span>🚛 {load.awardedCarrierName}</span>
                    )}
                  </div>
                  {load.description && (
                    <p className="load-description">{load.description}</p>
                  )}
                  <div className="load-actions">
                    {load.status === 'IN_TRANSIT' || load.status === 'AWAITING_PICKUP' ? (
                      <button className="btn-sm btn-track"
                              onClick={(e) => { e.stopPropagation(); navigate(`/track/${load.id}`); }}>
                        🗺️ Track
                      </button>
                    ) : null}
                    {load.status === 'OPEN' && (
                      <button className="btn-sm btn-danger"
                              onClick={(e) => { e.stopPropagation(); cancelLoad(load.id); }}>
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Bids panel */}
          {selectedLoad && (
            <div className="bids-panel">
              <h3>Bids for: {selectedLoad.origin} → {selectedLoad.destination}</h3>
              {bids.length === 0 ? (
                <div className="empty-state">
                  <span>🤝</span>
                  <p>No bids yet. Carriers will see your load on the marketplace.</p>
                </div>
              ) : (
                bids.map((bid) => (
                  <div key={bid.id} className={`bid-card bid-${bid.status.toLowerCase()}`}>
                    <div className="bid-header">
                      <span className="bid-carrier">{bid.carrierName}</span>
                      <span className="bid-amount">₹{bid.amount.toLocaleString()}</span>
                    </div>
                    {bid.notes && <p className="bid-notes">{bid.notes}</p>}
                    <div className="bid-footer">
                      <span className={`bid-status bid-status-${bid.status.toLowerCase()}`}>
                        {bid.status}
                      </span>
                      {bid.status === 'PENDING' && selectedLoad.status === 'OPEN' && (
                        <button className="btn-sm btn-accept"
                                onClick={() => acceptBid(bid.id)}>
                          ✓ Accept Bid
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}