import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAuthErrorMessage } from '../util/authErrors';

export const Register = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { register } = useAuth();
  const location = useLocation();
  const redirectTo = location.state?.from || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (password !== confirmPassword) {
      setError('Пароли не совпадают.');
      return;
    }

    setLoading(true);

    try {
      const response = await register(email, password, fullName);
      setSuccess(
        response?.message ||
          'На вашу почту отправлено письмо с подтверждением. Перейдите по ссылке из письма, чтобы войти в аккаунт.'
      );
    } catch (err) {
      setError(getAuthErrorMessage(err, 'register'));
    } finally {
      setLoading(false);
    }
  };

  const EyeButton = ({ onClick, visible }) => (
    <button
      type="button"
      aria-label={visible ? 'Скрыть пароль' : 'Показать пароль'}
      onClick={onClick}
      style={{
        position: 'absolute',
        right: '0.5rem',
        top: '50%',
        transform: 'translateY(-50%)',
        background: 'transparent',
        border: 'none',
        cursor: 'pointer',
        color: '#2A2A2A',
        padding: 0,
        display: 'flex',
        alignItems: 'center'
      }}
    >
      {visible ? (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
          <path d="M17.94 17.94L6.06 6.06" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M10.58 10.58A3 3 0 0 0 13.42 13.42" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M2.03 12.13C3.73 16.5 7.62 19.88 12.62 19.88c2.2 0 4.24-.73 5.94-1.81" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M21.97 11.87C20.27 7.5 16.38 4.12 11.38 4.12c-2.2 0-4.24.73-5.94 1.81" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ) : (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <circle cx="12" cy="12" r="3" stroke="#2A2A2A" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      )}
    </button>
  );

  return (
    <div className="auth-card">
      <h2>Регистрация</h2>
      {success && <div style={{ color: 'green', marginBottom: '1rem' }}>{success}</div>}
      {error && <div style={{ color: 'red', marginBottom: '1rem' }}>{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Ваше имя:</label>
          <input
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            className="form-input"
          />
        </div>
        <div className="form-group">
          <label className="form-label">Почта:</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="form-input"
          />
        </div>
        <div className="form-group">
          <label className="form-label">Пароль:</label>
          <div style={{ position: 'relative' }}>
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="form-input"
              style={{ paddingRight: '3rem' }}
            />
            <EyeButton onClick={() => setShowPassword((s) => !s)} visible={showPassword} />
          </div>
        </div>
        <div className="form-group">
          <label className="form-label">Подтвердите пароль:</label>
          <div style={{ position: 'relative' }}>
            <input
              type={showConfirmPassword ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              className="form-input"
              style={{ paddingRight: '3rem' }}
            />
            <EyeButton onClick={() => setShowConfirmPassword((s) => !s)} visible={showConfirmPassword} />
          </div>
        </div>
        <button
          type="submit"
          disabled={loading}
          className="btn btn-primary"
          style={{ width: '100%' }}
        >
          {loading ? 'Регистрируем...' : 'Регистрация'}
        </button>
      </form>
      <div className="auth-switch-block">
        <p className="auth-switch-text">Уже есть аккаунт?</p>
        <Link to="/login" state={{ from: redirectTo }} className="btn btn-secondary btn-block auth-switch-action">
          Войти
        </Link>
      </div>
    </div>
  );
};
