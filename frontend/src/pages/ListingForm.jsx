import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { userAPI } from '../api/user';
import { ProtectedRoute } from '../components/ProtectedRoute';

export const ListingForm = () => {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    species: '',
    breed: '',
    ageMonths: 0,
    price: '',
    mother: '',
    father: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [profile, setProfile] = useState(null);
  const [checkingProfile, setCheckingProfile] = useState(true);

  useEffect(() => {
    if (!isEdit) {
      checkSellerProfile();
    } else {
      loadListing();
    }
  }, [id]);

  const checkSellerProfile = async () => {
    try {
      const userProfile = await userAPI.getCurrentProfile();
      setProfile(userProfile);
      if (!userProfile.shopName) {
        setError('You need to create a seller profile before creating listings.');
      }
    } catch (err) {
      setError('Failed to load profile. Please try again.');
    } finally {
      setCheckingProfile(false);
    }
  };

  const loadListing = async () => {
    try {
      const listing = await listingsAPI.getListing(parseInt(id));
      setFormData({
        title: listing.title || '',
        description: listing.description || '',
        species: listing.species || '',
        breed: listing.breed || '',
        ageMonths: listing.ageMonths || 0,
        price: listing.price?.toString() || '',
        mother: listing.mother?.toString() || '',
        father: listing.father?.toString() || '',
      });
    } catch (err) {
      setError('Failed to load listing');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    // Check seller profile for new listings
    if (!isEdit && !profile?.shopName) {
      setError('You need to create a seller profile before creating listings.');
      return;
    }

    setLoading(true);

    try {
      const priceValue = parseFloat(formData.price);
      if (isNaN(priceValue) || priceValue < 0) {
        setError('Please enter a valid price');
        setLoading(false);
        return;
      }

      const data = {
        title: formData.title,
        description: formData.description,
        species: formData.species,
        breed: formData.breed || null,
        ageMonths: parseInt(formData.ageMonths),
        price: priceValue.toString(), // Send as string for BigDecimal
        mother: formData.mother ? parseInt(formData.mother) : null,
        father: formData.father ? parseInt(formData.father) : null,
      };

      if (isEdit) {
        await listingsAPI.updateListing(parseInt(id), data);
      } else {
        const created = await listingsAPI.createListing(data);
        navigate(`/listings/${created.listingsId}/photos`);
        return;
      }
      navigate('/listings');
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to save listing';
      
      // Provide more helpful error messages
      if (errorMessage.includes('seller') || errorMessage.includes('does not exist')) {
        setError('You need to create a seller profile before creating listings. Please create one in your profile page.');
      } else {
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  if (checkingProfile && !isEdit) {
    return (
      <ProtectedRoute>
        <div style={{ maxWidth: '600px', margin: '0 auto' }}>
          <div>Checking profile...</div>
        </div>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h2>{isEdit ? 'Edit' : 'Create'} Listing</h2>
        {error && (
          <div style={{ 
            color: 'red', 
            marginBottom: '1rem', 
            padding: '1rem', 
            backgroundColor: '#ffe6e6', 
            borderRadius: '4px',
            border: '1px solid #ff9999'
          }}>
            {error}
            {error.includes('seller profile') && !isEdit && (
              <div style={{ marginTop: '0.5rem' }}>
                <Link to="/profile" style={{ color: '#3498db', textDecoration: 'underline' }}>
                  Go to Profile to create seller profile
                </Link>
              </div>
            )}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Title:</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              required
              disabled={!isEdit && !profile?.shopName}
              style={{ 
                width: '100%', 
                padding: '0.5rem', 
                fontSize: '1rem',
                opacity: (!isEdit && !profile?.shopName) ? 0.6 : 1,
                cursor: (!isEdit && !profile?.shopName) ? 'not-allowed' : 'text'
              }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Description:</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              required
              rows="5"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Species:</label>
            <input
              type="text"
              value={formData.species}
              onChange={(e) => setFormData({ ...formData, species: e.target.value })}
              required
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Breed (optional):</label>
            <input
              type="text"
              value={formData.breed}
              onChange={(e) => setFormData({ ...formData, breed: e.target.value })}
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Age (months):</label>
            <input
              type="number"
              value={formData.ageMonths}
              onChange={(e) => setFormData({ ...formData, ageMonths: e.target.value })}
              required
              min="0"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Price:</label>
            <input
              type="number"
              step="0.01"
              value={formData.price}
              onChange={(e) => setFormData({ ...formData, price: e.target.value })}
              required
              min="0"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Mother ID (optional):</label>
            <input
              type="number"
              value={formData.mother}
              onChange={(e) => setFormData({ ...formData, mother: e.target.value })}
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Father ID (optional):</label>
            <input
              type="number"
              value={formData.father}
              onChange={(e) => setFormData({ ...formData, father: e.target.value })}
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <button
            type="submit"
            disabled={loading || (!isEdit && !profile?.shopName)}
            style={{
              width: '100%',
              padding: '0.75rem',
              backgroundColor: (!isEdit && !profile?.shopName) ? '#95a5a6' : '#3498db',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              fontSize: '1rem',
              cursor: (loading || (!isEdit && !profile?.shopName)) ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Saving...' : isEdit ? 'Update Listing' : 'Create Listing'}
          </button>
        </form>
      </div>
    </ProtectedRoute>
  );
};

