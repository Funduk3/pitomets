import api from './axios';

export const animalAPI = {
  getTypes: async () => {
    const res = await api.get('api/animal/types');
    return res.data;
  },
  searchBreeds: async (query, animalType) => {
    const res = await api.get('api/animal/breed/search', {
      params: { query, animalType },
    });
    return res.data;
  },
};
