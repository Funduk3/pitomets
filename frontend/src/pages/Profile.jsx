import { useState, useEffect } from 'react';
import { userAPI } from '../api/user';
import { sellerAPI } from '../api/seller';
import { photosAPI } from '../api/photos';
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

  if (loading) return <div>Грузим...</div>;
  if (!profile) return <div>Профиль не найден</div>;

  return (
      <ProtectedRoute>
        <div>
          <h2>Мой профиль</h2>

          {error && (
              <div style={{ color: 'red', marginBottom: '1rem' }}>
                {error}
              </div>
          )}

          <div style={{ display: 'flex', gap: '2rem', marginTop: '2rem' }}>
            <div>
              <h3>Информация о пользователе</h3>
              <form onSubmit={handleSaveProfile}>
                <div style={{ marginBottom: '0.75rem' }}>
                  <label style={{ display: 'block', marginBottom: '0.5rem' }}>Имя:</label>

                  {!isEditing ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <span style={{ fontSize: '1rem' }}>
                      {profile.fullName || '—'}
                    </span>
                        <button
                            type="button"
                            onClick={() => setIsEditing(true)}
                            style={{
                              padding: '0.3rem 0.75rem',
                              backgroundColor: '#eee',
                              border: '1px solid #ccc',
                              borderRadius: '4px',
                              cursor: 'pointer'
                            }}
                            disabled={savingProfile}
                        >
                          Изменить
                        </button>
                      </div>
                  ) : (
                      <input
                          type="text"
                          value={fullName}
                          onChange={(e) => setFullName(e.target.value)}
                          autoFocus
                          style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
                          required
                      />
                  )}
                </div>

                {/* Показать кнопки только в режиме редактирования */}
                {isEditing && (
                    <div style={{ marginTop: '0.75rem' }}>
                      <button
                          type="submit"
                          disabled={savingProfile}
                          style={{
                            padding: '0.5rem 1rem',
                            backgroundColor: '#3498db',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: savingProfile ? 'not-allowed' : 'pointer'
                          }}
                      >
                        {savingProfile ? 'Сохраняем...' : 'Сохранить'}
                      </button>

                      <button
                          type="button"
                          onClick={() => {
                            setFullName(profile.fullName || '');
                            setIsEditing(false);
                            setError('');
                          }}
                          style={{
                            marginLeft: '0.5rem',
                            padding: '0.5rem 1rem'
                          }}
                          disabled={savingProfile}
                      >
                        Отмена
                      </button>
                    </div>
                )}

                <p style={{ marginTop: '1rem' }}><strong>Почта:</strong> {profile.email}</p>
              </form>
            </div>

            <div>
              <h3>Аватар</h3>
              {avatarUrl ? (
                  <div>
                    <img src={avatarUrl} alt="Avatar" style={{ width: '150px', height: '150px', borderRadius: '50%', objectFit: 'cover' }} />
                    <div style={{ marginTop: '1rem' }}>
                      <input type="file" accept="image/*" onChange={handleAvatarUpload} style={{ marginRight: '1rem' }} />
                      <button onClick={handleDeleteAvatar} style={{ backgroundColor: '#e74c3c', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '4px', cursor: 'pointer' }}>
                        Удалить аватар
                      </button>
                    </div>
                  </div>
              ) : (
                  <div>
                    <p>Аватар не загружен</p>
                    <input type="file" accept="image/*" onChange={handleAvatarUpload} />
                  </div>
              )}
            </div>
          </div>

          {profile.shopName ? (
              <div style={{ marginTop: '2rem' }}>
                <h3>Профиль продавца</h3>
                <p><strong>Название магазина:</strong> {profile.shopName}</p>
                <p><strong>Описание:</strong> {profile.description || 'Описание пустое'}</p>
                <p><strong>Рейтинг:</strong> {profile.rating ? profile.rating.toFixed(2) : 'Пока нет отзывов'}</p>
                <p><strong>Верификация:</strong> {profile.isVerified ? 'Есть' : 'Нет'}</p>
                <Link to="/seller/profile" style={{ color: '#3498db' }}>Изменить профиль продавца</Link>
              </div>
          ) : (
              <div style={{ marginTop: '2rem' }}>
                <p>Вы пока что не продавец.</p>
                <Link to="/seller/profile" style={{ color: '#3498db' }}>Стать продавцом</Link>
              </div>
          )}
        </div>
      </ProtectedRoute>
  );
};
