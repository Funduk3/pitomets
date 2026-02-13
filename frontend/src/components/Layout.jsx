import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';
import { searchAPI } from '../api/search';

export const Layout = ({ children }) => {
  const { isAuthenticated, logout, user } = useAuth();
  const { hasUnread } = useMessengerWS();
  const navigate = useNavigate();
  const location = useLocation();
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

  useEffect(() => {
    if (!location.pathname.startsWith('/search')) {
      setSearchQuery('');
      setSuggestions([]);
      setShowSuggestions(false);
    }
  }, [location.pathname]);

  const submitSearch = (value) => {
    const q = (value ?? searchQuery).trim();
    if (!q) return;
    setShowSuggestions(false);
    navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  const isSearchPage = location.pathname.startsWith('/search');

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#FFFFFF' }}>
      <nav style={{
        backgroundColor: '#FFFFFF',
        padding: '1rem 1.5rem',
        color: '#111111',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid #EDEDED',
        position: 'sticky',
        top: 0,
        zIndex: 1000
      }}>
        <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'center' }}>
          <Link to="/" style={{ color: '#111111', textDecoration: 'none', fontSize: '1.5rem', fontWeight: '700', letterSpacing: '-0.01em' }}>
            Питомец
          </Link>
          {isAuthenticated() && (
            <>
              {isSeller && (
                <Link to="/listings" style={{ color: '#111111', textDecoration: 'none' }}>Мои объявления</Link>
              )}
              <Link to="/favourites" style={{ color: '#111111', textDecoration: 'none' }}>Избранные</Link>
              <Link to="/chats" style={{ color: hasUnread ? '#FF6B5A' : '#111111', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                Мои чаты
                {hasUnread && (
                  <span
                    aria-label="Есть непрочитанные"
                    style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '999px',
                      backgroundColor: '#FF6B5A',
                      display: 'inline-block',
                    }}
                  />
                )}
              </Link>
              <Link to="/profile" style={{ color: '#111111', textDecoration: 'none' }}>Профиль</Link>
              {!isSearchPage && (
                <div style={{ position: 'relative', minWidth: '420px' }}>
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
                        width: '360px',
                        padding: '0.55rem 0.75rem',
                        borderRadius: '8px',
                        border: '1px solid #EDEDED',
                        backgroundColor: '#FFFFFF',
                        color: '#111111'
                      }}
                    />
                    <button
                      type="submit"
                      style={{
                        marginLeft: '0.5rem',
                        padding: '0.5rem 0.85rem',
                        backgroundColor: '#111111',
                        color: '#FFFFFF',
                        border: '1px solid #111111',
                        borderRadius: '8px',
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
                      background: '#FFFFFF',
                      border: '1px solid #EDEDED',
                      borderRadius: '8px',
                      zIndex: 20,
                      color: '#111111',
                      boxShadow: '0 8px 18px rgba(17, 17, 17, 0.08)'
                    }}>
                      {suggestions.map((s, idx) => (
                        <div
                          key={idx}
                          onMouseDown={() => {
                            setSearchQuery(s.title);
                            submitSearch(s.title);
                          }}
                          style={{
                            padding: '0.6rem 0.75rem',
                            cursor: 'pointer',
                            borderBottom: '1px solid #EDEDED',
                            color: '#111111'
                          }}
                        >
                          {s.title}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          {isAuthenticated() && isSeller && (
            <Link
              to="/listings/create"
              style={{
                color: '#FFFFFF',
                textDecoration: 'none',
                backgroundColor: '#FF6B5A',
                padding: '0.45rem 0.75rem',
                borderRadius: '8px'
              }}
            >
              Создать объявление
            </Link>
          )}
          {isAuthenticated() ? (
            <button
              onClick={handleLogout}
              style={{
                backgroundColor: '#EDEDED',
                color: '#111111',
                border: '1px solid #EDEDED',
                padding: '0.5rem 1rem',
                borderRadius: '8px',
                cursor: 'pointer'
              }}
            >
              Выйти
            </button>
          ) : (
            <div style={{ display: 'flex', gap: '1rem' }}>
              <Link to="/login" style={{ color: '#111111', textDecoration: 'none' }}>Вход</Link>
              <Link to="/register" style={{ color: '#111111', textDecoration: 'none' }}>Регистрация</Link>
            </div>
          )}
        </div>
      </nav>
      <main style={{ flex: 1, padding: '2.5rem 1.5rem', maxWidth: '1100px', margin: '0 auto', width: '100%' }}>
        {children}
      </main>
    </div>
  );
};
