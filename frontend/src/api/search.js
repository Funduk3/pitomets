import api from './axios';

export const searchAPI = {
    searchListings: async (
        query,
        page = 0,
        size = 10,
        {
            city = null,
            metro = null,
            priceFrom = null,
            priceTo = null,
            types = null,
            breeds = null,
            genders = null,
            ages = null
        } = {}
    ) => {
        try {
            const params = {
                query,
                page,
                size,
            };

            if (city !== null) params.city = city;
            if (metro !== null) params.metro = metro;
            if (priceFrom !== null) params.priceFrom = priceFrom;
            if (priceTo !== null) params.priceTo = priceTo;
            if (Array.isArray(types) && types.length) params.types = types.join(',');
            if (Array.isArray(breeds) && breeds.length) params.breeds = breeds.join(',');
            if (Array.isArray(genders) && genders.length) params.genders = genders.join(',');
            if (Array.isArray(ages) && ages.length) params.ages = ages.join(',');

            const response = await api.get('/search/listings', { params });

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

    autocomplete: async (query, size = 5) => {
        try {
            const response = await api.get('/search/listings/autocomplete', {
                params: { query, size },
            });
            console.log('Autocomplete response:', response.data);
            return response.data || [];
        } catch (error) {
            console.error('Autocomplete API error:', error);
            return [];
        }
    },
};
