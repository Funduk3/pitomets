import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { userAPI } from '../api/user';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { citiesAPI } from '../api/cities';
import { metroAPI } from '../api/metro';

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
  const [cityQuery, setCityQuery] = useState('');
  const [cities, setCities] = useState([]);
  const [cityId, setCityId] = useState(null);
  const [cityLoading, setCityLoading] = useState(false);
  const [cityHasMetro, setCityHasMetro] = useState(false);
  const [metroQuery, setMetroQuery] = useState('');
  const [metroStations, setMetroStations] = useState([]);
  const [metroId, setMetroId] = useState(null);
  const [metroLoading, setMetroLoading] = useState(false);
  const cityRef = useRef(null);
  const metroRef = useRef(null);

  useEffect(() => {
    if (!isEdit) {
      checkSellerProfile();
    } else {
      loadListing();
    }
  }, [id]);

  useEffect(() => {
    if (cityQuery.length < 2) {
      setCities([]);
      return;
    }

    const timeout = setTimeout(async () => {
      try {
        setCityLoading(true);
        console.log('Запрос городов для:', cityQuery);
        const data = await citiesAPI.search(cityQuery);
        console.log('Результат:', data);
        setCities(data);
      } catch (e) {
        console.error('Ошибка запроса городов:', e);
      } finally {
        setCityLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeout);
  }, [cityQuery]);

  useEffect(() => {
    if (metroQuery.length < 2 || !cityId) {
      setMetroStations([]);
      return;
    }

    const timeout = setTimeout(async () => {
      try {
        setMetroLoading(true);
        const data = await metroAPI.search(metroQuery, cityId);
        setMetroStations(data);
      } catch (e) {
        console.error('Ошибка запроса метро:', e);
      } finally {
        setMetroLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeout);
  }, [metroQuery, cityId]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (cityRef.current && !cityRef.current.contains(event.target)) {
        setCities([]);
      }
      if (metroRef.current && !metroRef.current.contains(event.target)) {
        setMetroStations([]);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);


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

      if (listing.cityId && listing.cityTitle) {
        setCityId(listing.cityId);
        setCityQuery(listing.cityTitle);
        // если бек возвращает hasMetro в объекте listing — использовать его
        if (typeof listing.cityHasMetro !== 'undefined') {
          setCityHasMetro(listing.cityHasMetro);
        }
      }
      if (listing.metroStationId && listing.metroStationTitle) {
        setMetroId(listing.metroStationId);
        setMetroQuery(listing.metroStationTitle);
      }


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
        price: priceValue.toString(),
        mother: formData.mother ? parseInt(formData.mother) : null,
        father: formData.father ? parseInt(formData.father) : null,
        cityId,
        metroId: metroId ? parseInt(metroId) : null
      };

      if (!cityId) {
        setError('Пожалуйста, выберите город из списка.');
        setLoading(false);
        return;
      }

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
        <h2>{isEdit ? 'Изменить' : 'Создать'} Listing</h2>
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
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Заголовок:</label>
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
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Описание:</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              required
              rows="5"
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Вид:</label>
            <input
              type="text"
              value={formData.species}
              onChange={(e) => setFormData({ ...formData, species: e.target.value })}
              required
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Порода (опционально):</label>
            <input
              type="text"
              value={formData.breed}
              onChange={(e) => setFormData({ ...formData, breed: e.target.value })}
              style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Возраст (в месяцах):</label>
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
            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Цена:</label>
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
          <div style={{ marginBottom: '1rem', position: 'relative' }} ref={cityRef}>
            <label>Город</label>
            <input
                value={cityQuery}
                onChange={(e) => {
                  setCityQuery(e.target.value);
                  setCityId(null);
                  setError('');
                }}
                placeholder="Начните вводить город"
                required
                style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
            />

            {cityLoading && <div>Загрузка...</div>}

            {cities.length > 0 && (
                <div style={{
                  position: 'absolute',
                  background: 'white',
                  border: '1px solid #ddd',
                  width: '100%',
                  zIndex: 10,
                  maxHeight: '200px',
                  overflowY: 'auto'
                }}>
                  {cities.map((city) => (
                      <div
                          key={city.id}
                          style={{ padding: '0.5rem', cursor: 'pointer' }}
                          onClick={(e) => {
                            e.preventDefault();
                            setCityQuery(city.title);
                            setCityId(city.id);
                            setCityHasMetro(!!city.hasMetro);
                            setCities([]);
                            setMetroId(null);
                            setMetroQuery('');
                            setMetroStations([]);
                          }}
                      >
                        {city.title}
                      </div>
                  ))}
                </div>
            )}
          </div>

          {cityHasMetro && (
              <div style={{ marginBottom: '1rem', position: 'relative' }} ref={metroRef}>
                <label>Станция метро (опционально)</label>
                <input
                    value={metroQuery}
                    onChange={(e) => {
                      setMetroQuery(e.target.value);
                      setMetroId(null);
                    }}
                    placeholder="Начните вводить станцию метро"
                    style={{ width: '100%', padding: '0.5rem', fontSize: '1rem' }}
                />

                {metroLoading && <div>Загрузка...</div>}

                {metroStations.length > 0 && (
                    <div style={{
                      position: 'absolute',
                      background: 'white',
                      border: '1px solid #ddd',
                      width: '100%',
                      zIndex: 10,
                      maxHeight: '200px',
                      overflowY: 'auto'
                    }}>
                      {metroStations.map((s) => (
                          <div
                              key={s.id}
                              style={{ padding: '0.5rem', cursor: 'pointer' }}
                              onClick={(e) => {
                                e.preventDefault();
                                setMetroQuery(s.title);
                                setMetroId(s.id);
                                setMetroStations([]);
                              }}
                          >
                            <div style={{ fontWeight: '600' }}>{s.title}</div>
                            <div style={{ fontSize: '12px' }}>{s.line?.title}</div>
                          </div>
                      ))}
                    </div>
                )}
              </div>
          )}
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
            {loading ? 'Сохраняем...' : isEdit ? 'Обновить объявление' : 'Создать объявление'}
          </button>
        </form>
      </div>
    </ProtectedRoute>
  );
};

