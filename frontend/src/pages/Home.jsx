import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { listingsAPI } from '../api/listings';
import { resolveApiUrl } from '../api/axios';

export const Home = () => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState([]);
  const [cursor, setCursor] = useState(null);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadPage(null);
  }, []);

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
      <h1>Питомец</h1>
      <p>Объявления тут!</p>
      {!isAuthenticated() && (
        <div className="hero" style={{ marginTop: '2rem' }}>
          <Link
            to="/register"
            className="btn btn-primary"
            style={{ marginRight: '1rem' }}
          >
            Начать
          </Link>
          <Link
            to="/search"
            className="btn btn-secondary"
          >
            Поиск по объявлениям
          </Link>
        </div>
      )}

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
                <div key={listing.listingsId} className="listing-card">
                  {listing.coverPhotoId ? (
                    <img
                      src={resolveApiUrl(`/listings/${listing.listingsId}/photos/${listing.coverPhotoId}`)}
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
                  <Link
                    to={`/listings/${listing.listingsId}`}
                    className="btn btn-secondary"
                    style={{ display: 'inline-block', marginTop: '0.5rem', fontSize: '0.9rem' }}
                  >
                    Посмотреть
                  </Link>
                  </div>
                </div>
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
