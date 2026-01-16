import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { sellerAPI } from '../api/seller';
import { photosAPI } from '../api/photos';
import { messengerAPI } from '../api/messenger';
import { listingsAPI } from '../api/listings';
import { useAuth } from '../context/AuthContext';

export const SellerProfileView = () => {
  const { sellerId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [listings, setListings] = useState([]);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingListings, setLoadingListings] = useState(false);
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
      const data = await sellerAPI.getSellerProfile(parseInt(sellerId));
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
      const url = photosAPI.getAvatarByUserId(userId);
      setAvatarUrl(url);
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
    } catch (err) {
      console.error('Failed to load listings:', err);
      setListings([]);
    } finally {
      setLoadingListings(false);
    }
  };

  const handleMessageSeller = async () => {
    if (!isAuthenticated()) {
      alert('Please login to message seller');
      return;
    }

    if (!profile?.userId) {
      alert('Seller information not available');
      return;
    }

    if (user?.id === profile.userId) {
      alert('You cannot message yourself');
      return;
    }

    try {
      const chat = await messengerAPI.createOrGetChat(profile.userId);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      console.error('Failed to create chat:', err);
      alert('Failed to start conversation with seller');
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!profile) return <div>Seller profile not found</div>;

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
              alt="Seller avatar" 
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
            <h1 style={{ margin: '0 0 0.5rem 0' }}>{profile.shopName}</h1>
            {profile.description && (
              <p style={{ color: '#666', marginBottom: '0.5rem' }}>{profile.description}</p>
            )}
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
              {profile.rating != null && (
                <p style={{ margin: 0 }}>
                  <strong>Рейтинг:</strong> {profile.rating.toFixed(2)} / 5
                </p>
              )}
              {profile.isVerified && (
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
            {isAuthenticated() && user?.id !== profile.userId && (
              <button
                onClick={handleMessageSeller}
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
                Написать продавцу
              </button>
            )}
          </div>
        </div>
      </div>

      <div style={{ marginTop: '2rem' }}>
        <h2>Объявления продавца</h2>
        {loadingListings ? (
          <p>Загрузка объявлений...</p>
        ) : listings.length === 0 ? (
          <p>У продавца пока нет объявлений</p>
        ) : (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', 
            gap: '1rem',
            marginTop: '1rem'
          }}>
            {listings.map((listing) => (
              <Link
                key={listing.listingsId}
                to={`/listings/${listing.listingsId}`}
                style={{
                  textDecoration: 'none',
                  color: 'inherit',
                  border: '1px solid #ddd',
                  borderRadius: '8px',
                  padding: '1rem',
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
              </Link>
            ))}
          </div>
        )}
      </div>

      <div style={{ marginTop: '2rem' }}>
        <h2>Отзывы</h2>
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
    </div>
  );
};

