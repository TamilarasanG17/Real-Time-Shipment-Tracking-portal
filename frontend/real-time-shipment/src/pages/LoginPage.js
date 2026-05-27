
import React, { useState } from 'react';
import { useNavigate} from 'react-router-dom';
import { login, register } from '../api/api';


export default function LoginPage() {
  const navigate = useNavigate();
  const [tab, setTab]         = useState('login');  // 'login' | 'register'
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);

  const [loginForm, setLoginForm]     = useState({ email: '', password: '' });
  const [registerForm, setRegisterForm] = useState({
    fullName: '', email: '', password: '', role: 'SHIPPER'
  });

  const handleLogin = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const { data } = await login(loginForm);
      localStorage.setItem('token', data.token);
      localStorage.setItem('role', data.role);
      localStorage.setItem('email', data.email);
      // Redirect by role
      navigate(data.role === 'SHIPPER' ? '/shipper' : '/carrier');
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed');
    } finally { setLoading(false); }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      await register(registerForm);
      setTab('login');
      setError('Registration successful — please login.');
    } catch (err) {
      const details = err.response?.data?.details;
      setError(details ? Object.values(details).join(' | ') : 'Registration failed');
    } finally { setLoading(false); }
  };

  return (
    <div style={styles.wrap}>
      <div style={styles.card}>
        <h2 style={styles.title}>🚚 Shipment Tracker</h2>

        {/* Tabs */}
        <div style={styles.tabs}>
          {['login', 'register'].map(t => (
            <button key={t} style={{ ...styles.tab, ...(tab === t ? styles.tabActive : {}) }}
              onClick={() => { setTab(t); setError(''); }}>
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </button>
          ))}
        </div>

        {error && <p style={styles.error}>{error}</p>}

        {tab === 'login' ? (
          <form onSubmit={handleLogin} style={styles.form}>
            <input style={styles.input} type="email" placeholder="Email" required
              value={loginForm.email}
              onChange={e => setLoginForm({ ...loginForm, email: e.target.value })} />
            <input style={styles.input} type="password" placeholder="Password" required
              value={loginForm.password}
              onChange={e => setLoginForm({ ...loginForm, password: e.target.value })} />
            <button style={styles.btn} type="submit" disabled={loading}>
              {loading ? 'Logging in…' : 'Login'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleRegister} style={styles.form}>
            <input style={styles.input} type="text" placeholder="Full Name" required
              value={registerForm.fullName}
              onChange={e => setRegisterForm({ ...registerForm, fullName: e.target.value })} />
            <input style={styles.input} type="email" placeholder="Email" required
              value={registerForm.email}
              onChange={e => setRegisterForm({ ...registerForm, email: e.target.value })} />
            <input style={styles.input} type="password" placeholder="Password (min 8 chars)" required
              value={registerForm.password}
              onChange={e => setRegisterForm({ ...registerForm, password: e.target.value })} />
            <select style={styles.input} value={registerForm.role}
              onChange={e => setRegisterForm({ ...registerForm, role: e.target.value })}>
              <option value="SHIPPER">SHIPPER — Post loads</option>
              <option value="CARRIER">CARRIER — Take jobs</option>
            </select>
            <button style={styles.btn} type="submit" disabled={loading}>
              {loading ? 'Registering…' : 'Register'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

const styles = {
  wrap:      { display:'flex', alignItems:'center', justifyContent:'center', minHeight:'100vh', background:'#f0f4f8' },
  card:      { background:'#fff', padding:'2rem', borderRadius:'12px', width:'360px', boxShadow:'0 4px 20px rgba(0,0,0,0.1)' },
  title:     { textAlign:'center', marginBottom:'1.5rem', fontSize:'1.4rem', color:'#1a202c' },
  tabs:      { display:'flex', marginBottom:'1.5rem', borderRadius:'8px', overflow:'hidden', border:'1px solid #e2e8f0' },
  tab:       { flex:1, padding:'0.6rem', background:'#f7fafc', border:'none', cursor:'pointer', fontSize:'0.9rem', color:'#4a5568' },
  tabActive: { background:'#3182ce', color:'#fff', fontWeight:600 },
  form:      { display:'flex', flexDirection:'column', gap:'0.75rem' },
  input:     { padding:'0.65rem 0.9rem', border:'1px solid #e2e8f0', borderRadius:'6px', fontSize:'0.9rem', outline:'none' },
  btn:       { padding:'0.75rem', background:'#3182ce', color:'#fff', border:'none', borderRadius:'6px', fontSize:'1rem', cursor:'pointer', fontWeight:600 },
  error:     { color:'#c53030', background:'#fff5f5', border:'1px solid #feb2b2', borderRadius:'6px', padding:'0.5rem 0.75rem', fontSize:'0.85rem', marginBottom:'0.75rem' },
};