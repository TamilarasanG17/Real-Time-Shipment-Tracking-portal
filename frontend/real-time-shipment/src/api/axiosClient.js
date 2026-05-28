
import axios from 'axios';

const axiosClient = axios.create({
  baseURL: 'http://localhost:9090',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT token to every request automatically
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: redirect to login on 401 (expired token)
// axiosClient.interceptors.response.use(
//   (response) => response,
//   (error) => {
//     if (error.response?.status === 401) {
//       localStorage.removeItem('token');
//       localStorage.removeItem('role');
//       window.location.href = '/login';
//     }
//     return Promise.reject(error);
//   }
// );

// Response interceptor
axiosClient.interceptors.response.use(

  (response) => response,

  (error) => {

    const status = error.response?.status;

    console.log("API Error Status:", status);
    console.log("API Error Data:", error.response?.data);

    // Only logout for REAL unauthorized token issues
    if (status === 401) {

      const token = localStorage.getItem('token');

      if (token) {

        console.warn("Session expired. Redirecting to login...");

        localStorage.removeItem('token');
        localStorage.removeItem('role');

        setTimeout(() => {
          window.location.href = '/login';
        }, 1000);
      }
    }

    // DO NOT logout on 403
    if (status === 403) {
      console.warn("Forbidden request");
    }

    return Promise.reject(error);
  }
);

export default axiosClient;