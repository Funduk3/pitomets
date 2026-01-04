import api from './axios';

export const searchAPI = {
  searchListings: async (query, page = 0, size = 10) => {
    try {
    const response = await api.get('/search/listings', {
      params: { query, page, size },
    });
      // Log response for debugging
      console.log('Search API response:', response.data);
      return response.data || [];
    } catch (error) {
      console.error('Search API error:', error);
      console.error('Error details:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        message: error.message
      });
      throw error;
    }
  },
};

