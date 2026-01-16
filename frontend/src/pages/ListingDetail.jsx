import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { photosAPI } from '../api/photos';
import { favouritesAPI } from '../api/favourites';
import { messengerAPI } from '../api/messenger';
import { sellerAPI } from '../api/seller';
import { useAuth } from '../context/AuthContext';

export const ListingDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [listing, setListing] = useState(null);
  const [photos, setPhotos] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isFavourite, setIsFavourite] = useState(false);
  const [sellerProfile, setSellerProfile] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [similarListings, setSimilarListings] = useState([]);
  const [similarPhotos, setSimilarPhotos] = useState({});
  const [similarLoading, setSimilarLoading] = useState(false);

  useEffect(() => {
    loadListing();
    loadPhotos();
    loadReviews();
    loadFavouriteStatus();
    loadSimilarListings();
  }, [id]);

  useEffect(() => {
    if (listing?.sellerId) {
      loadSellerProfile();
    }
    return () => {
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
    };
  }, [listing]);

  const loadListing = async () => {
    try {
      const data = await listingsAPI.getListing(parseInt(id));
      setListing(data);
    } catch (err) {
      setError('Failed to load listing');
    } finally {
      setLoading(false);
    }
  };

  const loadPhotos = async () => {
    try {
      const data = await photosAPI.getListingPhotos(parseInt(id));
      setPhotos(data.photos || []);
    } catch (err) {
      console.error('Failed to load photos:', err);
    }
  };

  const loadReviews = async () => {
    try {
      const data = await listingsAPI.getListingReviews(parseInt(id));
      setReviews(data);
    } catch (err) {
      console.error('Failed to load reviews:', err);
    }
  };

  const loadFavouriteStatus = async () => {
    if (!isAuthenticated()) {
      setIsFavourite(false);
      return;
    }
    try {
      const favourites = await favouritesAPI.getFavourites();
      const exists = favourites.some((f) => f.id === parseInt(id));
      setIsFavourite(exists);
    } catch (err) {
      console.error('Failed to load favourites status:', err);
      setIsFavourite(false);
    }
  };

  const loadSellerProfile = async () => {
    if (!listing?.sellerId) return;
    try {
      const profile = await sellerAPI.getSellerProfile(listing.sellerId);
      setSellerProfile(profile);
      
      if (profile.userId) {
        loadAvatar(profile.userId);
      }
    } catch (err) {
      console.error('Failed to load seller profile:', err);
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

  const handleToggleFavourite = async () => {
    if (!isAuthenticated()) {
      alert('Please login to add favourites');
      return;
    }

    try {
      if (isFavourite) {
        await favouritesAPI.deleteFavourite(parseInt(id));
        setIsFavourite(false);
      } else {
        await favouritesAPI.addFavourite(parseInt(id));
        setIsFavourite(true);
      }
    } catch (err) {
      const msg = err.response?.data?.message;
      const status = err.response?.status;
      if (!isFavourite && (status === 409 || (msg && msg.toLowerCase().includes('already')))) {
        setIsFavourite(true);
        alert('This listing is already in your favourites.');
        return;
      }
      alert(msg || 'Failed to update favourites');
    }
  };

  const handleMessageSeller = async () => {
    if (!isAuthenticated()) {
      alert('Please login to message seller');
      return;
    }

    if (!listing || !listing.sellerId) {
      alert('Seller information not available');
      return;
    }

    if (user?.id === listing.sellerId) {
      alert('You cannot message yourself');
      return;
    }

    try {
      const chat = await messengerAPI.createOrGetChat(listing.sellerId);
      navigate(`/chats/${chat.id}`);
    } catch (err) {
      console.error('Failed to create chat:', err);
      alert('Failed to start conversation with seller');
    }
  };

  const loadSimilarListings = async () => {
    try {
      setSimilarLoading(true);

      const data = await listingsAPI.getSimilarListings(parseInt(id), 6);
      setSimilarListings(data);

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

      setSimilarPhotos(photosMap);
    } catch (err) {
      console.error('Failed to load similar listings:', err);
    } finally {
      setSimilarLoading(false);
    }
  };

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!listing) return <div>Объявление не найдено</div>;

  return (
    <div>
      <h2>{listing.title || 'Untitled'}</h2>
      {sellerProfile && (
        <div style={{ 
          marginTop: '1rem', 
          marginBottom: '1rem',
          padding: '1rem',
          border: '1px solid #ddd',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          backgroundColor: '#f9f9f9'
        }}>
          {avatarUrl && (
            <img 
              src={avatarUrl} 
              alt="Seller avatar" 
              style={{ 
                width: '60px', 
                height: '60px', 
                borderRadius: '50%', 
                objectFit: 'cover',
                border: '2px solid #ddd'
              }} 
            />
          )}
          <div style={{ flex: 1 }}>
            <Link
              to={`/seller/profile/view/${sellerProfile.userId}`}
              style={{
                textDecoration: 'none',
                color: '#3498db',
                fontSize: '1.1rem',
                fontWeight: 'bold'
              }}
            >
              {sellerProfile.shopName}
            </Link>
            {sellerProfile.rating != null && (
              <p style={{ margin: '0.25rem 0 0 0', color: '#666', fontSize: '0.9rem' }}>
                Рейтинг: {sellerProfile.rating.toFixed(2)} / 5
                {sellerProfile.isVerified && ' ✓ Проверен'}
              </p>
            )}
          </div>
        </div>
      )}
      <p style={{ marginTop: '0.25rem', color: '#555' }}>
        <strong>Рейтинг:</strong>{' '}
        {listing.sellerRating != null ? `${listing.sellerRating.toFixed(2)} / 5` : 'No ratings yet'}
        {listing.sellerReviewsCount != null && ` (${listing.sellerReviewsCount} отзывов)`}
      </p>
      <div style={{ display: 'flex', gap: '2rem', marginTop: '2rem' }}>
        <div style={{ flex: 1 }}>
          {photos.length > 0 ? (
            <div>
              {photos.map((photoUrl, index) => (
                <img
                  key={index}
                  src={photoUrl.startsWith('http') ? photoUrl : `http://localhost:8080${photoUrl}`}
                  alt={`Photo ${index + 1}`}
                  style={{ width: '100%', maxWidth: '500px', marginBottom: '1rem', borderRadius: '8px' }}
                />
              ))}
            </div>
          ) : (
            <div style={{ width: '100%', height: '300px', backgroundColor: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '8px' }}>
              No photos available
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}>
          <p><strong>Описание:</strong> {listing.description}</p>
          <p><strong>Цена:</strong> ${listing.price}</p>
          <p><strong>Вид:</strong> {listing.species || 'N/A'}</p>
          {listing.breed && <p><strong>Breed:</strong> {listing.breed}</p>}
          <p><strong>Возраст:</strong> {listing.ageMonths} months</p>
          {listing.mother && <p><strong>Mother ID:</strong> {listing.mother}</p>}
          {listing.father && <p><strong>Father ID:</strong> {listing.father}</p>}
          {isAuthenticated() && user?.id === listing.sellerId && (
            <Link
              to={`/listings/${id}/photos`}
              style={{
                display: 'inline-block',
                marginTop: '0.75rem',
                padding: '0.5rem 1rem',
                backgroundColor: '#f39c12',
                color: 'white',
                textDecoration: 'none',
                borderRadius: '4px'
              }}
            >
              Изменить фотографии
            </Link>
          )}
          {isAuthenticated() && user?.id !== listing.sellerId && (
            <button
              onClick={handleMessageSeller}
              style={{
                marginTop: '1rem',
                marginRight: '1rem',
                padding: '0.75rem 1.5rem',
                backgroundColor: '#27ae60',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Написать продавцу
            </button>
          )}
          {isAuthenticated() && (
            <button
              onClick={handleToggleFavourite}
              style={{
                marginTop: '1rem',
                padding: '0.75rem 1.5rem',
                backgroundColor: isFavourite ? '#95a5a6' : '#e74c3c',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              {isFavourite ? 'Удалить из избранных' : 'Добавить в избранное'}
            </button>
          )}
        </div>
      </div>

      <div style={{ marginTop: '3rem' }}>
        <h3>Отзывы</h3>
        {reviews.length === 0 ? (
          <p>Пока нет отзывов</p>
        ) : (
          <div>
            {reviews.map((review) => (
              <div key={review.id} style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1rem', marginBottom: '1rem' }}>
                <p><strong>Rating:</strong> {'⭐'.repeat(review.rating)}</p>
                {review.text && <p>{review.text}</p>}
                <p style={{ fontSize: '0.9rem', color: '#666' }}>
                  {new Date(review.createdAt).toLocaleDateString()}
                </p>
              </div>
            ))}
          </div>
        )}
        {isAuthenticated() && (
          <Link
            to={`/listings/${id}/review`}
            style={{
              display: 'inline-block',
              marginTop: '1rem',
              padding: '0.75rem 1.5rem',
              backgroundColor: '#3498db',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
          >
            Написать отзыв
          </Link>
        )}
      </div>
      <div style={{ marginTop: '3rem' }}>
        <h3>Похожие объявления</h3>

        {similarLoading && <p>Грузим...</p>}

        {!similarLoading && similarListings.length === 0 && (
            <p>Похожие объявления не найдены</p>
        )}

        <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))',
              gap: '1.5rem',
              marginTop: '1rem',
            }}
        >
          {similarListings.map((listing) => {
            const photos = similarPhotos[listing.id] || [];
            const firstPhoto = photos[0];

            return (
                <Link
                    key={listing.id}
                    to={`/listings/${listing.id}`}
                    style={{
                      textDecoration: 'none',
                      color: 'inherit',
                      border: '1px solid #ddd',
                      borderRadius: '8px',
                      overflow: 'hidden',
                      backgroundColor: '#fff',
                    }}
                >
                  {firstPhoto ? (
                      <img
                          src={firstPhoto.startsWith('http')
                              ? firstPhoto
                              : `http://localhost:8080${firstPhoto}`}
                          alt={listing.title || 'Untitled'}
                          style={{
                            width: '100%',
                            height: '160px',
                            objectFit: 'cover',
                            display: 'block',
                          }}
                      />
                  ) : (
                      <div
                          style={{
                            height: '160px',
                            backgroundColor: '#f0f0f0',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#999',
                          }}
                      >
                        Нет фото
                      </div>
                  )}

                  <div style={{ padding: '0.75rem' }}>
                    <h4 style={{ margin: '0 0 0.25rem 0' }}>
                      {listing.title || 'Untitled'}
                    </h4>
                    <p style={{ margin: 0, fontWeight: 'bold' }}>
                      ${listing.price} {/* TODO: currently we don’t have price */}
                    </p>
                    <p style={{ margin: '0.25rem 0', fontSize: '0.85rem', color: '#666' }}>
                      {listing.description} {listing.breed && `• ${listing.breed}`}
                    </p>
                  </div>
                </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
};

