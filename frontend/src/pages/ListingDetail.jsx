import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { resolveApiUrl } from '../api/axios';
import { photosAPI } from '../api/photos';
import { favouritesAPI } from '../api/favourites';
import { messengerAPI } from '../api/messenger';
import { sellerAPI } from '../api/seller';
import { useAuth } from '../context/AuthContext';
import { AGE_LABELS } from '../util/age';
import { GENDER_LABELS } from '../util/gender';

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
  const [lastResolvedProfile, setLastResolvedProfile] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [similarListings, setSimilarListings] = useState([]);
  const [similarPhotos, setSimilarPhotos] = useState({});
  const [similarLoading, setSimilarLoading] = useState(false);
  const [cityTitle, setCityTitle] = useState('');
  const [metroStation, setMetroStation] = useState(null);
  const [editReviewId, setEditReviewId] = useState(null);
  const [editRating, setEditRating] = useState(5);
  const [editText, setEditText] = useState('');
  const [reviewActionLoading, setReviewActionLoading] = useState(false);

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
      console.log('Listing data:', data);
      if (typeof window !== 'undefined' && window.__DEV_DEBUG) {
        console.debug('DEBUG loadListing response:', data);
      }
      setListing(data);
      setCityTitle(data.city?.title || '');
      if (data.metro) {
        console.log('Metro found:', data.metro);
        setMetroStation(data.metro);
      }
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

  const startEditReview = (review) => {
    setEditReviewId(review.id);
    setEditRating(review.rating);
    setEditText(review.text || '');
  };

  const cancelEditReview = () => {
    setEditReviewId(null);
    setEditRating(5);
    setEditText('');
  };

  const handleUpdateReview = async (e) => {
    e.preventDefault();
    setReviewActionLoading(true);
    try {
      await listingsAPI.updateReview({
        listingId: parseInt(id),
        rating: editRating,
        text: editText || null,
      });
      cancelEditReview();
      loadReviews();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update review');
    } finally {
      setReviewActionLoading(false);
    }
  };

  const handleDeleteReview = async (reviewId) => {
    if (!window.confirm('Удалить отзыв?')) return;
    setReviewActionLoading(true);
    try {
      await listingsAPI.deleteReview(reviewId);
      loadReviews();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete review');
    } finally {
      setReviewActionLoading(false);
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
      if (typeof window !== 'undefined' && window.__DEV_DEBUG) {
        console.debug('DEBUG loadSellerProfile: listing.sellerId=', listing.sellerId, ' -> sellerProfile=', profile);
        try { console.debug('DEBUG currentUser:', JSON.parse(localStorage.getItem('user') || 'null')); } catch(_) {}
      }
      setSellerProfile(profile);
      if (typeof window !== 'undefined' && window.__DEV_DEBUG) {
        setLastResolvedProfile(profile);
      }

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
      const chat = await messengerAPI.createOrGetChat(
        listing.sellerId,
        listing.listingsId,
        listing.title
      );
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
        <div className="card" style={{ marginTop: '1rem', marginBottom: '1rem' }}>
          <div className="card-body" style={{ display: 'flex', alignItems: 'center', gap: '1rem', backgroundColor: 'transparent' }}>
            {avatarUrl && (
              <img
                src={avatarUrl}
                alt="Seller avatar"
                className="avatar-sm"
              />
            )}
            <div style={{ flex: 1 }}>
              {(() => {
                const displayName = sellerProfile?.shopName || 'Продавец';
                const profileLink = sellerProfile?.id
                  ? `/seller/profile/view/${sellerProfile.id}`
                  : `/user/profile/${listing.sellerId}`;

                return (
                  <Link to={profileLink} style={{ textDecoration: 'none', color: '#3498db', fontSize: '1.1rem', fontWeight: 'bold' }}>
                    {displayName}
                  </Link>
                );
              })()}
               {sellerProfile.rating != null && (
                 <p className="small-muted" style={{ margin: '0.25rem 0 0 0' }}>
                   Рейтинг: {sellerProfile.rating.toFixed(2)} / 5
                   {sellerProfile.isVerified && ' ✓ Проверен'}
                 </p>
               )}
             </div>
          </div>
        </div>
      )}
      <p className="small-muted">
        <strong>Рейтинг:</strong>{' '}
        {listing.sellerRating != null ? `${listing.sellerRating.toFixed(2)} / 5` : 'No ratings yet'}
        {listing.sellerReviewsCount != null && ` (${listing.sellerReviewsCount} отзывов)`}
      </p>
      <div className="two-col">
        <div style={{ flex: 1 }}>
          {photos.length > 0 ? (
            <div>
              {photos.map((photoUrl, index) => (
                <img
                  key={index}
                  src={resolveApiUrl(photoUrl)}
                  alt={`Photo ${index + 1}`}
                  className="detail-image"
                />
              ))}
            </div>
          ) : (
            <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div className="card-body">Нет фото</div>
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}>
          <p><strong>Описание:</strong> {listing.description}</p>
          <p className="small-muted">
            <strong>Город:</strong> {cityTitle || 'Не указан'}
            {metroStation && (
              <span style={{ marginLeft: '0.5rem', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                • {metroStation.title}
                <span
                  style={{
                    width: '12px',
                    height: '12px',
                    borderRadius: '50%',
                    backgroundColor: metroStation.line?.color || '#000'
                  }}
                />
              </span>
            )}
          </p>
          <p><strong>Цена:</strong> <span className="tag-price">{listing.price} ₽</span></p>
          <p><strong>Вид:</strong> {listing.species || 'N/A'}</p>
          {listing.breed && <p><strong>Breed:</strong> {listing.breed}</p>}
          <p>
            <strong>Возраст:</strong>{' '}
            {listing.ageMonths != null ? (AGE_LABELS[listing.ageMonths] || 'Не указан') : 'Не указан'}
          </p>
          <p>
            <strong>Пол:</strong>{' '}
            {listing.gender ? (GENDER_LABELS[listing.gender] || 'Любой') : 'Любой'}
          </p>
          {isAuthenticated() && user?.id === listing.sellerId && (
            <div className="link-actions">
              <Link to={`/listings/${id}/edit`} className="btn btn-secondary">Изменить объявление</Link>

              <Link to={`/listings/${id}/photos`} className="btn btn-ghost">Изменить фотографии</Link>

              {/* allow owner to add own listing to favourites as well */}
              <button onClick={handleToggleFavourite} className={isFavourite ? 'btn btn-ghost' : 'btn btn-secondary'}>
                {isFavourite ? 'Удалить из избранных' : 'Добавить в избранное'}
              </button>
            </div>
          )}
          {isAuthenticated() && user?.id !== listing.sellerId && (
            <div className="link-actions">
              <button onClick={handleMessageSeller} className="btn btn-primary">Написать продавцу</button>
              {isAuthenticated() && (
                <button onClick={handleToggleFavourite} className={isFavourite ? 'btn btn-ghost' : 'btn btn-secondary'}>
                  {isFavourite ? 'Удалить из избранных' : 'Добавить в избранное'}
                </button>
              )}
            </div>
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
                {editReviewId === review.id ? (
                  <form onSubmit={handleUpdateReview}>
                    <div style={{ marginBottom: '0.75rem' }}>
                      <label style={{ display: 'block', marginBottom: '0.5rem' }}>Оценка:</label>
                      <select
                        value={editRating}
                        onChange={(e) => setEditRating(parseInt(e.target.value))}
                        style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
                      >
                        <option value={1}>1 ⭐</option>
                        <option value={2}>2 ⭐⭐</option>
                        <option value={3}>3 ⭐⭐⭐</option>
                        <option value={4}>4 ⭐⭐⭐⭐</option>
                        <option value={5}>5 ⭐⭐⭐⭐⭐</option>
                      </select>
                    </div>
                    <div style={{ marginBottom: '0.75rem' }}>
                      <label style={{ display: 'block', marginBottom: '0.5rem' }}>Текст отзыва:</label>
                      <textarea
                        value={editText}
                        onChange={(e) => setEditText(e.target.value)}
                        rows="4"
                        style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
                      />
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        type="submit"
                        disabled={reviewActionLoading}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#3498db',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: reviewActionLoading ? 'not-allowed' : 'pointer'
                        }}
                      >
                        {reviewActionLoading ? 'Сохраняем...' : 'Сохранить'}
                      </button>
                      <button
                        type="button"
                        onClick={cancelEditReview}
                        disabled={reviewActionLoading}
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#95a5a6',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: reviewActionLoading ? 'not-allowed' : 'pointer'
                        }}
                      >
                        Отмена
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p><strong>Оценка:</strong> {'⭐'.repeat(review.rating)}</p>
                    {review.text && <p>{review.text}</p>}
                    <p style={{ fontSize: '0.9rem', color: '#666' }}>
                      {new Date(review.createdAt).toLocaleDateString()}
                    </p>
                    {isAuthenticated() && user?.id === review.authorId && (
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                        <button
                          onClick={() => startEditReview(review)}
                          disabled={reviewActionLoading}
                          style={{
                            padding: '0.4rem 0.75rem',
                            backgroundColor: '#f39c12',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: reviewActionLoading ? 'not-allowed' : 'pointer'
                          }}
                        >
                          Редактировать
                        </button>
                        <button
                          onClick={() => handleDeleteReview(review.id)}
                          disabled={reviewActionLoading}
                          style={{
                            padding: '0.4rem 0.75rem',
                            backgroundColor: '#e74c3c',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: reviewActionLoading ? 'not-allowed' : 'pointer'
                          }}
                        >
                          Удалить
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
      <div style={{ marginTop: '3rem' }}>
        <h3>Похожие объявления</h3>

        {similarLoading && <p>Грузим...</p>}

        {!similarLoading && similarListings.length === 0 && (
          <p>Похожие объявления не найдены</p>
        )}

        <div className="listings-grid">
          {similarListings.map((listing) => {
            const photos = similarPhotos[listing.id] || [];
            const firstPhoto = photos[0];

            return (
              <Link key={listing.id} to={`/listings/${listing.id}`} className="listing-card">
                {firstPhoto ? (
                  <img
                    src={firstPhoto.startsWith('http')
                      ? firstPhoto
                      : resolveApiUrl(firstPhoto)}
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
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
};
