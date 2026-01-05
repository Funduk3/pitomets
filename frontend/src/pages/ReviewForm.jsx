import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { ProtectedRoute } from '../components/ProtectedRoute';

export const ReviewForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [rating, setRating] = useState(5);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await listingsAPI.createReview({
        listingId: parseInt(id),
        rating,
        text: text || null,
      });
      navigate(`/listings/${id}`);
    } catch (err) {
      const backendMessage = err.response?.data?.message;
      if (backendMessage?.toLowerCase()?.includes('own listing')) {
        setError('Нельзя оставлять отзыв на своё объявление.');
      } else {
        setError(backendMessage || 'Failed to create review');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <ProtectedRoute>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h2>Write a Review</h2>
        {error && <div style={{ color: 'red', marginBottom: '1rem' }}>{error}</div>}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Rating:</label>
            <select
              value={rating}
              onChange={(e) => setRating(parseInt(e.target.value))}
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            >
              <option value={1}>1 ⭐</option>
              <option value={2}>2 ⭐⭐</option>
              <option value={3}>3 ⭐⭐⭐</option>
              <option value={4}>4 ⭐⭐⭐⭐</option>
              <option value={5}>5 ⭐⭐⭐⭐⭐</option>
            </select>
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Review Text (optional):</label>
            <textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows="5"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '0.75rem',
              backgroundColor: '#3498db',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              fontSize: '1rem',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Submitting...' : 'Submit Review'}
          </button>
        </form>
      </div>
    </ProtectedRoute>
  );
};

