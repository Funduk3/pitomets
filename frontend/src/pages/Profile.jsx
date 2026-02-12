import { useState, useEffect } from 'react';
import { userAPI } from '../api/user';
import { photosAPI } from '../api/photos';
import { authAPI } from '../api/auth';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Link } from 'react-router-dom';

export const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [avatarUrl, setAvatarUrl] = useState(null);
  const [fullName, setFullName] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [changingPassword, setChangingPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  useEffect(() => {
    loadProfile();
    loadAvatar();
    return () => {
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
    };
  }, []);

  const setAvatarWithRevoke = (newUrl) => {
    if (avatarUrl && avatarUrl.startsWith('blob:')) {
      URL.revokeObjectURL(avatarUrl);
    }
    setAvatarUrl(newUrl);
  };

  const loadProfile = async () => {
    try {
      setError('');
      const data = await userAPI.getCurrentProfile();
      setProfile(data);
      setFullName(data?.fullName || '');
    } catch (err) {
      setError('Не удалось загрузить профиль');
    } finally {
      setLoading(false);
    }
  };

  const loadAvatar = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        const response = await fetch(photosAPI.getAvatar(), {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.ok) {
          const blob = await response.blob();
          const url = URL.createObjectURL(blob);
          setAvatarWithRevoke(url);
        }
      }
    } catch (err) {
      console.error('Failed to load avatar:', err);
    }
  };

  const handleAvatarUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      await photosAPI.uploadAvatar(file);
      // Clean up old blob URL if exists and reload new avatar
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
      await loadAvatar();
    } catch (err) {
      alert('Не удалось загрузить аватар');
    }
  };

  const handleDeleteAvatar = async () => {
    try {
      await photosAPI.deleteAvatar();
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
      setAvatarUrl(null);
    } catch (err) {
      alert('Не удалось удалить аватар');
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setError('');
    setSavingProfile(true);

    try {
      const updated = await userAPI.updateCurrentProfile({ fullName });
      setProfile(updated);
      setFullName(updated.fullName || ''); // keep local state in sync
      setIsEditing(false); // only after success
    } catch (err) {
      setError('Не удалось обновить профиль');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess('');

    if (newPassword !== confirmPassword) {
      setPasswordError('Пароли не совпадают');
      return;
    }

    try {
      setChangingPassword(true);
      await authAPI.changePassword(currentPassword, newPassword, confirmPassword);
      setPasswordSuccess('Пароль изменен');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPasswordError('Не удалось изменить пароль');
    } finally {
      setChangingPassword(false);
    }
  };

  const toggleChangePassword = () => {
    setIsChangingPassword((prev) => !prev);
    setPasswordError('');
    setPasswordSuccess('');
    if (isChangingPassword) {
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    }
  };

  if (loading) return <div>Грузим...</div>;
  if (!profile) return <div>Профиль не найден</div>;

  return (
    <ProtectedRoute>
      <div className="container">
        <h2 style={{ marginBottom: '1rem' }}>Мой профиль</h2>

        {error && (
          <div className="error-box">{error}</div>
        )}

        <div className="card">
          <div className="card-body">
            <div className="two-col">
              <div style={{ flex: 1 }}>
                <h3 style={{ marginTop: 0 }}>Информация о пользователе</h3>
                <form onSubmit={handleSaveProfile}>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <label className="form-label">Имя:</label>

                    {!isEditing ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <span style={{ fontSize: '1rem' }}>{profile.fullName || '—'}</span>
                        <button type="button" onClick={() => setIsEditing(true)} className="btn btn-secondary" disabled={savingProfile}>Изменить</button>
                      </div>
                    ) : (
                      <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} autoFocus className="form-input" required />
                    )}
                  </div>

                  {isEditing && (
                    <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem' }}>
                      <button type="submit" className="btn btn-primary" disabled={savingProfile}>{savingProfile ? 'Сохраняем...' : 'Сохранить'}</button>
                      <button type="button" className="btn btn-ghost" onClick={() => { setFullName(profile.fullName || ''); setIsEditing(false); setError(''); }} disabled={savingProfile}>Отмена</button>
                    </div>
                  )}

                  <p style={{ marginTop: '1rem' }}><strong>Почта:</strong> <span className="small-muted">{profile.email}</span></p>
                </form>
              </div>

              <div style={{ width: 220 }}>
                <h3 style={{ marginTop: 0 }}>Аватар</h3>
                {avatarUrl ? (
                  <div>
                    <img src={avatarUrl} alt="Avatar" className="avatar-md" />
                    <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input type="file" accept="image/*" onChange={handleAvatarUpload} />
                      <button onClick={handleDeleteAvatar} className="btn btn-danger">Удалить</button>
                    </div>
                  </div>
                ) : (
                  <div>
                    <p className="small-muted">Аватар не загружен</p>
                    <input type="file" accept="image/*" onChange={handleAvatarUpload} />
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div style={{ marginTop: '1rem' }}>
          <div className="card">
            <div className="card-body">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
                <h3 style={{ margin: 0 }}>Смена пароля</h3>
                <button type="button" className="btn btn-secondary" onClick={toggleChangePassword}>
                  {isChangingPassword ? 'Скрыть форму' : 'Сменить пароль'}
                </button>
              </div>

              {isChangingPassword && (
                <form onSubmit={handleChangePassword} style={{ maxWidth: 420, marginTop: '1rem' }}>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <label className="form-label">Текущий пароль</label>
                    <input
                      type="password"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      className="form-input"
                      required
                    />
                  </div>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <label className="form-label">Новый пароль</label>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="form-input"
                      required
                    />
                  </div>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <label className="form-label">Повторите пароль</label>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="form-input"
                      required
                    />
                  </div>

                  {passwordError && <div className="error-box" style={{ marginBottom: '0.75rem' }}>{passwordError}</div>}
                  {passwordSuccess && <div className="success-box" style={{ marginBottom: '0.75rem' }}>{passwordSuccess}</div>}

                  <button type="submit" className="btn btn-primary" disabled={changingPassword}>
                    {changingPassword ? 'Сохраняем...' : 'Изменить пароль'}
                  </button>
                </form>
              )}
            </div>
          </div>

          {profile.shopName ? (
            <div className="card">
              <div className="card-body">
                <h3>Профиль продавца</h3>
                <p><strong>Название магазина:</strong> {profile.shopName}</p>
                <p className="small-muted"><strong>Описание:</strong> {profile.description || 'Описание пустое'}</p>
                <p className="small-muted"><strong>Рейтинг:</strong> {profile.rating ? profile.rating.toFixed(2) : 'Пока нет отзывов'}</p>
                <p className="small-muted"><strong>Верификация:</strong> {profile.isVerified ? 'Есть' : 'Нет'}</p>
                <div style={{ marginTop: '0.5rem' }}>
                  <Link to="/seller/profile" className="btn btn-secondary">Изменить профиль продавца</Link>
                </div>
              </div>
            </div>
          ) : (
            <div className="card">
              <div className="card-body">
                <p>Вы пока что не продавец.</p>
                <Link to="/seller/profile" className="btn btn-primary">Стать продавцом</Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
};
