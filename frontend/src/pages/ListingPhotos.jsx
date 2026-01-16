import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { photosAPI } from '../api/photos';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { useAuth } from '../context/AuthContext';

export const ListingPhotos = () => {
  const { id } = useParams();
  const { isAuthenticated } = useAuth();
  const [photos, setPhotos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    loadPhotos();
  }, [id]);

  const loadPhotos = async () => {
    try {
      const data = await photosAPI.getListingPhotos(parseInt(id));
      setPhotos(data.photos || []);
    } catch (err) {
      console.error('Failed to load photos:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    try {
      await photosAPI.uploadListingPhoto(parseInt(id), file);
      loadPhotos();
    } catch (err) {
      alert('Failed to upload photo');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (photoUrl) => {
    // Extract photoId from URL
    const photoId = photoUrl.split('/').pop();
    if (!window.confirm('Are you sure you want to delete this photo?')) return;

    try {
      await photosAPI.deleteListingPhoto(parseInt(id), parseInt(photoId));
      loadPhotos();
    } catch (err) {
      alert('Failed to delete photo');
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h2>Фотографии объявление</h2>
        <Link to={`/listings/${id}`} style={{ color: '#3498db' }}>Вернуться к объявлению</Link>
      </div>

      {isAuthenticated() && (
        <div style={{ marginBottom: '2rem' }}>
          <input
            type="file"
            accept="image/*"
            onChange={handleUpload}
            disabled={uploading}
            style={{ marginBottom: '1rem' }}
          />
          {uploading && <p>Грузим...</p>}
        </div>
      )}

      {photos.length === 0 ? (
        <p>Ни одна фотография пока не загружена</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
          {photos.map((photoUrl, index) => {
            const fullUrl = photoUrl.startsWith('http') ? photoUrl : `http://localhost:8080${photoUrl}`;
            return (
              <div key={index} style={{ position: 'relative' }}>
                <img
                  src={fullUrl}
                  alt={`Photo ${index + 1}`}
                  style={{ width: '100%', height: '200px', objectFit: 'cover', borderRadius: '8px' }}
                />
                {isAuthenticated() && (
                  <button
                    onClick={() => handleDelete(photoUrl)}
                    style={{
                      position: 'absolute',
                      top: '0.5rem',
                      right: '0.5rem',
                      backgroundColor: '#e74c3c',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      padding: '0.25rem 0.5rem',
                      cursor: 'pointer'
                    }}
                  >
                    Delete
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

