import axios from 'axios';

export const API_BASE_URL = 'http://localhost:8080';
// export const API_BASE_URL = 'https://pitomets.com/api';

const normalizeBase = (base) => (base.endsWith('/') ? base.slice(0, -1) : base);
const API_BASE = normalizeBase(API_BASE_URL);

export const resolveApiUrl = (path) => {
    if (!path) return path;
    if (path.startsWith('http')) return path;
    const base = API_BASE.startsWith('http')
        ? API_BASE
        : `${window.location.origin}${API_BASE.startsWith('/') ? '' : '/'}${API_BASE}`;
    const prefix = normalizeBase(base);
    const p = path.startsWith('/') ? path : `/${path}`;
    return `${prefix}${p}`;
};

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/refresh`, {
            refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data;
          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', newRefreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
