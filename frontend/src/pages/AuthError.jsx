import { Link, useLocation } from 'react-router-dom';

export const AuthError = () => {
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  const message =
    params.get('message') ||
    params.get('error_description') ||
    params.get('error') ||
    'Авторизация была отменена или завершилась ошибкой';

  return (
    <div className="auth-card">
      <h2>Ошибка авторизации</h2>
      <div className="error-box" style={{ marginTop: '1rem' }}>
        {message}
      </div>
      <Link to="/login" className="btn btn-primary btn-block">
        Вернуться ко входу
      </Link>
    </div>
  );
};
