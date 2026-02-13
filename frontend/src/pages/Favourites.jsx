import { useState, useEffect } from 'react';
import { favouritesAPI } from '../api/favourites';
import { resolveApiUrl } from '../api/axios';
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

  if (loading) return <div>Грузим...</div>;
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
          <div className="listings-grid" style={{ marginTop: '2rem' }}>
            {favourites.map((listing) => {
              const listingPhotos = listingsPhotos[listing.id] || [];
              const firstPhoto = listingPhotos[0];

              return (
                <div key={listing.id} className="listing-card">
                  {firstPhoto ? (
                    <img
                      src={resolveApiUrl(firstPhoto)}
                      alt="Listing cover"
                      className="listing-image"
                    />
                  ) : (
                    <div className="listing-placeholder">
                      Нет фото
                    </div>
                  )}
                  <div className="listing-content">
                    <h3>
                      {listing.title || 'Без названия'}
                    </h3>
                    <p>
                      {listing.description?.substring(0, 90)}
                      {listing.description && listing.description.length > 90 ? '...' : ''}
                    </p>
                    <p>
                      <strong>Цена:</strong> <span className="tag-price">{listing.price} ₽</span>
                    </p>
                    <p>
                      <strong>Город:</strong> {listing.cityTitle || '—'}
                    </p>
                    <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem' }}>
                      <Link to={`/listings/${listing.id}`} className="btn btn-secondary" style={{ fontSize: '0.9rem' }}>Посмотреть</Link>
                      <button onClick={() => handleRemove(listing.id)} className="btn btn-danger" style={{ fontSize: '0.9rem' }}>Удалить</button>
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
