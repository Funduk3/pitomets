import { useState, useEffect } from 'react';
import { searchAPI } from '../api/search';
import { photosAPI } from '../api/photos';
import { Link } from 'react-router-dom';

export const Search = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [listingsPhotos, setListingsPhotos] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError('');

    try {
      const data = await searchAPI.searchListings(query);
      // Ensure data is an array
      if (Array.isArray(data)) {
        setResults(data);
        
        // Загружаем фотографии для каждого результата
        const photosPromises = data.map(async (listing) => {
          try {
            const photosData = await photosAPI.getListingPhotos(listing.id);
            return { listingId: listing.id, photos: photosData.photos || [] };
          } catch (err) {
            console.error(`Failed to load photos for listing ${listing.id}:`, err);
            return { listingId: listing.id, photos: [] };
          }
        });
        
        const photosResults = await Promise.all(photosPromises);
        const photosMap = {};
        photosResults.forEach(({ listingId, photos }) => {
          photosMap[listingId] = photos;
        });
        setListingsPhotos(photosMap);
      } else {
        console.error('Unexpected response format:', data);
        setError('Invalid response format from server');
        setResults([]);
      }
    } catch (err) {
      console.error('Search error:', err);
      const errorMessage = err.response?.data?.message || err.message || 'Failed to search listings';
      setError(errorMessage);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Search Listings</h2>
      <form onSubmit={handleSearch} style={{ marginBottom: '2rem', display: 'flex', gap: '1rem' }}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for pets..."
          style={{ flex: 1, padding: '0.75rem', fontSize: '1rem', borderRadius: '4px', border: '1px solid #ddd' }}
        />
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '0.75rem 2rem',
            backgroundColor: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            fontSize: '1rem',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      {error && (
        <div style={{ 
          color: 'red', 
          marginBottom: '1rem', 
          padding: '1rem', 
          backgroundColor: '#ffe6e6', 
          borderRadius: '4px',
          border: '1px solid #ff9999'
        }}>
          {error}
        </div>
      )}

      {results.length === 0 && !loading && query && !error && (
        <p>No results found for "{query}"</p>
      )}

      {results.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
          {results.map((listing) => {
            const listingPhotos = listingsPhotos[listing.id] || [];
            const firstPhoto = listingPhotos[0];
            
            return (
              <Link
                key={listing.id}
                to={`/listings/${listing.id}`}
                style={{
                  border: '1px solid #ddd',
                  borderRadius: '8px',
                  overflow: 'hidden',
                  textDecoration: 'none',
                  color: 'inherit',
                  display: 'block',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  backgroundColor: 'white'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = 'none';
                }}
              >
                {firstPhoto ? (
                  <img
                    src={firstPhoto.startsWith('http') ? firstPhoto : `http://localhost:8080${firstPhoto}`}
                    alt={listing.title}
                    style={{
                      width: '100%',
                      height: '200px',
                      objectFit: 'cover',
                      display: 'block'
                    }}
                  />
                ) : (
                  <div style={{
                    width: '100%',
                    height: '200px',
                    backgroundColor: '#f0f0f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#999'
                  }}>
                    Нет фото
                  </div>
                )}
                <div style={{ padding: '1rem' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0' }}>{listing.title}</h3>
                  <p style={{ margin: '0.5rem 0', color: '#666', fontSize: '0.9rem' }}>
                    {listing.description?.substring(0, 100)}
                    {listing.description && listing.description.length > 100 ? '...' : ''}
                  </p>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
};

