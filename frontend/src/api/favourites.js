import api from './axios';

export const favouritesAPI = {
  getFavourites: async () => {
    const response = await api.get('/favourites');
    return response.data;
  },

  addFavourite: async (listingId) => {
    const response = await api.post('/favourites', { listingId });
    return response.data;
  },

  deleteFavourite: async (listingId) => {
    await api.delete('/favourites', { data: { listingId } });
  },
};

