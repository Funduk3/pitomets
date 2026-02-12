import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { ProtectedRoute } from '../components/ProtectedRoute';
import './listing-form.css';

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
      <div className="lf-form">
        <div className="lf-card">
          <h2>Написать отзыв</h2>
          {error && <div className="error-box" role="alert" aria-live="assertive">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="review-rating" className="form-label">Оценка:</label>
              <select
                id="review-rating"
                className="form-input"
                value={rating}
                onChange={(e) => setRating(parseInt(e.target.value))}
              >
                <option value={1}>1 ⭐</option>
                <option value={2}>2 ⭐⭐</option>
                <option value={3}>3 ⭐⭐⭐</option>
                <option value={4}>4 ⭐⭐⭐⭐</option>
                <option value={5}>5 ⭐⭐⭐⭐⭐</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="review-text" className="form-label">Текст отзыва (опционально):</label>
              <textarea
                id="review-text"
                className="form-input"
                value={text}
                onChange={(e) => setText(e.target.value)}
                rows="5"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="lf-submit btn-primary"
              aria-disabled={loading}
            >
              {loading ? 'Отправляем...' : 'Отправить отзыв'}
            </button>
          </form>
        </div>
      </div>
    </ProtectedRoute>
  );
};
