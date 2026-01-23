import api from './axios';

export const metroAPI = {
    search: async (query, cityId) => {
        const response = await api.get('api/metro', { params: { query, cityId } });
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`api/metro/${id}`);
        return response.data;
    }
};
