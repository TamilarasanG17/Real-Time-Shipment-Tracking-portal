// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ShipperDashboard from './pages/ShipperDashboard';
import CarrierDashboard from './pages/CarrierDashboard';
import TrackingPage from './pages/TrackingPage';
import './styles/App.css';

function ProtectedRoute({ children, requiredRole }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to={`/dashboard/${user.role.toLowerCase()}`} replace />;
  }
  return children;
}

function RootRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={`/dashboard/${user.role.toLowerCase()}`} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route path="/dashboard/shipper" element={
            <ProtectedRoute requiredRole="SHIPPER">
              <ShipperDashboard />
            </ProtectedRoute>
          } />

          <Route path="/dashboard/carrier" element={
            <ProtectedRoute requiredRole="CARRIER">
              <CarrierDashboard />
            </ProtectedRoute>
          } />

          <Route path="/track/:shipmentId" element={
            <ProtectedRoute>
              <TrackingPage />
            </ProtectedRoute>
          } />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}