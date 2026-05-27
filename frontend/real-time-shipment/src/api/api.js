import axiosClient from './axiosClient';

// ── Auth ────────────────────────────────────────────────────────────
export const register = (data) => axiosClient.post('/api/auth/register', data);
export const login    = (data) => axiosClient.post('/api/auth/login', data);

// ── Shipments ───────────────────────────────────────────────────────
export const postShipment     = (data) => axiosClient.post('/api/shipments', data);
export const getOpenShipments = ()     => axiosClient.get('/api/shipments/open');
export const getMyShipments   = ()     => axiosClient.get('/api/shipments/mine');
export const getShipmentById  = (id)   => axiosClient.get(`/api/shipments/${id}`);

// ── Bids ─────────────────────────────────────────────────────────────
export const submitBid         = (shipmentId, data) => axiosClient.post(`/api/shipments/${shipmentId}/bids`, data);
export const getBidsForShipment= (shipmentId)       => axiosClient.get(`/api/shipments/${shipmentId}/bids`);
export const awardBid          = (shipmentId, bidId)=> axiosClient.post(`/api/shipments/${shipmentId}/bids/${bidId}/award`);
export const getMyBids         = ()                 => axiosClient.get('/api/bids/mine');

// ── Tracking ─────────────────────────────────────────────────────────
export const confirmPickup    = (shipmentId)       => axiosClient.post(`/api/tracking/${shipmentId}/pickup`);
export const confirmDelivery  = (shipmentId)       => axiosClient.post(`/api/tracking/${shipmentId}/delivery`);
export const getPingHistory   = (shipmentId)       => axiosClient.get(`/api/tracking/${shipmentId}/history`);
export const getLatestPing    = (shipmentId)       => axiosClient.get(`/api/tracking/${shipmentId}/latest`);
export const sendGpsLocation  = (shipmentId, data) => axiosClient.post(`/api/tracking/${shipmentId}/location`, data);