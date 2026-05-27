import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage         from './pages/LoginPage';
import ShipperDashboard  from './pages/ShipperDashboard';
import CarrierDashboard  from './pages/CarrierDashboard';

function PrivateRoute({ children, requiredRole }) {
  const token = localStorage.getItem('token');
  const role  = localStorage.getItem('role');
  if (!token) return <Navigate to="/login" replace />;
  if (requiredRole && role !== requiredRole) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login"   element={<LoginPage />} />
        <Route path="/shipper" element={
          <PrivateRoute requiredRole="SHIPPER"><ShipperDashboard /></PrivateRoute>
        } />
        <Route path="/carrier" element={
          <PrivateRoute requiredRole="CARRIER"><CarrierDashboard /></PrivateRoute>
        } />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}