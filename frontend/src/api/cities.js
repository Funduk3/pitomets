import api from './axios';

export const citiesAPI = {
    search: async (query) => {
        const res = await api.get(`api/cities`, { params: { query } });
        return res.data;
    },
};
