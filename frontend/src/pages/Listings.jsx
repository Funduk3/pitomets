import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { Link } from 'react-router-dom';

export const Listings = () => {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadListings();
  }, []);

  const loadListings = async () => {
    try {
      setLoading(true);
      const data = await listingsAPI.getMyListings();
      setListings(data);
    } catch (err) {
      setError('Failed to load listings');
      console.error('Error loading listings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (listingId) => {
    if (!window.confirm('Are you sure you want to delete this listing?')) return;

    try {
      await listingsAPI.deleteListing(listingId);
      setListings(listings.filter(l => l.listingsId !== listingId));
    } catch (err) {
      alert('Failed to delete listing');
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <ProtectedRoute>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2>My Listings</h2>
          <Link
            to="/listings/create"
            style={{
              padding: '0.75rem 1.5rem',
              backgroundColor: '#27ae60',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
          >
            Create New Listing
          </Link>
        </div>
        {error && (
          <div style={{ color: 'red', marginBottom: '1rem', padding: '1rem', backgroundColor: '#ffe6e6', borderRadius: '4px' }}>
            {error}
            <button onClick={loadListings} style={{ marginLeft: '1rem', padding: '0.5rem 1rem', cursor: 'pointer' }}>Retry</button>
          </div>
        )}
        {listings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p>You don't have any listings yet.</p>
            <Link to="/listings/create" style={{ color: '#3498db' }}>Create your first listing</Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
            {listings.map((listing) => (
              <div key={listing.listingsId} style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1rem' }}>
                <h3>{listing.title || 'Untitled'}</h3>
                <p>{listing.description}</p>
                <p><strong>Price:</strong> ${listing.price}</p>
                <p><strong>Species:</strong> {listing.species}</p>
                {listing.breed && <p><strong>Breed:</strong> {listing.breed}</p>}
                <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
                  <Link
                    to={`/listings/${listing.listingsId}`}
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
                  <Link
                    to={`/listings/${listing.listingsId}/edit`}
                    style={{
                      padding: '0.5rem 1rem',
                      backgroundColor: '#f39c12',
                      color: 'white',
                      textDecoration: 'none',
                      borderRadius: '4px',
                      fontSize: '0.9rem'
                    }}
                  >
                    Edit
                  </Link>
                  <button
                    onClick={() => handleDelete(listing.listingsId)}
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
                    Delete
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

