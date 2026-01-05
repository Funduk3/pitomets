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

  useEffect(() => {
    loadProfile();
    loadAvatar();

    return () => {
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
    };
  }, []);

  const loadProfile = async () => {
    try {
      const data = await userAPI.getCurrentProfile();
      setProfile(data);
    } catch (err) {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const loadAvatar = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        // Use the API instance to get the avatar with proper auth headers
        const response = await fetch(photosAPI.getAvatar(), {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        if (response.ok) {
          const blob = await response.blob();
          const url = URL.createObjectURL(blob);
          setAvatarUrl(url);
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
      // Clean up old blob URL if exists
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
      loadAvatar();
    } catch (err) {
      alert('Failed to upload avatar');
    }
  };

  const handleDeleteAvatar = async () => {
    try {
      await photosAPI.deleteAvatar();
      // Clean up blob URL if exists
      if (avatarUrl && avatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarUrl);
      }
      setAvatarUrl(null);
    } catch (err) {
      alert('Failed to delete avatar');
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!profile) return <div>No profile found</div>;

  return (
    <ProtectedRoute>
      <div>
        <h2>My Profile</h2>
        <div style={{ display: 'flex', gap: '2rem', marginTop: '2rem' }}>
          <div>
            <h3>User Information</h3>
            <p><strong>Name:</strong> {profile.fullName}</p>
            <p><strong>Email:</strong> {profile.email}</p>
          </div>
          <div>
            <h3>Avatar</h3>
            {avatarUrl ? (
              <div>
                <img src={avatarUrl} alt="Avatar" style={{ width: '150px', height: '150px', borderRadius: '50%', objectFit: 'cover' }} />
                <div style={{ marginTop: '1rem' }}>
                  <input type="file" accept="image/*" onChange={handleAvatarUpload} style={{ marginRight: '1rem' }} />
                  <button onClick={handleDeleteAvatar} style={{ backgroundColor: '#e74c3c', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '4px', cursor: 'pointer' }}>
                    Delete Avatar
                  </button>
                </div>
              </div>
            ) : (
              <div>
                <p>No avatar uploaded</p>
                <input type="file" accept="image/*" onChange={handleAvatarUpload} />
              </div>
            )}
          </div>
        </div>

        {profile.shopName ? (
          <div style={{ marginTop: '2rem' }}>
            <h3>Seller Profile</h3>
            <p><strong>Shop Name:</strong> {profile.shopName}</p>
            <p><strong>Description:</strong> {profile.description || 'No description'}</p>
            <p><strong>Rating:</strong> {profile.rating ? profile.rating.toFixed(2) : 'No ratings yet'}</p>
            <p><strong>Verified:</strong> {profile.isVerified ? 'Yes' : 'No'}</p>
            <Link to="/seller/profile" style={{ color: '#3498db' }}>Edit Seller Profile</Link>
          </div>
        ) : (
          <div style={{ marginTop: '2rem' }}>
            <p>You don't have a seller profile yet.</p>
            <Link to="/seller/profile" style={{ color: '#3498db' }}>Create Seller Profile</Link>
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
};

