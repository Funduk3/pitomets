import { useState, useEffect } from 'react';
import { favouritesAPI } from '../api/favourites';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Link } from 'react-router-dom';

export const Favourites = () => {
  const [favourites, setFavourites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadFavourites();
  }, []);

  const loadFavourites = async () => {
    try {
      const data = await favouritesAPI.getFavourites();
      setFavourites(data);
    } catch (err) {
      setError('Failed to load favourites');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (listingId) => {
    try {
      await favouritesAPI.deleteFavourite(listingId);
      setFavourites(favourites.filter(f => f.id !== listingId));
    } catch (err) {
      alert('Failed to remove from favourites');
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <ProtectedRoute>
      <div>
        <h2>My Favourites</h2>
        {favourites.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>You don't have any favourites yet.</p>
            <Link to="/search" style={{ color: '#3498db' }}>Search for listings</Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem', marginTop: '2rem' }}>
            {favourites.map((listing) => (
              <div key={listing.id} style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1rem' }}>
                <h3>{listing.title}</h3>
                <p>{listing.description}</p>
                <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                  <Link
                    to={`/listings/${listing.id}`}
                    style={{
                      padding: '0.5rem 1rem',
                      backgroundColor: '#3498db',
                      color: 'white',
                      textDecoration: 'none',
                      borderRadius: '4px',
                      fontSize: '0.9rem'
                    }}
                  >
                    View
                  </Link>
                  <button
                    onClick={() => handleRemove(listing.id)}
                    style={{
                      padding: '0.5rem 1rem',
                      backgroundColor: '#e74c3c',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.9rem'
                    }}
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
};

