import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { photosAPI } from '../api/photos';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Link } from 'react-router-dom';

export const Listings = () => {
  const [listings, setListings] = useState([]);
  const [listingsPhotos, setListingsPhotos] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadListings();
  }, []);

  const loadListings = async () => {
    try {
      setLoading(true);
      const data = await listingsAPI.getMyListings();
      setListings(data);
      
      // Загружаем фотографии для каждого объявления
      const photosPromises = data.map(async (listing) => {
        try {
          const photosData = await photosAPI.getListingPhotos(listing.listingsId);
          return { listingId: listing.listingsId, photos: photosData.photos || [] };
        } catch (err) {
          console.error(`Failed to load photos for listing ${listing.listingsId}:`, err);
          return { listingId: listing.listingsId, photos: [] };
        }
      });
      
      const photosResults = await Promise.all(photosPromises);
      const photosMap = {};
      photosResults.forEach(({ listingId, photos }) => {
        photosMap[listingId] = photos;
      });
      setListingsPhotos(photosMap);
    } catch (err) {
      setError('Failed to load listings');
      console.error('Error loading listings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (listingId) => {
    if (!window.confirm('Are you sure you want to delete this listing?')) return;

    try {
      await listingsAPI.deleteListing(listingId);
      setListings(listings.filter(l => l.listingsId !== listingId));
    } catch (err) {
      alert('Failed to delete listing');
    }
  };

  if (loading) return <div>Грузим...</div>;

  return (
    <ProtectedRoute>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2>Мои объявления</h2>
          <Link
            to="/listings/create"
            style={{
              padding: '0.75rem 1.5rem',
              backgroundColor: '#27ae60',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
          >
            Создать объявление
          </Link>
        </div>
        {error && (
          <div style={{ color: 'red', marginBottom: '1rem', padding: '1rem', backgroundColor: '#ffe6e6', borderRadius: '4px' }}>
            {error}
            <button onClick={loadListings} style={{ marginLeft: '1rem', padding: '0.5rem 1rem', cursor: 'pointer' }}>Retry</button>
          </div>
        )}
        {listings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>У вас пока нет объявлений.</p>
            <Link to="/listings/create" style={{ color: '#3498db' }}>Создать первое объявление</Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
            {listings.map((listing) => {
              const listingPhotos = listingsPhotos[listing.listingsId] || [];
              const firstPhoto = listingPhotos[0];
              
              return (
                <div key={listing.listingsId} style={{ border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
                  {firstPhoto ? (
                    <img
                      src={firstPhoto.startsWith('http') ? firstPhoto : `http://localhost:8080${firstPhoto}`}
                      alt={listing.title || 'Untitled'}
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
                    <h3 style={{ margin: '0 0 0.5rem 0' }}>{listing.title || 'Untitled'}</h3>
                    <p style={{ margin: '0.5rem 0', color: '#666', fontSize: '0.9rem' }}>
                      {listing.description?.substring(0, 100)}
                      {listing.description && listing.description.length > 100 ? '...' : ''}
                    </p>
                    <p><strong>Цена:</strong> {listing.price} ₽</p>
                    <p><strong>Вид:</strong> {listing.species}</p>
                    {listing.breed && <p><strong>Порода:</strong> {listing.breed}</p>}
                    <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <Link
                        to={`/listings/${listing.listingsId}`}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#3498db',
                          color: 'white',
                          textDecoration: 'none',
                          borderRadius: '4px',
                          fontSize: '0.9rem'
                        }}
                      >
                        Посмотреть
                      </Link>
                      <Link
                        to={`/listings/${listing.listingsId}/edit`}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#f39c12',
                          color: 'white',
                          textDecoration: 'none',
                          borderRadius: '4px',
                          fontSize: '0.9rem'
                        }}
                      >
                        Изменить
                      </Link>
                      <button
                        onClick={() => handleDelete(listing.listingsId)}
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
                        Удалить
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

