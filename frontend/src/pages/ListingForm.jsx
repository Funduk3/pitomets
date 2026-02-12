import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { listingsAPI } from '../api/listings';
import { userAPI } from '../api/user';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { citiesAPI } from '../api/cities';
import { metroAPI } from '../api/metro';
import { animalAPI } from '../api/animal';
import { AGE_LABELS } from '../util/age';
import { GENDER_LABELS, GenderEnum } from '../util/gender';
import './listing-form.css';

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
    gender: GenderEnum.ANY,
    price: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [profile, setProfile] = useState(null);
  const [checkingProfile, setCheckingProfile] = useState(true);
  const [cityQuery, setCityQuery] = useState('');
  const [cities, setCities] = useState([]);
  const [cityId, setCityId] = useState(null);
  const [initialCityId, setInitialCityId] = useState(null);
  const [cityLoading, setCityLoading] = useState(false);
  const [cityHasMetro, setCityHasMetro] = useState(false);
  const [metroQuery, setMetroQuery] = useState('');
  const [metroStations, setMetroStations] = useState([]);
  const [metroId, setMetroId] = useState(null);
  const [initialMetroId, setInitialMetroId] = useState(null);
  const [metroLoading, setMetroLoading] = useState(false);
  const [animalTypes, setAnimalTypes] = useState([]);
  const [animalTypesLoading, setAnimalTypesLoading] = useState(false);
  const [animalTypeId, setAnimalTypeId] = useState(null);
  const [animalTypeHasBreed, setAnimalTypeHasBreed] = useState(false);
  const [breedQuery, setBreedQuery] = useState('');
  const [breeds, setBreeds] = useState([]);
  const [breedLoading, setBreedLoading] = useState(false);
  const cityRef = useRef(null);
  const metroRef = useRef(null);
  const breedRef = useRef(null);

  useEffect(() => {
    if (!isEdit) {
      checkSellerProfile();
    } else {
      loadListing();
    }
  }, [id]);

  useEffect(() => {
    const loadAnimalTypes = async () => {
      try {
        setAnimalTypesLoading(true);
        const data = await animalAPI.getTypes();
        setAnimalTypes(data);
      } catch (e) {
        console.error('Ошибка загрузки типов животных:', e);
      } finally {
        setAnimalTypesLoading(false);
      }
    };

    loadAnimalTypes();
  }, []);

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
    if (breedQuery.length < 2 || !animalTypeId) {
      setBreeds([]);
      return;
    }

    const timeout = setTimeout(async () => {
      try {
        setBreedLoading(true);
        const data = await animalAPI.searchBreeds(breedQuery, animalTypeId);
        setBreeds(data);
      } catch (e) {
        console.error('Ошибка запроса пород:', e);
      } finally {
        setBreedLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeout);
  }, [breedQuery, animalTypeId]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (cityRef.current && !cityRef.current.contains(event.target)) {
        setCities([]);
      }
      if (metroRef.current && !metroRef.current.contains(event.target)) {
        setMetroStations([]);
      }
      if (breedRef.current && !breedRef.current.contains(event.target)) {
        setBreeds([]);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    if (!cityQuery || !cities || cityId) return;
    const q = cityQuery.trim().toLowerCase();
    if (q.length < 2) return;

    // If the user typed the exact city name and it's present in suggestions, auto-select it
    const exact = cities.find(c => (c.title || '').trim().toLowerCase() === q);
    if (exact) {
      setCityQuery(exact.title);
      setCityId(Number(exact.id));
      setCityHasMetro(!!exact.hasMetro);
      setCities([]);
      setMetroId(null);
      setMetroQuery('');
      setMetroStations([]);
    }
  }, [cities, cityQuery, cityId]);

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
      console.log('ListingForm: loadListing raw response:', listing);
      setFormData({
        title: listing.title || '',
        description: listing.description || '',
        species: listing.species || '',
        breed: listing.breed || '',
        ageMonths: listing.ageMonths ?? 0,
        gender: listing.gender || GenderEnum.ANY,
        price: listing.price?.toString() || '',
      });
      setBreedQuery(listing.breed || '');

      // Restore city id/title robustly: API may return either flat fields or nested objects
      let resolvedCityId = listing.cityId ?? listing.city?.id ?? null;
      const resolvedCityTitle = listing.cityTitle ?? listing.city?.title ?? '';
      if (resolvedCityId || resolvedCityTitle) {
        // normalize id to number or null
        resolvedCityId = resolvedCityId != null ? Number(resolvedCityId) : null;
        setCityId(resolvedCityId);
        setInitialCityId(resolvedCityId);
        setCityQuery(resolvedCityTitle);
        console.log('ListingForm: restored city', { resolvedCityId, resolvedCityTitle });
        // cityHasMetro could come as listing.cityHasMetro or nested city.hasMetro
        const hasMetroFlag = typeof listing.cityHasMetro !== 'undefined' ? listing.cityHasMetro : !!listing.city?.hasMetro;
        setCityHasMetro(hasMetroFlag);
        // clear any existing autocomplete state to avoid races
        setCities([]);
      }

      // Restore metro station robustly
      let resolvedMetroId = listing.metroStationId ?? listing.metro?.id ?? null;
      const resolvedMetroTitle = listing.metroStationTitle ?? listing.metro?.title ?? '';
      if (resolvedMetroId || resolvedMetroTitle) {
        resolvedMetroId = resolvedMetroId != null ? Number(resolvedMetroId) : null;
        setMetroId(resolvedMetroId);
        setInitialMetroId(resolvedMetroId);
        setMetroQuery(resolvedMetroTitle);
        setMetroStations([]);
      }


    } catch (err) {
      setError('Failed to load listing');
    }
  };

  useEffect(() => {
    if (!animalTypes.length || animalTypeId || !formData.species) return;
    const match = animalTypes.find((t) => t.title === formData.species);
    if (match) {
      setAnimalTypeId(match.id);
      setAnimalTypeHasBreed(!!match.hasBreed);
    }
  }, [animalTypes, animalTypeId, formData.species]);

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

      // Before building payload ensure we resolved cityId from typed input if possible
      // Use a local variable so state updates don't block constructing the payload
      let resolvedCityId = cityId;
      if ((resolvedCityId == null) && (cityQuery || '').trim().length >= 2) {
        const q = cityQuery.trim();
        // try to match among already loaded cities first
        const localMatch = cities.find(c => (c.title || '').trim().toLowerCase() === q.toLowerCase());
        if (localMatch) {
          resolvedCityId = Number(localMatch.id);
          // update local UI state for consistency
          setCityId(resolvedCityId);
          setCityHasMetro(!!localMatch.hasMetro);
        } else {
          // fallback: ask server for exact match quickly
          try {
            const data = await citiesAPI.search(q);
            if (Array.isArray(data) && data.length > 0) {
              const exact = data.find(c => (c.title || '').trim().toLowerCase() === q.toLowerCase());
              if (exact) {
                resolvedCityId = Number(exact.id);
                setCityId(resolvedCityId);
                setCityHasMetro(!!exact.hasMetro);
                setCityQuery(exact.title);
              }
            }
          } catch (e) {
            console.error('City resolve before submit failed:', e);
          }
        }
      }

      // Build payloads differently for create vs update because backend expects different DTOs
      if (isEdit) {
        // UpdateListingRequest fields are optional — include only fields that are meaningful.
        const updateData = {
          description: formData.description || null,
          species: formData.species || null,
          breed: animalTypeHasBreed ? (formData.breed || null) : null,
          ageMonths: Number(formData.ageMonths),
          gender: formData.gender || null,
          price: priceValue,
          isArchived: null,
          title: formData.title || null,
          mother: null,
          father: null,
        };

        // Only include city/metroStation if we have values — otherwise omit to avoid overwriting
        // Prefer resolvedCityId (current typed/selected), fallback to initial ones loaded from server
        if (resolvedCityId != null) updateData.city = Number(resolvedCityId);
        else if (initialCityId != null) updateData.city = Number(initialCityId);

        if (metroId) updateData.metroStation = Number(metroId);
        else if (initialMetroId != null) updateData.metroStation = Number(initialMetroId);
        console.log('ListingForm: sending update payload', updateData);
        await listingsAPI.updateListing(parseInt(id), updateData);
      } else {
        const createData = {
           title: formData.title,
           description: formData.description,
           species: formData.species,
           breed: animalTypeHasBreed ? (formData.breed || null) : null,
           ageMonths: Number(formData.ageMonths),
           gender: formData.gender,
           price: priceValue,
           cityId: cityId != null ? Number(cityId) : null,
           metroId: metroId ? Number(metroId) : null
         };

        if (!createData.cityId) {
          setError('Пожалуйста, выберите город из списка.');
          setLoading(false);
          return;
        }

        console.log('ListingForm: sending create payload', createData);
        const created = await listingsAPI.createListing(createData);
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

  // Try to resolve typed city on blur (accept exact match ignoring case)
  const handleCityBlur = async () => {
    // if already selected - nothing to do
    if (cityId) return;
    const q = (cityQuery || '').trim();
    if (q.length < 2) return;

    // try to match among already loaded cities first
    const match = cities.find(c => (c.title || '').trim().toLowerCase() === q.toLowerCase());
    if (match) {
      setCityId(Number(match.id));
      setCityHasMetro(!!match.hasMetro);
      setCities([]);
      return;
    }

    // fallback: ask server for exact match quickly
    try {
      const data = await citiesAPI.search(q);
      if (Array.isArray(data) && data.length > 0) {
        const exact = data.find(c => (c.title || '').trim().toLowerCase() === q.toLowerCase());
        if (exact) {
          setCityId(Number(exact.id));
          setCityHasMetro(!!exact.hasMetro);
          setCityQuery(exact.title);
          setCities([]);
        }
      }
    } catch (e) {
      // ignore silently
      console.error('City blur lookup failed:', e);
    }
  };

  if (checkingProfile && !isEdit) {
    return (
      <ProtectedRoute>
        <div className="lf-form">
          <div className="lf-card">
            <div>Checking profile...</div>
          </div>
        </div>
      </ProtectedRoute>
    );
  }

  // Determine whether to show metro input: show when selected/initial city has metro, or there's already a metro selected
  const showMetro = cityHasMetro || metroId != null || initialMetroId != null;

  return (
    <ProtectedRoute>
      <div className="lf-form">
        <div className="lf-card">
          <h2>{isEdit ? 'Изменить' : 'Создать'} объявление</h2>
          {error && (
            <div className="error-box" role="alert" aria-live="assertive">
              {error}
              {error.includes('seller profile') && !isEdit && (
                <div style={{ marginTop: '0.5rem' }}>
                  <Link to="/profile" style={{ color: 'var(--color-accent)', textDecoration: 'underline' }}>
                    Go to Profile to create seller profile
                  </Link>
                </div>
              )}
            </div>
          )}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="listing-title" className="form-label">Заголовок:</label>
              <input
                id="listing-title"
                className="form-input"
                type="text"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                required
                disabled={!isEdit && !profile?.shopName}
                aria-required="true"
                aria-invalid={false}
              />
            </div>
            <div className="form-group">
              <label htmlFor="listing-description" className="form-label">Описание:</label>
              <textarea
                id="listing-description"
                className="form-input"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                required
                rows="5"
              />
            </div>
            <div className="form-group">
              <label htmlFor="listing-species" className="form-label">Вид:</label>
              <select
                id="listing-species"
                className="form-input"
                value={animalTypeId ?? ''}
                onChange={(e) => {
                  const nextId = e.target.value ? Number(e.target.value) : null;
                  const selected = animalTypes.find((t) => t.id === nextId);
                  setAnimalTypeId(nextId);
                  setAnimalTypeHasBreed(!!selected?.hasBreed);
                  setFormData({
                    ...formData,
                    species: selected?.title || '',
                    breed: ''
                  });
                  setBreedQuery('');
                  setBreeds([]);
                }}
                required
              >
                <option value="" disabled>
                  {animalTypesLoading ? 'Загрузка типов...' : 'Выберите вид'}
                </option>
                {animalTypes.slice(0, 10).map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.title}
                  </option>
                ))}
              </select>
            </div>
            {animalTypeHasBreed && (
              <div className="form-group" ref={breedRef}>
                <label htmlFor="listing-breed" className="form-label">Порода (опционально):</label>
                <input
                  id="listing-breed"
                  className="form-input"
                  type="text"
                  value={breedQuery}
                  onChange={(e) => {
                    setBreedQuery(e.target.value);
                    setFormData({ ...formData, breed: e.target.value });
                  }}
                  placeholder="Начните вводить породу"
                  aria-autocomplete="list"
                  aria-controls="breed-listbox"
                />

                {breedLoading && <div>Загрузка...</div>}

                {breeds.length > 0 && (
                  <div id="breed-listbox" role="listbox" className="autocomplete-dropdown">
                    {breeds.map((breed) => (
                      <div
                        key={breed.id}
                        className="autocomplete-item"
                        role="option"
                        tabIndex={0}
                        onClick={(e) => {
                          e.preventDefault();
                          setBreedQuery(breed.title);
                          setFormData({ ...formData, breed: breed.title });
                          setBreeds([]);
                        }}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            setBreedQuery(breed.title);
                            setFormData({ ...formData, breed: breed.title });
                            setBreeds([]);
                          }
                        }}
                      >
                        {breed.title}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
            <div className="form-group">
              <label htmlFor="listing-age" className="form-label">Возраст:</label>
              <select
                id="listing-age"
                className="form-input"
                value={formData.ageMonths}
                onChange={(e) => setFormData({ ...formData, ageMonths: Number(e.target.value) })}
                required
              >
                {Object.entries(AGE_LABELS).map(([value, label]) => (
                  <option key={value} value={Number(value)}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="listing-gender" className="form-label">Пол:</label>
              <select
                id="listing-gender"
                className="form-input"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
                required
              >
                {Object.entries(GENDER_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="listing-price" className="form-label">Цена:</label>
              <input
                id="listing-price"
                className="form-input"
                type="number"
                step="0.01"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                required
                min="0"
                aria-invalid={false}
              />
            </div>
            <div className="form-group" ref={cityRef}>
              <label htmlFor="listing-city" className="form-label">Город</label>
              <input
                  id="listing-city"
                  aria-controls="city-listbox"
                  aria-autocomplete="list"
                  value={cityQuery}
                  onChange={(e) => {
                    setCityQuery(e.target.value);
                    setCityId(null);
                    setError('');
                  }}
                  placeholder="Начните вводить город"
                  required
                  className="form-input"
                  onBlur={handleCityBlur}
               />

              {cityLoading && <div>Загрузка...</div>}

              {cities.length > 0 && (
                  <div id="city-listbox" role="listbox" className="autocomplete-dropdown">
                    {cities.map((city) => (
                        <div
                            key={city.id}
                            className="autocomplete-item"
                            role="option"
                            tabIndex={0}
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
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.preventDefault();
                                setCityQuery(city.title);
                                setCityId(city.id);
                                setCityHasMetro(!!city.hasMetro);
                                setCities([]);
                                setMetroId(null);
                                setMetroQuery('');
                                setMetroStations([]);
                              }
                            }}
                        >
                          {city.title}
                        </div>
                    ))}
                  </div>
              )}
            </div>

            {showMetro && (
                <div className="form-group" ref={metroRef}>
                  <label htmlFor="listing-metro" className="form-label">Станция метро (опционально)</label>
                  <input
                      id="listing-metro"
                      className="form-input"
                      value={metroQuery}
                      onChange={(e) => {
                        setMetroQuery(e.target.value);
                        setMetroId(null);
                      }}
                      placeholder="Начните вводить станцию метро"
                  />

                  {metroLoading && <div>Загрузка...</div>}

                  {metroStations.length > 0 && (
                      <div role="listbox" id="metro-listbox" className="autocomplete-dropdown">
                        {metroStations.map((s) => (
                            <div
                                key={s.id}
                                className="autocomplete-item"
                                role="option"
                                tabIndex={0}
                                onClick={(e) => {
                                  e.preventDefault();
                                  setMetroQuery(s.title);
                                  setMetroId(s.id);
                                  setMetroStations([]);
                                }}
                                onKeyDown={(e) => {
                                  if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    setMetroQuery(s.title);
                                    setMetroId(s.id);
                                    setMetroStations([]);
                                  }
                                }}
                            >
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                {s.line?.color && (
                                    <span
                                        style={{
                                          width: '10px',
                                          height: '10px',
                                          borderRadius: '50%',
                                          backgroundColor: s.line.color,
                                          flexShrink: 0
                                        }}
                                    />
                                )}

                                <div>
                                  <div style={{ fontWeight: '600' }}>{s.title}</div>
                                  <div style={{ fontSize: '12px', color: '#666' }}>
                                    {s.line?.title}
                                  </div>
                                </div>
                              </div>
                            </div>
                        ))}
                      </div>
                  )}
                </div>
            )}
            <button
              type="submit"
              disabled={loading || (!isEdit && !profile?.shopName)}
              className="lf-submit btn-primary"
              aria-disabled={loading || (!isEdit && !profile?.shopName)}
            >
              {loading ? 'Сохраняем...' : isEdit ? 'Обновить объявление' : 'Создать объявление'}
            </button>
          </form>
        </div>
      </div>
    </ProtectedRoute>
  );
};
