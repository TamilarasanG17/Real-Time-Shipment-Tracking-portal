// src/services/api.js
import axios from 'axios';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:9090';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach JWT token ─────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: handle 401 globally ─────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ── Auth API ──────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data) => api.post('/api/auth/register', data),
  login:    (data) => api.post('/api/auth/login', data),
};

// ── Shipments API ─────────────────────────────────────────────────────────────
export const shipmentsApi = {
  getOpen:         ()           => api.get('/api/shipments'),
  getMyLoads:      ()           => api.get('/api/shipments/my-loads'),
  getMyAssignments:()           => api.get('/api/shipments/my-assignments'),
  getById:         (id)         => api.get(`/api/shipments/${id}`),
  create:          (data)       => api.post('/api/shipments', data),
  updateStatus:    (id, status) => api.patch(`/api/shipments/${id}/status?status=${status}`),
  cancel:          (id)         => api.patch(`/api/shipments/${id}/cancel`),
};

// ── Bids API ──────────────────────────────────────────────────────────────────
export const bidsApi = {
  placeBid:    (shipmentId, data) => api.post(`/api/bids/shipment/${shipmentId}`, data),
  getBids:     (shipmentId)       => api.get(`/api/bids/shipment/${shipmentId}`),
  acceptBid:   (bidId)            => api.post(`/api/bids/${bidId}/accept`),
  getMyBids:   ()                 => api.get('/api/bids/my-bids'),
};

// ── Tracking API (REST fallback) ──────────────────────────────────────────────
export const trackingApi = {
  getLastPosition: (shipmentId) => api.get(`/api/tracking/${shipmentId}`),
};

export default api;