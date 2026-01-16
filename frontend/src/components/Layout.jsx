import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';

export const Layout = ({ children }) => {
  const { isAuthenticated, logout, user, loading } = useAuth();
  const { hasUnread } = useMessengerWS();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isSeller = Boolean(user?.shopName);

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
          <Link to="/search" style={{ color: 'white', textDecoration: 'none' }}>Поиск</Link>
          {isAuthenticated() && (
            <>
              {isSeller && (
                <Link to="/listings" style={{ color: 'white', textDecoration: 'none' }}>Мои объявления</Link>
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

