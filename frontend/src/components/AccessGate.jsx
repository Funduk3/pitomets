import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const AuthRequired = () => {
  const location = useLocation();
  const from = location.state?.from || location.pathname;
  return (
    <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
      <h2 style={{ marginBottom: '0.5rem' }}>Вы не авторизованы</h2>
      <p style={{ marginBottom: '1.5rem', color: '#555' }}>Авторизуйтесь, чтобы продолжить</p>
      <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem' }}>
        <Link to="/login" state={{ from }} className="btn btn-primary">Вход</Link>
        <Link to="/register" state={{ from }} className="btn btn-secondary">Регистрация</Link>
      </div>
    </div>
  );
};

export const SellerRequired = () => (
  <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
    <h2 style={{ marginBottom: '0.5rem' }}>Нужно стать продавцом</h2>
    <p style={{ marginBottom: '1.5rem', color: '#555' }}>Этот раздел доступен только продавцам</p>
    <Link to="/seller/profile" className="btn btn-primary">Стать продавцом</Link>
  </div>
);

export const SellerApprovalRequired = () => (
  <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
    <h2 style={{ marginBottom: '0.5rem' }}>Профиль на модерации</h2>
    <p style={{ marginBottom: '1.5rem', color: '#555' }}>
      Доступ к созданию объявлений появится после одобрения модератором.
    </p>
    <Link to="/" className="btn btn-secondary">На главную</Link>
  </div>
);

export const AdminRequired = () => (
  <div style={{ maxWidth: '520px', margin: '0 auto', textAlign: 'center' }}>
    <h2 style={{ marginBottom: '0.5rem' }}>Недостаточно прав</h2>
    <p style={{ marginBottom: '1.5rem', color: '#555' }}>Этот раздел доступен только администратору</p>
    <Link to="/" className="btn btn-secondary">На главную</Link>
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
  const isApprovedSeller = user?.sellerProfileApproved === true;
  if (!isAuthenticated()) return <AuthRequired />;
  if (!isSeller) return <SellerRequired />;
  if (!isApprovedSeller) return <SellerApprovalRequired />;
  return children;
};

export const RequireAdmin = ({ children }) => {
  const { isAuthenticated, user, loading } = useAuth();
  if (loading) return <div>Грузим...</div>;
  if (!isAuthenticated()) return <AuthRequired />;
  if (user?.role !== 'ADMIN') return <AdminRequired />;
  return children;
};
