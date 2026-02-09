import api from './axios';

export const listingsAPI = {
  getListing: async (listingId) => {
    const response = await api.get('/listings/', {
      params: { id: listingId },
    });
    return response.data;
  },

  getMyListings: async () => {
    const response = await api.get('/listings/my');
    return response.data;
  },

  getSellerListings: async (sellerId) => {
    const response = await api.get('/listings/seller', {
      params: { sellerId },
    });
    return response.data;
  },

  getHomeListings: async (cursor = null) => {
    const response = await api.get('/listings/home', {
      params: cursor == null ? {} : { cursor },
    });
    return response.data;
  },

  createListing: async (listingData) => {
    const response = await api.post('/listings/', listingData);
    return response.data;
  },

  updateListing: async (listingId, updateData) => {
    const response = await api.put('/listings/', updateData, {
      params: { id: listingId },
    });
    return response.data;
  },

  deleteListing: async (listingId) => {
    await api.delete('/listings/', {
      params: { id: listingId },
    });
  },

  getListingReviews: async (listingId) => {
    const response = await api.get('/listings/reviews/', {
      params: { id: listingId },
    });
    return response.data;
  },

  createReview: async (reviewData) => {
    const response = await api.post('/listings/reviews/', reviewData);
    return response.data;
  },

  updateReview: async (reviewData) => {
    const response = await api.put('/listings/reviews/', reviewData);
    return response.data;
  },

  deleteReview: async (reviewId) => {
    await api.delete(`/listings/reviews/${reviewId}`);
  },

  getSimilarListings: (listingId, size = 6) =>
      api.get(`/search/listings/${listingId}/similar`, {
        params: { size },
      }).then(res => res.data),
};
