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
  const [showScrollTop, setShowScrollTop] = useState(false);
  const [isMobileViewport, setIsMobileViewport] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

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

  useEffect(() => {
    const onScroll = () => {
      setShowScrollTop(window.scrollY > 480);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const media = window.matchMedia('(max-width: 768px)');
    const syncViewport = () => setIsMobileViewport(media.matches);
    syncViewport();
    media.addEventListener('change', syncViewport);
    return () => media.removeEventListener('change', syncViewport);
  }, []);

  const submitSearch = (value) => {
    const q = (value ?? searchQuery).trim();
    if (!q) return;
    setShowSuggestions(false);
    navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  const isSearchPage = location.pathname.startsWith('/search');
  const isLoggedIn = isAuthenticated();
  const isMobileGuest = !isLoggedIn && isMobileViewport;
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#FFFFFF' }}>
      <nav className="app-nav">
        <div className={`nav-left${isMobileGuest ? ' nav-left-mobile-guest' : ''}`}>
          <Link to="/" className="brand-link">
            Питомец
          </Link>
          <div className={`nav-links${isMobileGuest ? ' nav-links-mobile-guest' : ''}`}>
            {!isMobileGuest && (
              <Link to="/listings" style={{ color: '#111111', textDecoration: 'none' }}>Мои объявления</Link>
            )}
            <Link to="/favourites" style={{ color: '#111111', textDecoration: 'none' }}>Избранные</Link>
            <Link to="/chats" style={{ color: hasUnread ? '#FF6B5A' : '#111111', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
              Чаты
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
            {isAdmin && (
              <Link to="/moderation" style={{ color: '#111111', textDecoration: 'none' }}>Модерация</Link>
            )}
            {isMobileGuest && (
              <Link to="/login" className="nav-mobile-login">Вход</Link>
            )}
          </div>
          {!isSearchPage && (
            <div className="nav-search">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                  submitSearch();
                }}
                className="nav-search-form"
              >
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onFocus={() => suggestions.length && setShowSuggestions(true)}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                  placeholder="Ищем питомца..."
                  className="nav-search-input"
                />
                <button
                  type="submit"
                  className="nav-search-button"
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
        </div>
        <div className="nav-actions">
          {isLoggedIn ? (
            <>
              <Link
                to="/listings/create"
                className="nav-create"
              >
                Создать объявление
              </Link>
              <button
                onClick={handleLogout}
                className="nav-logout"
              >
                Выйти
              </button>
            </>
          ) : !isMobileGuest ? (
            <div className="nav-auth">
              <Link to="/login" style={{ color: '#111111', textDecoration: 'none' }}>Вход</Link>
            </div>
          ) : null}
        </div>
      </nav>
      <main className="main-content">
        {children}
      </main>
      {showScrollTop && (
        <button
          className="scroll-top"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          aria-label="Наверх"
        >
          ↑
        </button>
      )}
    </div>
  );
};
