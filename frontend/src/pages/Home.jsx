import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { listingsAPI } from '../api/listings';
import { photosAPI } from '../api/photos';
import { resolveApiUrl } from '../api/axios';

export const Home = () => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState([]);
  const [cursor, setCursor] = useState(null);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [history, setHistory] = useState([]);
  const [listingPhotos, setListingPhotos] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadPage(null);
  }, []);

  useEffect(() => {
    if (!items.length) {
      setListingPhotos({});
      return;
    }

    let cancelled = false;
    (async () => {
      const results = await Promise.all(
        items.map(async (listing) => {
          try {
            const data = await photosAPI.getListingPhotos(listing.listingsId);
            return { id: listing.listingsId, photo: data?.photos?.[0] || null };
          } catch (_) {
            return { id: listing.listingsId, photo: null };
          }
        })
      );

      if (cancelled) return;
      const next = {};
      results.forEach(({ id, photo }) => { next[id] = photo; });
      setListingPhotos(next);
    })();

    return () => { cancelled = true; };
  }, [items]);

  const typeButtons = [
    { label: 'Собаки', icon: '🐶' },
    { label: 'Кошки', icon: '🐱' },
    { label: 'Птицы', icon: '🦜' },
    { label: 'Грызуны', icon: '🐹' },
    { label: 'Хорьки / Экзоты', mobileLabel: 'Хорьки Экзоты', icon: '🦦' },
    { label: 'Аквариум', icon: '🐠' },
    { label: 'Рептилии', icon: '🦎' },
    { label: 'Скот / Фермерские', mobileLabel: 'Скот Фермерские', icon: '🐄' },
    { label: 'Другое', icon: '🐾' },
  ];

  const loadPage = async (nextCursorValue) => {
    try {
      setLoading(true);
      setError('');
      const data = await listingsAPI.getHomeListings(nextCursorValue);
      setItems(data.items || []);
      setNextCursor(data.nextCursor ?? null);
      setHasMore(Boolean(data.hasMore));
      setCursor(nextCursorValue ?? null);
    } catch (err) {
      console.error('Failed to load home listings:', err);
      setError('Не удалось загрузить объявления');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div
        className="home-category-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
          gap: '0.75rem',
          marginBottom: '2rem'
        }}
      >
        {typeButtons.map(({ label, mobileLabel, icon }) => (
          <Link
            key={label}
            to={`/search?types=${encodeURIComponent(label)}`}
            className="btn btn-secondary home-category-button"
            style={{
              width: '100%',
              textAlign: 'center',
              padding: '0.9rem 1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem'
            }}
          >
            <span className="home-category-icon" style={{ fontSize: '1.25rem' }} aria-hidden="true">{icon}</span>
            <span className="home-category-label home-category-label-desktop">{label}</span>
            <span className="home-category-label home-category-label-mobile">{mobileLabel || label}</span>
          </Link>
        ))}
      </div>

      <div style={{ marginTop: '2rem' }}>
        <h2>Свежие объявления</h2>
        {loading && <div>Грузим...</div>}
        {error && (
          <div style={{ color: 'red', marginBottom: '1rem' }}>
            {error}
            <button
              onClick={() => loadPage(cursor)}
              style={{ marginLeft: '1rem' }}
            >
              Повторить
            </button>
          </div>
        )}
        {!loading && !error && items.length === 0 && (
          <div>Пока нет объявлений.</div>
        )}
        {!loading && !error && items.length > 0 && (
          <div>
            <div className="listings-grid">
              {items.map((listing) => (
                <Link key={listing.listingsId} to={`/listings/${listing.listingsId}`} className="listing-card">
                  {listingPhotos[listing.listingsId] ? (
                    <img
                      src={resolveApiUrl(listingPhotos[listing.listingsId])}
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
                    {listing.description && listing.description.length > 90
                      ? '...'
                      : ''}
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
              ))}
            </div>
            <div className="pager-controls">
              <button
                onClick={() => {
                  if (history.length === 0) return;
                  const prev = history[history.length - 1];
                  setHistory(history.slice(0, -1));
                  loadPage(prev);
                }}
                disabled={history.length === 0 || loading}
                className="btn"
              >
                Назад
              </button>
              <button
                onClick={() => {
                  if (!hasMore || !nextCursor) return;
                  setHistory([...history, cursor]);
                  loadPage(nextCursor);
                }}
                disabled={loading || !hasMore || !nextCursor}
                className="btn"
              >
                Вперед
              </button>
              <div style={{ marginLeft: '0.5rem', color: '#666' }}>
                Стр. {history.length + 1}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
