import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link, useLocation } from 'react-router-dom';
import { sellerAPI } from '../api/seller';
import { resolveApiUrl } from '../api/axios';
import { photosAPI } from '../api/photos';
import { messengerAPI } from '../api/messenger';
import { listingsAPI } from '../api/listings';
import { useAuth } from '../context/AuthContext';
import { GENDER_LABELS } from '../util/gender';
import { AGE_LABELS } from '../util/age';

export const SellerProfileView = () => {
  const { sellerId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();
  const [profile, setProfile] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [listings, setListings] = useState([]);
  const [listingsPhotos, setListingsPhotos] = useState({});
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingListings, setLoadingListings] = useState(false);
  const [activeTab, setActiveTab] = useState('listings');
  const [error, setError] = useState('');

  useEffect(() => {
    if (sellerId) {
      loadProfile();
    }
  }, [sellerId]);

  useEffect(() => {
    if (profile?.userId) {
      loadAvatar(profile.userId);
    }
  }, [profile]);

  const loadProfile = async () => {
    try {
      const parsedId = parseInt(sellerId);
      let data;
      try {
        data = await sellerAPI.getSellerProfileById(parsedId);
      } catch (e) {
        data = await sellerAPI.getSellerProfile(parsedId);
      }
      setProfile(data);
    } catch (err) {
      setError('Failed to load seller profile');
      console.error('Failed to load profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadReviews = async () => {
    try {
      if (profile?.id) {
        const data = await sellerAPI.getSellerReviews(profile.id);
        setReviews(data);
      }
    } catch (err) {
      console.error('Failed to load reviews:', err);
    }
  };

  useEffect(() => {
    if (profile?.id) {
      loadReviews();
    }
  }, [profile]);

  useEffect(() => {
    if (profile?.userId) {
      loadListings();
    }
  }, [profile]);

  const loadAvatar = async (userId) => {
    try {
      const data = await photosAPI.getAvatarByUserId(userId);
      setAvatarUrl(data?.url || null);
    } catch (err) {
      console.error('Failed to load avatar:', err);
      setAvatarUrl(null);
    }
  };

  const loadListings = async () => {
    if (!profile?.userId) return;
    setLoadingListings(true);
    try {
      const data = await listingsAPI.getSellerListings(profile.userId);
      // Фильтруем только неархивированные объявления
      const activeListings = data.filter(listing => !listing.isArchived);
      setListings(activeListings);
      
      // Загружаем фотографии для каждого объявления
      const photosPromises = activeListings.map(async (listing) => {
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
      console.error('Failed to load listings:', err);
      setListings([]);
    } finally {
      setLoadingListings(false);
    }
  };

  const handleMessageSeller = async () => {
    if (!isAuthenticated()) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }

    if (!profile?.userId) {
      alert('Seller information not available');
      return;
    }

    try {
      const chat = await messengerAPI.createOrGetChat(profile.userId, null, null);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      alert('Failed to create chat');
    }
  };

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!profile) return <div>Seller profile not found</div>;

  return (
    <div className="container">
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card-body" style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
          {avatarUrl && (
            <img src={avatarUrl} alt="Seller avatar" className="avatar-md" />
          )}
          <div style={{ flex: 1 }}>
            <h1 style={{ margin: '0 0 0.25rem 0' }}>{profile.shopName}</h1>
            {profile.description && <p className="small-muted" style={{ marginBottom: '0.5rem' }}>{profile.description}</p>}

            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
              {profile.rating != null && (
                <div className="small-muted" style={{ margin: 0 }}>
                  <strong>Рейтинг:</strong> {profile.rating.toFixed(2)} / 5
                </div>
              )}
              {profile.isVerified && (
                <span className="btn btn-primary" style={{ padding: '0.25rem 0.5rem', borderRadius: '6px', fontSize: '0.9rem' }}>
                  ✓ Проверен
                </span>
              )}
            </div>

            {user?.id !== profile.userId && (
              <div style={{ marginTop: '1rem' }}>
                <button onClick={handleMessageSeller} className="btn btn-primary">Написать продавцу</button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div>
        <div className="tabs" role="tablist" aria-label="Seller sections">
          <button
            role="tab"
            aria-selected={activeTab === 'listings'}
            aria-controls="tab-listings"
            id="tab-button-listings"
            className={`tab ${activeTab === 'listings' ? 'active' : ''}`}
            onClick={() => setActiveTab('listings')}
          >
            Объявления ({listings.length})
          </button>
          <button
            role="tab"
            aria-selected={activeTab === 'reviews'}
            aria-controls="tab-reviews"
            id="tab-button-reviews"
            className={`tab ${activeTab === 'reviews' ? 'active' : ''}`}
            onClick={() => setActiveTab('reviews')}
          >
            Отзывы ({reviews.length})
          </button>
        </div>

        {activeTab === 'listings' && (
          <div id="tab-listings" role="tabpanel" aria-labelledby="tab-button-listings">
            {loadingListings ? (
              <p>Загрузка объявлений...</p>
            ) : listings.length === 0 ? (
              <p>У продавца пока нет объявлений</p>
            ) : (
              <div className="profile-listings-grid">
                {listings.map((listing) => {
                  const listingPhotos = listingsPhotos[listing.listingsId] || [];
                  const firstPhoto = listingPhotos[0];

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
                          <strong>Город:</strong> {listing.city?.title || '—'}
                        </p>
                        <p className="small-muted">
                          <strong>Просмотры:</strong> {listing.viewsCount ?? 0}
                          {' • '}
                          <strong>В избранных:</strong> {listing.likesCount ?? 0}
                        </p>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {activeTab === 'reviews' && (
          <div id="tab-reviews" role="tabpanel" aria-labelledby="tab-button-reviews">
            {reviews.length === 0 ? (
              <p>Пока нет отзывов</p>
            ) : (
              <div>
                {reviews.map((review) => (
                  <div key={review.id} className="card" style={{ marginBottom: '1rem' }}>
                    <div className="card-body">
                      <p><strong>Рейтинг:</strong> {'⭐'.repeat(review.rating)}</p>
                      {review.text && <p>{review.text}</p>}
                      <p className="small-muted">{new Date(review.createdAt).toLocaleDateString()}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
