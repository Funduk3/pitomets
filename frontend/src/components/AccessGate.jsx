import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const AuthRequired = () => (
  <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
    <h2 style={{ marginBottom: '0.5rem' }}>Вы не авторизованы</h2>
    <p style={{ marginBottom: '1.5rem', color: '#555' }}>Авторизуйтесь, чтобы продолжить</p>
    <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem' }}>
      <Link to="/login" className="btn btn-primary">Вход</Link>
      <Link to="/register" className="btn btn-secondary">Регистрация</Link>
    </div>
  </div>
);

export const SellerRequired = () => (
  <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
    <h2 style={{ marginBottom: '0.5rem' }}>Нужно стать продавцом</h2>
    <p style={{ marginBottom: '1.5rem', color: '#555' }}>Этот раздел доступен только продавцам</p>
    <Link to="/seller/profile" className="btn btn-primary">Стать продавцом</Link>
  </div>
);

export const RequireAuth = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated()) return <AuthRequired />;
  return children;
};

export const RequireSeller = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const isSeller = Boolean(user?.shopName || user?.isSeller);
  if (!isAuthenticated()) return <AuthRequired />;
  if (!isSeller) return <SellerRequired />;
  return children;
};
