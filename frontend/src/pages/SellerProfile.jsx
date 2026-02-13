import { useState, useEffect } from 'react';
import { sellerAPI } from '../api/seller';
import { userAPI } from '../api/user';
import { ProtectedRoute } from '../components/ProtectedRoute';

export const SellerProfile = () => {
  const [shopName, setShopName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isUpdate, setIsUpdate] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const profile = await userAPI.getCurrentProfile();
      if (profile.shopName) {
        setShopName(profile.shopName);
        setDescription(profile.description || '');
        setIsUpdate(true);
      }
    } catch (err) {
      console.error('Failed to load profile:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isUpdate) {
        await sellerAPI.updateSellerProfile({ shopName, description });
      } else {
        await sellerAPI.createSellerProfile({ shopName, description });
      }
      alert(isUpdate ? 'Профиль успешно обновлён!' : 'Профиль успешно создан!');
      window.location.href = '/profile';
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save profile');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ProtectedRoute>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h2>{isUpdate ? 'Обновить' : 'Создать'} профиль продавца</h2>
        {error && <div style={{ color: 'red', marginBottom: '1rem' }}>{error}</div>}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Название магазина:</label>
            <input
              type="text"
              value={shopName}
              onChange={(e) => setShopName(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Описание:</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows="5"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary btn-block"
          >
            {loading ? 'Сохраняем...' : isUpdate ? 'Обновить профиль' : 'Создать профиль'}
          </button>
        </form>
      </div>
    </ProtectedRoute>
  );
};
