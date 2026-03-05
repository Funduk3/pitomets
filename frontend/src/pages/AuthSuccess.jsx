import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const OAUTH_REPEAT_GUARD_KEY = 'oauth:last-processed-token';

export const AuthSuccess = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { completeOAuthLogin } = useAuth();
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) {
      return;
    }
    processedRef.current = true;

    const params = new URLSearchParams(location.search);
    const token = params.get('token')?.trim();
    const refreshToken = params.get('refreshToken')?.trim() || null;

    if (!token) {
      navigate('/auth/error?message=Токен%20авторизации%20не%20получен', { replace: true });
      return;
    }

    if (sessionStorage.getItem(OAUTH_REPEAT_GUARD_KEY) === token) {
      navigate('/profile', { replace: true });
      return;
    }

    sessionStorage.setItem(OAUTH_REPEAT_GUARD_KEY, token);

    // Удаляем query-параметры с токеном, чтобы он не оставался в истории.
    window.history.replaceState({}, document.title, location.pathname);

    completeOAuthLogin(token, refreshToken)
      .then(() => {
        navigate('/profile', { replace: true });
      })
      .catch((error) => {
        sessionStorage.removeItem(OAUTH_REPEAT_GUARD_KEY);
        const message = error?.response?.data?.message || 'Не удалось завершить вход через OAuth2';
        navigate(`/auth/error?message=${encodeURIComponent(message)}`, { replace: true });
      });
  }, [completeOAuthLogin, location.pathname, location.search, navigate]);

  return (
    <div className="auth-card">
      <h2>Вход через OAuth2</h2>
      <p style={{ marginTop: '0.75rem' }}>Завершаем авторизацию, подождите...</p>
    </div>
  );
};
