import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { resolveApiUrl } from '../api/axios';
import { photosAPI } from '../api/photos';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { useAuth } from '../context/AuthContext';

export const Listings = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
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
      setError('Не удалось загрузить объявления');
      console.error('Error loading listings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (listingId) => {
    if (!window.confirm('Удалить объявление?')) return;

    try {
      await listingsAPI.deleteListing(listingId);
      setListings(listings.filter(l => l.listingsId !== listingId));
    } catch (err) {
      alert('Не удалось удалить объявление');
    }
  };

  if (loading) return <div>Грузим...</div>;

  return (
    <ProtectedRoute>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2>Мои объявления</h2>
        </div>
        {error && (
          <div style={{ color: 'red', marginBottom: '1rem', padding: '1rem', backgroundColor: '#ffe6e6', borderRadius: '4px' }}>
            {error}
            <button onClick={loadListings} style={{ marginLeft: '1rem', padding: '0.5rem 1rem', cursor: 'pointer' }}>Повторить</button>
          </div>
        )}
        {listings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>У вас пока нет объявлений.</p>
            {user?.sellerProfileApproved === true ? (
              <Link to="/listings/create" style={{ color: '#3498db' }}>Создать первое объявление</Link>
            ) : (
              <p className="small-muted" style={{ marginTop: '0.5rem' }}>
                Профиль продавца на модерации. Создание объявлений будет доступно после одобрения.
              </p>
            )}
          </div>
        ) : (
          <div className="listings-grid" style={{ marginTop: '2rem' }}>
            {listings.map((listing) => {
              const listingPhotos = listingsPhotos[listing.listingsId] || [];
              const firstPhoto = listingPhotos[0];
              const approved = listing.isApproved ?? listing.approved;
              const moderationPending = listing.manualModerationPending === true;
              const showPending = (moderationPending || (approved === false || approved === 0)) && !listing.moderatorMessage;
              
              return (
                <Link key={listing.listingsId} to={`/listings/${listing.listingsId}`} className="listing-card">
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
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <h3 style={{ margin: 0 }}>{listing.title || 'Без названия'}</h3>
                      {showPending && (
                        <span style={{ padding: '0.2rem 0.5rem', backgroundColor: '#f2f2f2', color: '#666', borderRadius: '999px', fontSize: '0.8rem' }}>
                          На модерации
                        </span>
                      )}
                      {listing.moderatorMessage && (
                        <span style={{ padding: '0.2rem 0.5rem', backgroundColor: '#fdecea', color: '#c0392b', borderRadius: '999px', fontSize: '0.8rem' }}>
                          Отклонено
                        </span>
                      )}
                    </div>
                    <p>
                      {listing.description?.substring(0, 90)}
                      {listing.description && listing.description.length > 90 ? '...' : ''}
                    </p>
                    {listing.moderatorMessage && (
                      <p style={{ margin: '0.5rem 0', color: '#c0392b', fontSize: '0.9rem' }}>
                        Сообщение модератора: {listing.moderatorMessage}
                      </p>
                    )}
                    <p>
                      <strong>Цена:</strong> <span className="tag-price">{listing.price} ₽</span>
                    </p>
                    <p>
                      <strong>Город:</strong> {listing.city?.title || '—'}
                    </p>
                    <p className="small-muted">
                      <strong>Просмотры:</strong> {listing.viewsCount ?? 0}
                      {' • '}
                      <strong>В избранных:</strong> {listing.likesCount ?? 0}
                    </p>
                    <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          navigate(`/listings/${listing.listingsId}/edit`);
                        }}
                        className="btn"
                        style={{ backgroundColor: '#f39c12', color: '#fff', fontSize: '0.9rem' }}
                      >
                        Изменить
                      </button>
                      <button
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          handleDelete(listing.listingsId);
                        }}
                        className="btn btn-danger"
                        style={{ fontSize: '0.9rem' }}
                      >
                        Удалить
                      </button>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
};
