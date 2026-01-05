import api from './axios';

export const userAPI = {
  getCurrentProfile: async () => {
    const response = await api.get('/profile/me');
    return response.data;
  },

  getAllUsers: async () => {
    const response = await api.get('/all');
    return response.data;
  },

  getSellerReviews: async (sellerProfileId) => {
    const response = await api.get('/reviews', {
      params: { id: sellerProfileId },
    });
    return response.data;
  },
};

