import api from './axios';

export const photosAPI = {
  // Listing photos
  uploadListingPhoto: async (listingId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post(`/listings/${listingId}/photos`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getListingPhotos: async (listingId) => {
    const response = await api.get(`/listings/${listingId}/photos`);
    return response.data;
  },

  getListingPhoto: (listingId, photoId) => {
    return `${api.defaults.baseURL}/listings/${listingId}/photos/${photoId}.jpg`;
  },

  deleteListingPhoto: async (listingId, photoId) => {
    await api.delete(`/listings/${listingId}/photos/${photoId}`);
  },

  // User avatar
  uploadAvatar: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/users/photos/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getAvatar: async () => {
    const response = await api.get('/users/photos/avatar');
    return response.data;
  },

  getAvatarByUserId: async (userId) => {
    const response = await api.get(`/users/photos/avatar/${userId}`);
    return response.data;
  },

  deleteAvatar: async () => {
    const response = await api.delete('/users/photos/avatar');
    return response.data;
  },
};
