import api from './axios';

export const sellerAPI = {
  createSellerProfile: async (profileData) => {
    const response = await api.post('/seller/profile', profileData);
    return response.data;
  },

  updateSellerProfile: async (profileData) => {
    const response = await api.put('/seller/profile', profileData);
    return response.data;
  },

  getSellerProfile: async (sellerId) => {
    const response = await api.get('/seller/profile', {
      params: { sellerId },
    });
    return response.data;
  },

  getSellerReviews: async (sellerProfileId) => {
    const response = await api.get(`/seller/${sellerProfileId}/reviews/`);
    return response.data;
  },

  createSellerReview: async (sellerProfileId, reviewData) => {
    const response = await api.post(`/seller/${sellerProfileId}/reviews/`, reviewData);
    return response.data;
  },

  updateSellerReview: async (sellerProfileId, reviewData) => {
    const response = await api.put(`/seller/${sellerProfileId}/reviews/`, reviewData);
    return response.data;
  },

  deleteSellerReview: async (sellerProfileId, reviewId) => {
    await api.delete(`/seller/${sellerProfileId}/reviews/${reviewId}`);
  },
};