import api from './axios';

export const userAPI = {
  getCurrentProfile: async () => {
    const response = await api.get('/profile/me');
    return response.data;
  },

  getUserProfile: async (userId) => {
    const response = await api.get(`/profile/user/${userId}`);
    return response.data;
  },

  updateCurrentProfile: async (data) => {
    const response = await api.put('/profile/me', data);
    return response.data;
  },

  getAllUsers: async () => {
    const response = await api.get('/all');
    return response.data;
  },
};
