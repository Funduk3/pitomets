import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';
import { searchAPI } from '../api/search';

export const Layout = ({ children }) => {
  const { isAuthenticated, logout, user, loading } = useAuth();
  const { hasUnread } = useMessengerWS();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isSeller = Boolean(user?.shopName);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSuggestions([]);
      return;
    }

    const timeout = setTimeout(async () => {
      try {
        const data = await searchAPI.autocomplete(searchQuery);
        if (Array.isArray(data)) {
          setSuggestions(data);
          setShowSuggestions(true);
        }
      } catch (err) {
        setSuggestions([]);
      }
    }, 250);

    return () => clearTimeout(timeout);
  }, [searchQuery]);

  const submitSearch = (value) => {
    const q = (value ?? searchQuery).trim();
    if (!q) return;
    setShowSuggestions(false);
    navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <nav style={{
        backgroundColor: '#2c3e50',
        padding: '1rem',
        color: 'white',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <Link to="/" style={{ color: 'white', textDecoration: 'none', fontSize: '1.5rem', fontWeight: 'bold' }}>
            Питомец
          </Link>
          <div style={{ position: 'relative', minWidth: '260px' }}>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                submitSearch();
              }}
              style={{ display: 'flex', alignItems: 'center' }}
            >
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => suggestions.length && setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                placeholder="Ищем питомца..."
                style={{
                  width: '220px',
                  padding: '0.5rem 0.75rem',
                  borderRadius: '4px',
                  border: '1px solid #1f2d3a'
                }}
              />
              <button
                type="submit"
                style={{
                  marginLeft: '0.5rem',
                  padding: '0.45rem 0.75rem',
                  backgroundColor: '#3498db',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Найти
              </button>
            </form>
            {showSuggestions && suggestions.length > 0 && (
              <div style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                background: 'white',
                border: '1px solid #ddd',
                borderRadius: '4px',
                zIndex: 20,
                color: '#2c3e50'
              }}>
                {suggestions.map((s, idx) => (
                  <div
                    key={idx}
                    onMouseDown={() => {
                      setSearchQuery(s.title);
                      submitSearch(s.title);
                    }}
                    style={{
                      padding: '0.5rem 0.75rem',
                      cursor: 'pointer',
                      borderBottom: '1px solid #eee',
                      color: '#2c3e50'
                    }}
                  >
                    {s.title}
                  </div>
                ))}
              </div>
            )}
          </div>
          {isAuthenticated() && (
            <>
              {isSeller && (
                <>
                  <Link to="/listings" style={{ color: 'white', textDecoration: 'none' }}>Мои объявления</Link>
                  <Link
                    to="/listings/create"
                    style={{
                      color: 'white',
                      textDecoration: 'none',
                      backgroundColor: '#27ae60',
                      padding: '0.35rem 0.6rem',
                      borderRadius: '4px'
                    }}
                  >
                    Создать объявление
                  </Link>
                </>
              )}
              <Link to="/favourites" style={{ color: 'white', textDecoration: 'none' }}>Избранные</Link>
              <Link to="/chats" style={{ color: hasUnread ? '#e74c3c' : 'white', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                Мои чаты
                {hasUnread && (
                  <span
                    aria-label="Есть непрочитанные"
                    style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '999px',
                      backgroundColor: '#e74c3c',
                      display: 'inline-block',
                    }}
                  />
                )}
              </Link>
              <Link to="/profile" style={{ color: 'white', textDecoration: 'none' }}>Профиль</Link>
            </>
          )}
        </div>
        <div>
          {isAuthenticated() ? (
            <button
              onClick={handleLogout}
              style={{
                backgroundColor: '#e74c3c',
                color: 'white',
                border: 'none',
                padding: '0.5rem 1rem',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Выйти
            </button>
          ) : (
            <div style={{ display: 'flex', gap: '1rem' }}>
              <Link to="/login" style={{ color: 'white', textDecoration: 'none' }}>Вход</Link>
              <Link to="/register" style={{ color: 'white', textDecoration: 'none' }}>Регистрация</Link>
            </div>
          )}
        </div>
      </nav>
      <main style={{ flex: 1, padding: '2rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
        {children}
      </main>
    </div>
  );
};
