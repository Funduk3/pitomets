import { useEffect, useState } from 'react';
import { adminAPI } from '../api/admin';
import { photosAPI } from '../api/photos';
import { resolveApiUrl } from '../api/axios';

export const Moderation = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [listings, setListings] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [sellerProfiles, setSellerProfiles] = useState([]);
  const [listingPhotos, setListingPhotos] = useState({});
  const [sellerAvatars, setSellerAvatars] = useState({});
  const [messages, setMessages] = useState({});

  const loadAll = async () => {
    try {
      setLoading(true);
      setError('');
      const [listingsData, reviewsData, sellerProfilesData] = await Promise.all([
        adminAPI.getPendingListings(),
        adminAPI.getPendingReviews(),
        adminAPI.getPendingSellerProfiles(),
      ]);
      setListings(listingsData);
      setReviews(reviewsData);
      setSellerProfiles(sellerProfilesData);

      // preload listing photos
      const listingPhotosPromises = listingsData.map(async (listing) => {
        try {
          const photosData = await photosAPI.getListingPhotos(listing.listingsId);
          const first = (photosData.photos || [])[0] || null;
          return { listingId: listing.listingsId, photo: first };
        } catch (err) {
          return { listingId: listing.listingsId, photo: null };
        }
      });
      const listingPhotosResults = await Promise.all(listingPhotosPromises);
      const listingPhotosMap = {};
      listingPhotosResults.forEach(({ listingId, photo }) => {
        listingPhotosMap[listingId] = photo;
      });
      setListingPhotos(listingPhotosMap);

      // preload seller avatars
      const avatarPromises = sellerProfilesData
        .filter((profile) => profile.userId != null)
        .map(async (profile) => {
          const url = photosAPI.getAvatarByUserId(profile.userId);
          return { userId: profile.userId, url };
        });
      const avatarResults = await Promise.all(avatarPromises);
      const avatarMap = {};
      avatarResults.forEach(({ userId, url }) => {
        avatarMap[userId] = url;
      });
      setSellerAvatars(avatarMap);
    } catch (err) {
      console.error('Failed to load moderation data:', err);
      setError('Не удалось загрузить данные модерации');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const updateMessage = (key, value) => {
    setMessages((prev) => ({ ...prev, [key]: value }));
  };

  const getMessage = (key) => messages[key] || '';

  const handleApproveListing = async (id) => {
    await adminAPI.approveListing(id);
    setListings((prev) => prev.filter((l) => l.listingsId !== id));
  };

  const handleDeclineListing = async (id) => {
    const message = getMessage(`listing-${id}`).trim();
    if (!message) {
      alert('Укажите сообщение модератора для объявления');
      return;
    }
    await adminAPI.declineListing(id, message);
    setListings((prev) => prev.filter((l) => l.listingsId !== id));
  };

  const handleApproveReview = async (id) => {
    await adminAPI.approveReview(id);
    setReviews((prev) => prev.filter((r) => r.id !== id));
  };

  const handleDeclineReview = async (id) => {
    const message = getMessage(`review-${id}`).trim();
    await adminAPI.declineReview(id, message);
    setReviews((prev) => prev.filter((r) => r.id !== id));
  };

  const handleApproveSellerProfile = async (id) => {
    await adminAPI.approveSellerProfile(id);
    setSellerProfiles((prev) => prev.filter((p) => p.id !== id));
  };

  const handleDeclineSellerProfile = async (id) => {
    const message = getMessage(`seller-profile-${id}`).trim();
    if (!message) {
      alert('Укажите сообщение модератора для профиля продавца');
      return;
    }
    await adminAPI.declineSellerProfile(id, message);
    setSellerProfiles((prev) => prev.filter((p) => p.id !== id));
  };

  if (loading) {
    return <div>Грузим...</div>;
  }

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '1rem' }}>Модерация</h2>
      {error && (
        <div style={{ color: '#b00020', marginBottom: '1rem' }}>
          {error}
          <button onClick={loadAll} style={{ marginLeft: '1rem' }}>Повторить</button>
        </div>
      )}

      <section style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Объявления на модерации</h3>
        {listings.length === 0 ? (
          <div style={{ color: '#666' }}>Нет объявлений на модерации</div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem' }}>
            {listings.map((listing) => (
              <div key={listing.listingsId} style={{ border: '1px solid #eee', borderRadius: '8px', padding: '1rem' }}>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  {listingPhotos[listing.listingsId] ? (
                    <img
                      src={resolveApiUrl(listingPhotos[listing.listingsId])}
                      alt="Listing cover"
                      style={{ width: '120px', height: '90px', objectFit: 'cover', borderRadius: '6px' }}
                    />
                  ) : (
                    <div style={{ width: '120px', height: '90px', background: '#f5f5f5', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                      Нет фото
                    </div>
                  )}
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>{listing.title}</div>
                    <div style={{ color: '#666', marginTop: '0.25rem' }}>{listing.description}</div>
                    <div style={{ marginTop: '0.5rem' }}>
                      <strong>Цена:</strong> {listing.price} ₽
                    </div>
                  </div>
                </div>
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <button className="btn btn-primary" onClick={() => handleApproveListing(listing.listingsId)}>Одобрить</button>
                  <div style={{ display: 'flex', gap: '0.5rem', flex: 1, minWidth: '260px' }}>
                    <input
                      type="text"
                      placeholder="Причина отклонения"
                      value={getMessage(`listing-${listing.listingsId}`)}
                      onChange={(e) => updateMessage(`listing-${listing.listingsId}`, e.target.value)}
                      style={{ flex: 1, padding: '0.5rem' }}
                    />
                    <button className="btn btn-danger" onClick={() => handleDeclineListing(listing.listingsId)}>Отклонить</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Профили продавцов на модерации</h3>
        {sellerProfiles.length === 0 ? (
          <div style={{ color: '#666' }}>Нет профилей продавцов на модерации</div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem' }}>
            {sellerProfiles.map((profile) => (
              <div key={profile.id} style={{ border: '1px solid #eee', borderRadius: '8px', padding: '1rem' }}>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  {profile.userId && sellerAvatars[profile.userId] ? (
                    <img
                      src={sellerAvatars[profile.userId]}
                      alt="Seller avatar"
                      style={{ width: '72px', height: '72px', borderRadius: '50%', objectFit: 'cover' }}
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                      }}
                    />
                  ) : (
                    <div style={{ width: '72px', height: '72px', borderRadius: '50%', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
                      Нет фото
                    </div>
                  )}
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>{profile.shopName || 'Без названия'}</div>
                    {profile.description && (
                      <div style={{ color: '#666', marginTop: '0.25rem' }}>{profile.description}</div>
                    )}
                    {profile.userId && (
                      <div style={{ marginTop: '0.25rem', color: '#999' }}>User ID: {profile.userId}</div>
                    )}
                  </div>
                </div>
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <button className="btn btn-primary" onClick={() => handleApproveSellerProfile(profile.id)}>Одобрить</button>
                  <div style={{ display: 'flex', gap: '0.5rem', flex: 1, minWidth: '260px' }}>
                    <input
                      type="text"
                      placeholder="Причина отклонения"
                      value={getMessage(`seller-profile-${profile.id}`)}
                      onChange={(e) => updateMessage(`seller-profile-${profile.id}`, e.target.value)}
                      style={{ flex: 1, padding: '0.5rem' }}
                    />
                    <button className="btn btn-danger" onClick={() => handleDeclineSellerProfile(profile.id)}>Отклонить</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h3 style={{ marginBottom: '0.75rem' }}>Отзывы на модерации</h3>
        {reviews.length === 0 ? (
          <div style={{ color: '#666' }}>Нет отзывов на модерации</div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem' }}>
            {reviews.map((review) => (
              <div key={review.id} style={{ border: '1px solid #eee', borderRadius: '8px', padding: '1rem' }}>
                <div><strong>Оценка:</strong> {review.rating}</div>
                <div style={{ color: '#666', marginTop: '0.25rem' }}>{review.text || 'Без текста'}</div>
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <button className="btn btn-primary" onClick={() => handleApproveReview(review.id)}>Одобрить</button>
                  <div style={{ display: 'flex', gap: '0.5rem', flex: 1, minWidth: '260px' }}>
                    <input
                      type="text"
                      placeholder="Причина отклонения"
                      value={getMessage(`review-${review.id}`)}
                      onChange={(e) => updateMessage(`review-${review.id}`, e.target.value)}
                      style={{ flex: 1, padding: '0.5rem' }}
                    />
                    <button className="btn btn-danger" onClick={() => handleDeclineReview(review.id)}>Отклонить</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
