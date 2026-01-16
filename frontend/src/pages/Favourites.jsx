import { useState, useEffect } from 'react';
import { favouritesAPI } from '../api/favourites';
import { photosAPI } from '../api/photos';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Link } from 'react-router-dom';

export const Favourites = () => {
  const [favourites, setFavourites] = useState([]);
  const [listingsPhotos, setListingsPhotos] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadFavourites();
  }, []);

  const loadFavourites = async () => {
    try {
      const data = await favouritesAPI.getFavourites();
      setFavourites(data);
      
      // Загружаем фотографии для каждого объявления
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
    } catch (err) {
      setError('Failed to load favourites');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (listingId) => {
    try {
      await favouritesAPI.deleteFavourite(listingId);
      setFavourites(favourites.filter(f => f.id !== listingId));
    } catch (err) {
      alert('Failed to remove from favourites');
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <ProtectedRoute>
      <div>
        <h2>Мои избранные</h2>
        {favourites.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>У вас сейчас нет избранных питомцев.</p>
            <Link to="/search" style={{ color: '#3498db' }}>Их можно найти в поиске</Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem', marginTop: '2rem' }}>
            {favourites.map((listing) => {
              const listingPhotos = listingsPhotos[listing.id] || [];
              const firstPhoto = listingPhotos[0];
              
              return (
                <div key={listing.id} style={{ border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
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
                    <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                      <Link
                        to={`/listings/${listing.id}`}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#3498db',
                          color: 'white',
                          textDecoration: 'none',
                          borderRadius: '4px',
                          fontSize: '0.9rem'
                        }}
                      >
                        View
                      </Link>
                      <button
                        onClick={() => handleRemove(listing.id)}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#e74c3c',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '0.9rem'
                        }}
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
};

