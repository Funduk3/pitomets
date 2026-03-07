import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { userAPI } from '../api/user';
import { photosAPI } from '../api/photos';
import { sellerAPI } from '../api/seller';
import { resolveApiUrl } from '../api/axios';
import { listingsAPI } from '../api/listings';
import { useAuth } from '../context/AuthContext';
import { messengerAPI } from '../api/messenger';
import { GENDER_LABELS } from '../util/gender';
import { AGE_LABELS } from '../util/age';

export const UserProfileView = () => {
  const { userId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
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
    if (userId) {
      loadProfile();
    }
  }, [userId]);

  useEffect(() => {
    if (profile?.avatarKey || profile?.id) {
      loadAvatar(profile.id);
    }
  }, [profile]);

  useEffect(() => {
    if (profile?.isSeller && profile?.id) {
      loadReviews();
      loadListings();
    }
  }, [profile]);

  const loadProfile = async () => {
    try {
      const data = await userAPI.getUserProfile(parseInt(userId));
      setProfile(data);
    } catch (err) {
      setError('Failed to load user profile');
      console.error('Failed to load profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadReviews = async () => {
    try {
      if (profile?.isSeller) {
        // Нужно получить sellerProfileId из sellerAPI
        const sellerProfile = await sellerAPI.getSellerProfile(parseInt(userId));
        if (sellerProfile?.id) {
          const data = await sellerAPI.getSellerReviews(sellerProfile.id);
          setReviews(data);
        }
      }
    } catch (err) {
      console.error('Failed to load reviews:', err);
      setReviews([]);
    }
  };

  const loadListings = async () => {
    if (!profile?.isSeller) return;
    setLoadingListings(true);
    try {
      const data = await listingsAPI.getSellerListings(parseInt(userId));
      const activeListings = data.filter(listing => !listing.isArchived);
      setListings(activeListings);
      
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

  const loadAvatar = async (userId) => {
    try {
      const data = await photosAPI.getAvatarByUserId(userId);
      setAvatarUrl(data?.url || null);
    } catch (err) {
      console.error('Failed to load avatar:', err);
      setAvatarUrl(null);
    }
  };

  const handleMessageUser = async () => {
    if (!isAuthenticated()) {
      alert('Please login to message user');
      return;
    }

    if (!profile?.id) {
      alert('User information not available');
      return;
    }

    try {
      const chat = await messengerAPI.createOrGetChat(profile.id, null, null);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      alert('Failed to create chat');
    }
  };

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!profile) return <div>User profile not found</div>;

  const displayName = profile.isSeller ? profile.shopName : profile.fullName;

  return (
    <div className="container">
      <div className="card" style={{ padding: 0, marginBottom: '2rem' }}>
        <div style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
            {avatarUrl && (
              <img
                src={avatarUrl}
                alt="User avatar"
                className="avatar-md"
              />
            )}
            <div style={{ flex: 1 }}>
              <h1 style={{ margin: '0 0 0.5rem 0' }}>{displayName}</h1>
              {profile.isSeller && profile.description && (
                <p className="small-muted" style={{ marginBottom: '0.5rem' }}>{profile.description}</p>
              )}
              {!profile.isSeller && (
                <p className="small-muted" style={{ marginBottom: '0.5rem' }}>{profile.email}</p>
              )}
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
                {profile.isSeller && profile.rating != null && (
                  <p style={{ margin: 0 }}>
                    <strong>Рейтинг:</strong> {profile.rating.toFixed(2)} / 5
                  </p>
                )}
                {profile.isSeller && profile.isVerified && (
                  <span className="btn btn-primary" style={{ padding: '0.25rem 0.5rem', borderRadius: '6px', backgroundColor: '#FF6B5A' }}>
                    ✓ Проверен
                  </span>
                )}
              </div>
              {isAuthenticated() && user?.id !== profile.id && (
                <button
                  onClick={handleMessageUser}
                  className="btn btn-primary"
                  style={{ marginTop: '1rem' }}
                >
                  Написать
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {profile.isSeller && (
        <div style={{ marginTop: '2rem' }}>
          <div className="tabs">
            <button
              onClick={() => setActiveTab('listings')}
              className={`tab ${activeTab === 'listings' ? 'active' : ''}`}
            >
              Объявления ({listings.length})
            </button>
            <button
              onClick={() => setActiveTab('reviews')}
              className={`tab ${activeTab === 'reviews' ? 'active' : ''}`}
            >
              Отзывы ({reviews.length})
            </button>
          </div>

          {activeTab === 'listings' && (
            <div>
              {loadingListings ? (
                <p>Загрузка объявлений...</p>
              ) : listings.length === 0 ? (
                <p>У пользователя пока нет объявлений</p>
              ) : (
                <div className="profile-listings-grid">
                  {listings.map((listing) => {
                    const listingPhotos = listingsPhotos[listing.listingsId] || [];
                    const firstPhoto = listingPhotos[0];

                    return (
                      <Link
                        key={listing.listingsId}
                        to={`/listings/${listing.listingsId}`}
                        className="link-card"
                      >
                        {firstPhoto ? (
                          <img
                            src={resolveApiUrl(firstPhoto)}
                            alt={listing.title}
                            style={{ height: '200px', objectFit: 'cover', display: 'block' }}
                          />
                        ) : (
                          <div className="listing-placeholder">
                            Нет фото
                          </div>
                        )}
                        <div className="card-body">
                          <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.1rem', color: '#3498db' }}>
                            {listing.title}
                          </h3>
                          <p className="small-muted" style={{ margin: '0.5rem 0', fontSize: '0.9rem' }}>
                            {listing.description?.substring(0, 100)}
                            {listing.description && listing.description.length > 100 ? '...' : ''}
                          </p>
                          <div style={{ marginTop: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span className="tag-price">
                              {listing.price} ₽
                            </span>
                            {listing.species && (
                              <span className="small-muted">
                                {listing.species}
                              </span>
                            )}
                          </div>
                          {listing.ageMonths != null && (
                            <p className="small-muted" style={{ margin: '0.25rem 0 0 0' }}>
                              Возраст: {AGE_LABELS[listing.ageMonths] || 'Не указан'}
                            </p>
                          )}
                          {listing.gender && (
                            <p className="small-muted" style={{ margin: '0.25rem 0 0 0' }}>
                              Пол: {GENDER_LABELS[listing.gender] || 'Любой'}
                            </p>
                          )}
                          <p className="small-muted" style={{ margin: '0.25rem 0 0 0' }}>
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
            <div>
              {reviews.length === 0 ? (
                <p>Пока нет отзывов</p>
              ) : (
                <div>
                  {reviews.map((review) => (
                    <div key={review.id} className="card" style={{ padding: '1rem', marginBottom: '1rem' }}>
                      <p><strong>Рейтинг:</strong> {'⭐'.repeat(review.rating)}</p>
                      {review.text && <p>{review.text}</p>}
                      <p className="small-muted">
                        {new Date(review.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {!profile.isSeller && (
        <div className="card" style={{ marginTop: '2rem', padding: '2rem', textAlign: 'center' }}>
          <p>Этот пользователь не является продавцом</p>
        </div>
      )}
    </div>
  );
};
