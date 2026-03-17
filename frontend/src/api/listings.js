import api from './axios';

const normalizeListing = (listing) => {
  if (!listing || typeof listing !== 'object') return listing;
  const archived = listing.isArchived ?? listing.archived;
  const approved = listing.isApproved ?? listing.approved;
  return { ...listing, isArchived: archived, isApproved: approved };
};

const normalizeListings = (data) => {
  if (Array.isArray(data)) return data.map(normalizeListing);
  return normalizeListing(data);
};

export const listingsAPI = {
  getListing: async (listingId) => {
    const response = await api.get('/listings/', {
      params: { id: listingId },
    });
    return normalizeListing(response.data);
  },

  getMyListings: async () => {
    const response = await api.get('/listings/my');
    return normalizeListings(response.data);
  },

  getSellerListings: async (sellerId, archived = false) => {
    const response = await api.get('/listings/seller', {
      params: { sellerId, archived },
    });
    return normalizeListings(response.data);
  },

  getHomeListings: async (cursor = null) => {
    const response = await api.get('/listings/home', {
      params: cursor == null ? {} : { cursor },
    });
    return {
      ...response.data,
      items: normalizeListings(response.data?.items || []),
    };
  },

  createListing: async (listingData) => {
    const response = await api.post('/listings/', listingData);
    return response.data;
  },

  updateListing: async (listingId, updateData) => {
    const response = await api.put('/listings/', updateData, {
      params: { id: listingId },
    });
    return normalizeListing(response.data);
  },

  archiveListing: async (listingId, archived = true) => {
    const response = await api.put('/listings/archive', null, {
      params: { id: listingId, archived },
    });
    return normalizeListing(response.data);
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
