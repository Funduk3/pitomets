import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { userAPI } from '../api/user';
import { photosAPI } from '../api/photos';
import { messengerAPI } from '../api/messenger';
import { sellerAPI } from '../api/seller';
import { listingsAPI } from '../api/listings';
import { useAuth } from '../context/AuthContext';

export const UserProfileView = () => {
  const { userId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
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
    return () => {
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
    };
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
      const token = localStorage.getItem('accessToken');
      if (token) {
        const response = await fetch(photosAPI.getAvatarByUserId(userId), {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        if (response.ok) {
          const blob = await response.blob();
          const url = URL.createObjectURL(blob);
          setAvatarUrl(url);
        } else {
          setAvatarUrl(null);
        }
      }
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

    if (user?.id === profile.id) {
      alert('You cannot message yourself');
      return;
    }

    try {
      const chat = await messengerAPI.createOrGetChat(profile.id);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      console.error('Failed to create chat:', err);
      alert('Failed to start conversation');
    }
  };

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!profile) return <div>User profile not found</div>;

  const displayName = profile.isSeller ? profile.shopName : profile.fullName;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ 
        padding: '2rem',
        border: '1px solid #ddd',
        borderRadius: '8px',
        backgroundColor: '#f9f9f9',
        marginBottom: '2rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          {avatarUrl && (
            <img 
              src={avatarUrl} 
              alt="User avatar" 
              style={{ 
                width: '120px', 
                height: '120px', 
                borderRadius: '50%', 
                objectFit: 'cover',
                border: '3px solid #ddd'
              }} 
            />
          )}
          <div style={{ flex: 1 }}>
            <h1 style={{ margin: '0 0 0.5rem 0' }}>{displayName}</h1>
            {profile.isSeller && profile.description && (
              <p style={{ color: '#666', marginBottom: '0.5rem' }}>{profile.description}</p>
            )}
            {!profile.isSeller && (
              <p style={{ color: '#666', marginBottom: '0.5rem' }}>{profile.email}</p>
            )}
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
              {profile.isSeller && profile.rating != null && (
                <p style={{ margin: 0 }}>
                  <strong>Рейтинг:</strong> {profile.rating.toFixed(2)} / 5
                </p>
              )}
              {profile.isSeller && profile.isVerified && (
                <span style={{ 
                  padding: '0.25rem 0.5rem',
                  backgroundColor: '#27ae60',
                  color: 'white',
                  borderRadius: '4px',
                  fontSize: '0.9rem'
                }}>
                  ✓ Проверен
                </span>
              )}
            </div>
            {isAuthenticated() && user?.id !== profile.id && (
              <button
                onClick={handleMessageUser}
                style={{
                  marginTop: '1rem',
                  padding: '0.75rem 1.5rem',
                  backgroundColor: '#27ae60',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '1rem'
                }}
              >
                Написать
              </button>
            )}
          </div>
        </div>
      </div>

      {profile.isSeller && (
        <div style={{ marginTop: '2rem' }}>
          <div style={{ 
            display: 'flex', 
            gap: '1rem', 
            borderBottom: '2px solid #ddd',
            marginBottom: '1rem'
          }}>
            <button
              onClick={() => setActiveTab('listings')}
              style={{
                padding: '0.75rem 1.5rem',
                border: 'none',
                backgroundColor: activeTab === 'listings' ? '#3498db' : 'transparent',
                color: activeTab === 'listings' ? 'white' : '#666',
                cursor: 'pointer',
                fontSize: '1rem',
                fontWeight: activeTab === 'listings' ? 'bold' : 'normal',
                borderBottom: activeTab === 'listings' ? '2px solid #3498db' : '2px solid transparent',
                marginBottom: '-2px'
              }}
            >
              Объявления ({listings.length})
            </button>
            <button
              onClick={() => setActiveTab('reviews')}
              style={{
                padding: '0.75rem 1.5rem',
                border: 'none',
                backgroundColor: activeTab === 'reviews' ? '#3498db' : 'transparent',
                color: activeTab === 'reviews' ? 'white' : '#666',
                cursor: 'pointer',
                fontSize: '1rem',
                fontWeight: activeTab === 'reviews' ? 'bold' : 'normal',
                borderBottom: activeTab === 'reviews' ? '2px solid #3498db' : '2px solid transparent',
                marginBottom: '-2px'
              }}
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
                <div style={{ 
                  display: 'grid', 
                  gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', 
                  gap: '1rem',
                  marginTop: '1rem'
                }}>
                  {listings.map((listing) => {
                    const listingPhotos = listingsPhotos[listing.listingsId] || [];
                    const firstPhoto = listingPhotos[0];
                    
                    return (
                      <Link
                        key={listing.listingsId}
                        to={`/listings/${listing.listingsId}`}
                        style={{
                          textDecoration: 'none',
                          color: 'inherit',
                          border: '1px solid #ddd',
                          borderRadius: '8px',
                          overflow: 'hidden',
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
                          <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.1rem', color: '#3498db' }}>
                            {listing.title}
                          </h3>
                          <p style={{ margin: '0.5rem 0', color: '#666', fontSize: '0.9rem' }}>
                            {listing.description?.substring(0, 100)}
                            {listing.description && listing.description.length > 100 ? '...' : ''}
                          </p>
                          <div style={{ marginTop: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#27ae60' }}>
                              ${listing.price}
                            </span>
                            {listing.species && (
                              <span style={{ fontSize: '0.85rem', color: '#666' }}>
                                {listing.species}
                              </span>
                            )}
                          </div>
                          {listing.ageMonths && (
                            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem', color: '#666' }}>
                              Возраст: {listing.ageMonths} мес.
                            </p>
                          )}
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
                    <div key={review.id} style={{ 
                      border: '1px solid #ddd', 
                      borderRadius: '8px', 
                      padding: '1rem', 
                      marginBottom: '1rem' 
                    }}>
                      <p><strong>Рейтинг:</strong> {'⭐'.repeat(review.rating)}</p>
                      {review.text && <p>{review.text}</p>}
                      <p style={{ fontSize: '0.9rem', color: '#666' }}>
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
        <div style={{ marginTop: '2rem', padding: '2rem', border: '1px solid #ddd', borderRadius: '8px', textAlign: 'center' }}>
          <p>Этот пользователь не является продавцом</p>
        </div>
      )}
    </div>
  );
};

