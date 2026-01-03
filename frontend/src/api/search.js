import api from './axios';

export const searchAPI = {
  searchListings: async (query, page = 0, size = 10) => {
    const response = await api.get('/search/listings', {
      params: { query, page, size },
    });
    return response.data;
  },
};

