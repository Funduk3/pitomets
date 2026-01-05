import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { photosAPI } from '../api/photos';
import { favouritesAPI } from '../api/favourites';
import { useAuth } from '../context/AuthContext';

export const ListingDetail = () => {
  const { id } = useParams();
  const { isAuthenticated, user } = useAuth();
  const [listing, setListing] = useState(null);
  const [photos, setPhotos] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isFavourite, setIsFavourite] = useState(false);

  useEffect(() => {
    loadListing();
    loadPhotos();
    loadReviews();
  }, [id]);

  const loadListing = async () => {
    try {
      const data = await listingsAPI.getListing(parseInt(id));
      setListing(data);
    } catch (err) {
      setError('Failed to load listing');
    } finally {
      setLoading(false);
    }
  };

  const loadPhotos = async () => {
    try {
      const data = await photosAPI.getListingPhotos(parseInt(id));
      setPhotos(data.photos || []);
    } catch (err) {
      console.error('Failed to load photos:', err);
    }
  };

  const loadReviews = async () => {
    try {
      const data = await listingsAPI.getListingReviews(parseInt(id));
      setReviews(data);
    } catch (err) {
      console.error('Failed to load reviews:', err);
    }
  };

  const handleAddFavourite = async () => {
    if (!isAuthenticated()) {
      alert('Please login to add favourites');
      return;
    }

    try {
      await favouritesAPI.addFavourite(parseInt(id));
      setIsFavourite(true);
    } catch (err) {
      const msg = err.response?.data?.message;
      const status = err.response?.status;
      if (status === 409 || (msg && msg.toLowerCase().includes('already'))) {
        setIsFavourite(true);
        alert('This listing is already in your favourites.');
      } else {
        alert(msg || 'Failed to add to favourites');
      }
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!listing) return <div>Listing not found</div>;

  return (
    <div>
      <h2>{listing.title || 'Untitled'}</h2>
      <p style={{ marginTop: '0.25rem', color: '#555' }}>
        <strong>Rating:</strong>{' '}
        {listing.sellerRating != null ? `${listing.sellerRating.toFixed(2)} / 5` : 'No ratings yet'}
        {listing.sellerReviewsCount != null && ` (${listing.sellerReviewsCount} reviews)`}
      </p>
      <div style={{ display: 'flex', gap: '2rem', marginTop: '2rem' }}>
        <div style={{ flex: 1 }}>
          {photos.length > 0 ? (
            <div>
              {photos.map((photoUrl, index) => (
                <img
                  key={index}
                  src={photoUrl.startsWith('http') ? photoUrl : `http://localhost:8080${photoUrl}`}
                  alt={`Photo ${index + 1}`}
                  style={{ width: '100%', maxWidth: '500px', marginBottom: '1rem', borderRadius: '8px' }}
                />
              ))}
            </div>
          ) : (
            <div style={{ width: '100%', height: '300px', backgroundColor: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '8px' }}>
              No photos available
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}>
          <p><strong>Description:</strong> {listing.description}</p>
          <p><strong>Price:</strong> ${listing.price}</p>
          <p><strong>Species:</strong> {listing.species || 'N/A'}</p>
          {listing.breed && <p><strong>Breed:</strong> {listing.breed}</p>}
          <p><strong>Age:</strong> {listing.ageMonths} months</p>
          {listing.mother && <p><strong>Mother ID:</strong> {listing.mother}</p>}
          {listing.father && <p><strong>Father ID:</strong> {listing.father}</p>}
          {isAuthenticated() && user?.id === listing.sellerId && (
            <Link
              to={`/listings/${id}/photos`}
              style={{
                display: 'inline-block',
                marginTop: '0.75rem',
                padding: '0.5rem 1rem',
                backgroundColor: '#f39c12',
                color: 'white',
                textDecoration: 'none',
                borderRadius: '4px'
              }}
            >
              Manage Photos
            </Link>
          )}
          {isAuthenticated() && (
            <button
              onClick={handleAddFavourite}
              disabled={isFavourite}
              style={{
                marginTop: '1rem',
                padding: '0.75rem 1.5rem',
                backgroundColor: isFavourite ? '#95a5a6' : '#e74c3c',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: isFavourite ? 'not-allowed' : 'pointer'
              }}
            >
              {isFavourite ? 'In Favourites' : 'Add to Favourites'}
            </button>
          )}
        </div>
      </div>

      <div style={{ marginTop: '3rem' }}>
        <h3>Reviews</h3>
        {reviews.length === 0 ? (
          <p>No reviews yet</p>
        ) : (
          <div>
            {reviews.map((review) => (
              <div key={review.id} style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1rem', marginBottom: '1rem' }}>
                <p><strong>Rating:</strong> {'⭐'.repeat(review.rating)}</p>
                {review.text && <p>{review.text}</p>}
                <p style={{ fontSize: '0.9rem', color: '#666' }}>
                  {new Date(review.createdAt).toLocaleDateString()}
                </p>
              </div>
            ))}
          </div>
        )}
        {isAuthenticated() && (
          <Link
            to={`/listings/${id}/review`}
            style={{
              display: 'inline-block',
              marginTop: '1rem',
              padding: '0.75rem 1.5rem',
              backgroundColor: '#3498db',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
          >
            Write a Review
          </Link>
        )}
      </div>
    </div>
  );
};

