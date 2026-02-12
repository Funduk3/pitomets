import api from './axios';

export const authAPI = {
  register: async (email, passwordHash, fullName) => {
    const response = await api.post('/register', {
      email,
      passwordHash,
      fullName,
    });
    return response.data;
  },

  login: async (email, passwordHash) => {
    const response = await api.post('/login', {
      email,
      passwordHash,
    });
    return response.data;
  },

  refresh: async (refreshToken) => {
    const response = await api.post('/refresh', {
      refreshToken,
    });
    return response.data;
  },

  logout: async (refreshToken) => {
    await api.post('/logout', {
      refreshToken,
    });
  },

  confirm: async (token) => {
    await api.get(`/confirm?token=${token}`);
  },

  forgotPassword: async (email) => {
    await api.post('/forgot-password', { email });
  },

  resetPassword: async (token, newPassword, confirmPassword) => {
    await api.post('/reset-password', { token, newPassword, confirmPassword });
  },

  changePassword: async (currentPassword, newPassword, confirmPassword) => {
    await api.post('/change-password', { currentPassword, newPassword, confirmPassword });
  },
};
