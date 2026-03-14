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
  const [photos, setPhotos] = useState([]);
  const [listingPhotos, setListingPhotos] = useState({});
  const [sellerAvatars, setSellerAvatars] = useState({});
  const [messages, setMessages] = useState({});

  const loadAll = async () => {
    try {
      setLoading(true);
      setError('');
      const [listingsData, reviewsData, sellerProfilesData, photosData] = await Promise.all([
        adminAPI.getPendingListings(),
        adminAPI.getPendingReviews(),
        adminAPI.getPendingSellerProfiles(),
        adminAPI.getPendingPhotos(),
      ]);
      setListings(listingsData);
      setReviews(reviewsData);
      setSellerProfiles(sellerProfilesData);
      setPhotos(photosData);

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
          try {
            const data = await photosAPI.getAvatarByUserId(profile.userId);
            return { userId: profile.userId, url: data?.url || null };
          } catch (_) {
            return { userId: profile.userId, url: null };
          }
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

  const localizeModerationReason = (reason) => {
    if (!reason) {
      return '';
    }

    const normalized = reason.toLowerCase();
    if (normalized.includes('moderium_api_token is empty')) {
      return 'Токен MODERIUM API не настроен';
    }
    if (normalized.includes('no text parts for moderation')) {
      return 'Отсутствует текст для модерации';
    }
    if (normalized.includes('unexpected moderation error')) {
      return 'Неожиданная ошибка модерации';
    }
    if (normalized.includes('toxicity >= 0.8 or explicit profanity detected')) {
      return 'Токсичность >= 0.8 или обнаружена явная нецензурная лексика';
    }
    if (
      normalized.includes('i/o error') ||
      normalized.includes('connection refused') ||
      normalized.includes('connect timed out') ||
      normalized.includes('read timed out')
    ) {
      return 'Сервис MODERIUM временно недоступен';
    }
    return reason;
  };

  const renderModerationHint = (hint) => {
    if (!hint) {
      return null;
    }

    const statusLabel = {
      APPROVED: 'Оценка ИИ: одобрить',
      REJECTED: 'Оценка ИИ: отклонить',
      REVIEW: 'Оценка ИИ: ручная проверка',
      ERROR: 'Оценка ИИ: ошибка анализа',
    }[hint.status] || 'Оценка ИИ';

    const statusColor = {
      APPROVED: '#0f766e',
      REJECTED: '#b91c1c',
      REVIEW: '#92400e',
      ERROR: '#7c3aed',
    }[hint.status] || '#374151';

    const hasToxicity = typeof hint.toxicityScore === 'number';
    const toxicityValue = hasToxicity ? Math.max(0, Math.min(1, hint.toxicityScore)) : null;
    const toxicityText = hasToxicity ? toxicityValue.toFixed(2) : 'н/д';
    const profanityDetected = hint.profanityDetected;
    const sexualContentDetected = hint.sexualContentDetected;
    const hasBadContent = profanityDetected === true || sexualContentDetected === true;

    const toxicityTone = (!hasToxicity && !hasBadContent)
      ? { label: 'Нет данных', color: '#6b7280', bg: '#f3f4f6' }
      : hasBadContent
        ? { label: 'Высокий риск', color: '#b91c1c', bg: '#fee2e2' }
        : toxicityValue < 0.4
          ? { label: 'Низкий риск', color: '#065f46', bg: '#d1fae5' }
          : toxicityValue < 0.7
            ? { label: 'Средний риск', color: '#92400e', bg: '#fef3c7' }
            : { label: 'Высокий риск', color: '#b91c1c', bg: '#fee2e2' };

    const formatDetected = (value) => {
      if (value === true) return 'Да';
      if (value === false) return 'Нет';
      return 'н/д';
    };

    return (
      <div style={{ marginTop: '0.6rem', padding: '0.55rem 0.7rem', borderRadius: '8px', background: '#f8fafc', border: `1px solid ${statusColor}33` }}>
        <div style={{ fontSize: '0.85rem', color: statusColor, fontWeight: 600 }}>
          {statusLabel}
        </div>
        <div style={{ marginTop: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.45rem', flexWrap: 'wrap' }}>
          <div style={{ fontSize: '0.85rem', color: '#374151' }}>
            Уровень токсичности: <strong>{toxicityText}</strong>
          </div>
          <div style={{ fontSize: '0.85rem', color: '#374151' }}>
            Мат: <strong>{formatDetected(profanityDetected)}</strong>
          </div>
          <div style={{ fontSize: '0.85rem', color: '#374151' }}>
            18+: <strong>{formatDetected(sexualContentDetected)}</strong>
          </div>
          <span
            style={{
              fontSize: '0.74rem',
              fontWeight: 600,
              color: toxicityTone.color,
              background: toxicityTone.bg,
              border: `1px solid ${toxicityTone.color}33`,
              borderRadius: '999px',
              padding: '0.12rem 0.45rem',
            }}
          >
            {toxicityTone.label}
          </span>
        </div>
        {hasToxicity && (
          <div style={{ marginTop: '0.35rem', height: '7px', width: '100%', borderRadius: '999px', background: '#e5e7eb', overflow: 'hidden' }}>
            <div
              style={{
                width: `${Math.round(toxicityValue * 100)}%`,
                height: '100%',
                background: toxicityTone.color,
                transition: 'width 180ms ease',
              }}
            />
          </div>
        )}
        {hint.reason && (
          <div style={{ marginTop: '0.2rem', fontSize: '0.82rem', color: '#4b5563' }}>
            Причина: {localizeModerationReason(hint.reason)}
          </div>
        )}
      </div>
    );
  };

  const renderPhotoModerationHint = (hint) => {
    if (!hint) {
      return null;
    }

    const statusLabel = {
      APPROVED: 'Оценка ИИ: одобрить',
      REJECTED: 'Оценка ИИ: отклонить',
      REVIEW: 'Оценка ИИ: ручная проверка',
      ERROR: 'Оценка ИИ: ошибка анализа',
    }[hint.status] || 'Оценка ИИ';

    const statusColor = {
      APPROVED: '#0f766e',
      REJECTED: '#b91c1c',
      REVIEW: '#92400e',
      ERROR: '#7c3aed',
    }[hint.status] || '#374151';

    const hasScore = typeof hint.toxicityScore === 'number';
    const scoreValue = hasScore ? Math.max(0, Math.min(1, hint.toxicityScore)) : null;
    const scoreText = hasScore ? scoreValue.toFixed(2) : 'н/д';

    const labels = Array.isArray(hint.labels) ? hint.labels : [];
    const labelText = labels.length ? labels.join(', ') : 'н/д';

    return (
      <div style={{ marginTop: '0.6rem', padding: '0.55rem 0.7rem', borderRadius: '8px', background: '#f8fafc', border: `1px solid ${statusColor}33` }}>
        <div style={{ fontSize: '0.85rem', color: statusColor, fontWeight: 600 }}>
          {statusLabel}
        </div>
        <div style={{ marginTop: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.45rem', flexWrap: 'wrap' }}>
          <div style={{ fontSize: '0.85rem', color: '#374151' }}>
            NSFW score: <strong>{scoreText}</strong>
          </div>
          <div style={{ fontSize: '0.85rem', color: '#374151' }}>
            Метки: <strong>{labelText}</strong>
          </div>
        </div>
        {hint.reason && (
          <div style={{ marginTop: '0.2rem', fontSize: '0.82rem', color: '#4b5563' }}>
            Причина: {localizeModerationReason(hint.reason)}
          </div>
        )}
      </div>
    );
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
                    {renderModerationHint(listing.moderationHint)}
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
                    {renderModerationHint(profile.moderationHint)}
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
                {renderModerationHint(review.moderationHint)}
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

      <section style={{ marginTop: '2rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Фотографии на модерации</h3>
        {photos.length === 0 ? (
          <div style={{ color: '#666' }}>Нет фотографий на модерации</div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem' }}>
            {photos.map((photo) => (
              <div key={`${photo.entityType}-${photo.entityId}-${photo.photoUri}`} style={{ border: '1px solid #eee', borderRadius: '8px', padding: '1rem' }}>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  <img
                    src={resolveApiUrl(photo.photoUrl)}
                    alt="Moderation photo"
                    style={{ width: '140px', height: '105px', objectFit: 'cover', borderRadius: '6px' }}
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600 }}>
                      {photo.entityType === 'LISTING' ? 'Объявление' : 'Профиль пользователя'} #{photo.entityId}
                    </div>
                    <div style={{ color: '#666', marginTop: '0.25rem' }}>{photo.photoUri}</div>
                    {renderPhotoModerationHint(photo.moderationHint)}
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
