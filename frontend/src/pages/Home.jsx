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
        <div style={{ marginTop: '2rem' }}>
          <Link
            to="/register"
            style={{
              display: 'inline-block',
              padding: '1rem 2rem',
              backgroundColor: '#3498db',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px',
              marginRight: '1rem'
            }}
          >
            Начать
          </Link>
          <Link
            to="/search"
            style={{
              display: 'inline-block',
              padding: '1rem 2rem',
              backgroundColor: '#27ae60',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
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
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                gap: '1.25rem',
                marginTop: '1rem'
              }}
            >
              {items.map((listing) => (
                <div
                  key={listing.listingsId}
                  style={{
                    border: '1px solid #ddd',
                    borderRadius: '8px',
                    overflow: 'hidden'
                  }}
                >
                  {listing.coverPhotoId ? (
                    <img
                      src={resolveApiUrl(`/listings/${listing.listingsId}/photos/${listing.coverPhotoId}`)}
                      alt="Listing cover"
                      style={{
                        width: '100%',
                        height: '180px',
                        objectFit: 'cover',
                        display: 'block'
                      }}
                    />
                  ) : (
                    <div
                      style={{
                        width: '100%',
                        height: '180px',
                        backgroundColor: '#f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#999'
                      }}
                    >
                      Нет фото
                    </div>
                  )}
                  <div style={{ padding: '1rem' }}>
                    <h3 style={{ margin: '0 0 0.5rem 0' }}>
                      {listing.title || 'Без названия'}
                    </h3>
                  <p style={{ margin: '0.5rem 0', color: '#666' }}>
                    {listing.description?.substring(0, 90)}
                    {listing.description && listing.description.length > 90
                      ? '...'
                      : ''}
                  </p>
                  <p style={{ margin: '0.25rem 0' }}>
                    <strong>Цена:</strong> {listing.price} ₽
                  </p>
                  <p style={{ margin: '0.25rem 0' }}>
                    <strong>Город:</strong> {listing.city?.title || '—'}
                  </p>
                  <Link
                    to={`/listings/${listing.listingsId}`}
                    style={{
                      display: 'inline-block',
                      marginTop: '0.5rem',
                      padding: '0.5rem 0.75rem',
                      backgroundColor: '#3498db',
                      color: 'white',
                      textDecoration: 'none',
                      borderRadius: '4px',
                      fontSize: '0.9rem'
                    }}
                  >
                    Посмотреть
                  </Link>
                  </div>
                </div>
              ))}
            </div>
            <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
              <button
                onClick={() => {
                  if (history.length === 0) return;
                  const prev = history[history.length - 1];
                  setHistory(history.slice(0, -1));
                  loadPage(prev);
                }}
                disabled={history.length === 0 || loading}
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
