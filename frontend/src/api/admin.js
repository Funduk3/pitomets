import api from './axios';

export const adminAPI = {
  getPendingListings: async () => {
    const response = await api.get('/admin/listing/all');
    return response.data;
  },

  getPendingReviews: async () => {
    const response = await api.get('/admin/review/all');
    return response.data;
  },

  getPendingSellerProfiles: async () => {
    const response = await api.get('/admin/seller-profile/all');
    return response.data;
  },

  getPendingPhotos: async () => {
    const response = await api.get('/admin/photo/all');
    return response.data;
  },

  approveListing: async (id) => {
    await api.post(`/admin/listing/${id}/approve`);
  },

  declineListing: async (id, message) => {
    await api.post(`/admin/listing/${id}/decline`, { id, message });
  },

  approveReview: async (id) => {
    await api.post(`/admin/review/${id}/approve`);
  },

  declineReview: async (id, message) => {
    await api.post(`/admin/review/${id}/decline`, { id, message });
  },

  approveSellerProfile: async (id) => {
    await api.post(`/admin/seller-profile/${id}/approve`);
  },

  declineSellerProfile: async (id, message) => {
    await api.post(`/admin/seller-profile/${id}/decline`, { id, message });
  },
};
