import {useState, useEffect, useRef} from 'react';
import {searchAPI} from '../api/search';
import {photosAPI} from '../api/photos';
import { resolveApiUrl } from '../api/axios';
import {Link, useLocation} from 'react-router-dom';
import { citiesAPI } from '../api/cities';
import { metroAPI } from '../api/metro';

export const Search = () => {
    const location = useLocation();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [listingsPhotos, setListingsPhotos] = useState({});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [hasSearched, setHasSearched] = useState(false);
    const [suggestions, setSuggestions] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [cityQuery, setCityQuery] = useState('');
    const [citySuggestions, setCitySuggestions] = useState([]);
    const [selectedCity, setSelectedCity] = useState(null);
    const [showCitySuggestions, setShowCitySuggestions] = useState(false);
    const [metroQuery, setMetroQuery] = useState('');
    const [metroStations, setMetroStations] = useState([]);
    const [metroId, setMetroId] = useState(null);
    const [metroLoading, setMetroLoading] = useState(false);
    const metroRef = useRef(null);
    const cityDropdownRef = useRef(null);
    const queryDropdownRef = useRef(null);
    const rootRef = useRef(null);
    const [priceFromInput, setPriceFromInput] = useState('');
    const [priceToInput, setPriceToInput] = useState('');

    useEffect(() => {
        if (!query.trim()) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }

        const timeout = setTimeout(async () => {
            const data = await searchAPI.autocomplete(query);
            if (Array.isArray(data)) {
                setSuggestions(data);
                setShowSuggestions(true);
            }
        }, 250);

        return () => clearTimeout(timeout);
    }, [query]);

    // Close dropdowns on outside click or Escape
    useEffect(() => {
        const onKey = (e) => {
            if (e.key === 'Escape') {
                setShowSuggestions(false);
                setShowCitySuggestions(false);
                setMetroStations([]);
            }
        };

        const onClick = (e) => {
            const root = rootRef.current;
            if (!root) return;
            if (!root.contains(e.target)) {
                setShowSuggestions(false);
                setShowCitySuggestions(false);
                setMetroStations([]);
            }
        };

        document.addEventListener('keydown', onKey);
        document.addEventListener('mousedown', onClick);
        return () => {
            document.removeEventListener('keydown', onKey);
            document.removeEventListener('mousedown', onClick);
        };
    }, []);

    useEffect(() => {
        if (hasSearched) {
            handleSearch(new Event('submit'));
        }
    }, [selectedCity]);

    useEffect(() => {
        if (
            metroQuery.length < 2 ||
            !selectedCity?.id ||
            !selectedCity?.hasMetro
        ) {
            setMetroStations([]);
            return;
        }

        const timeout = setTimeout(async () => {
            try {
                setMetroLoading(true);
                const data = await metroAPI.search(
                    metroQuery,
                    selectedCity.id
                );
                setMetroStations(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error('Metro autocomplete error', e);
                setMetroStations([]);
            } finally {
                setMetroLoading(false);
            }
        }, 300);

        return () => clearTimeout(timeout);
    }, [metroQuery, selectedCity]);


    useEffect(() => {
        if (cityQuery.length < 2) {
            setCitySuggestions([]);
            setShowCitySuggestions(false);
            return;
        }

        const timeout = setTimeout(async () => {
            try {
                const data = await citiesAPI.search(cityQuery);
                if (Array.isArray(data)) {
                    setCitySuggestions(data);
                    setShowCitySuggestions(true);
                }
            } catch (e) {
                console.error('City autocomplete error', e);
            }
        }, 300);

        return () => clearTimeout(timeout);
    }, [cityQuery]);

    useEffect(() => {
        if (hasSearched) {
            handleSearch();
        }
    }, [selectedCity, metroId, priceFromInput, priceToInput]);

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const q = params.get('q');
        if (q && q !== query) {
            setQuery(q);
            setHasSearched(true);
            handleSearch(null, q);
        }
    }, [location.search]);

    const handleSearch = async (e, overrideQuery) => {
        if (e?.preventDefault) e.preventDefault();

        const searchText = (overrideQuery ?? query).trim();
        if (!searchText) return;

        // форматируем цену: заменяем запятую на точку
        const normalize = (s) => {
            if (s == null || s === '') return null;
            return s.replace(',', '.');
        };
        const priceFromNormalized = normalize(priceFromInput);
        const priceToNormalized = normalize(priceToInput);

        // базовая валидация: если введено — должно быть число
        const parseIf = (s) => (s == null ? null : (isNaN(Number(s)) ? NaN : s));
        const pfParsed = parseIf(priceFromNormalized);
        const ptParsed = parseIf(priceToNormalized);

        if ((pfParsed !== null && isNaN(Number(pfParsed))) || (ptParsed !== null && isNaN(Number(ptParsed)))) {
            setError('Цена должна быть числом (используйте точку как разделитель).');
            return;
        }

        // если обе заданы, проверим порядок
        if (pfParsed !== null && ptParsed !== null && Number(pfParsed) > Number(ptParsed)) {
            setError('priceFrom не может быть больше priceTo.');
            return;
        }

        setHasSearched(true);
        setLoading(true);
        setError('');

        try {
            const metroParam = (selectedCity?.hasMetro && metroId != null) ? metroId : null;

            const data = await searchAPI.searchListings(
                searchText,
                0,
                10,
                {
                    city: selectedCity?.id ?? null,
                    metro: metroParam,
                    priceFrom: pfParsed !== null ? String(pfParsed) : null,
                    priceTo: ptParsed !== null ? String(ptParsed) : null
                }
            );

            if (Array.isArray(data)) {
                setResults(data);
                // загрузка фото — без изменений
                const photosPromises = data.map(async (listing) => {
                    try {
                        const photosData = await photosAPI.getListingPhotos(listing.id);
                        return {listingId: listing.id, photos: photosData.photos || []};
                    } catch (err) {
                        console.error(`Failed to load photos for listing ${listing.id}:`, err);
                        return {listingId: listing.id, photos: []};
                    }
                });

                const photosResults = await Promise.all(photosPromises);
                const photosMap = {};
                photosResults.forEach(({listingId, photos}) => {
                    photosMap[listingId] = photos;
                });
                setListingsPhotos(photosMap);
            } else {
                console.error('Unexpected response format:', data);
                setError('Invalid response format from server');
                setResults([]);
            }
        } catch (err) {
            console.error('Search error:', err);
            const errorMessage = err.response?.data?.message || err.message || 'Failed to search listings';
            setError(errorMessage);
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Поиск по объявлениям</h2>
            <form onSubmit={handleSearch} className="flex-column-gap" style={{marginBottom: '2rem', display: 'flex', gap: '1rem'}} ref={rootRef}>
                <div style={{ position: 'relative', width: '220px' }}>
                    <input
                        type="text"
                        placeholder="Город"
                        value={cityQuery}
                        onChange={(e) => {
                            setCityQuery(e.target.value);
                            setSelectedCity(null);
                        }}
                        onFocus={() => citySuggestions.length && setShowCitySuggestions(true)}
                        onBlur={() => setShowCitySuggestions(false)}
                        className="form-input"
                        style={{ width: '100%' }}
                    />

                    {showCitySuggestions && citySuggestions.length > 0 && (
                        <div ref={cityDropdownRef} className="autocomplete-dropdown" style={{ position: 'absolute' }}>
                            {citySuggestions.map(city => (
                                <div
                                    key={city.id}
                                    onMouseDown={() => {
                                        setSelectedCity(city);
                                        setCityQuery(city.title);
                                        setMetroQuery('');
                                        setMetroId(null);
                                        setMetroStations([]);
                                        setShowCitySuggestions(false);
                                    }}
                                    style={{ padding: '0.5rem', cursor: 'pointer' }}
                                >
                                    {city.title}
                                </div>
                            ))}
                        </div>
                    )}

                </div>
                {selectedCity && (
                    <div className="small-muted" style={{ marginBottom: '1rem' }}>
                        Фильтр: {selectedCity.title}
                        <span
                            onClick={() => {
                                setSelectedCity(null);
                                setCityQuery('');
                                setMetroQuery('');
                            }}
                            style={{ marginLeft: '8px', cursor: 'pointer' }}
                        >
            ✕
        </span>
                    </div>
                )}
                {selectedCity?.hasMetro && (
                    <div style={{ position: 'relative', width: '220px' }} ref={metroRef}>
                        <input
                            type="text"
                            placeholder="Метро"
                            value={metroQuery}
                            onChange={(e) => {
                                setMetroQuery(e.target.value);
                                setMetroId(null);
                            }}
                            className="form-input"
                            style={{ width: '100%' }}
                        />

                        {metroLoading && <div>Загрузка...</div>}

                        {metroStations.length > 0 && (
                            <div className="autocomplete-dropdown" style={{ position: 'absolute', maxHeight: '200px' }}>
                                 {metroStations.map(s => (
                                    <div
                                        key={s.id}
                                        onMouseDown={() => {
                                            setMetroQuery(s.title);
                                            setMetroId(s.id);
                                            setMetroStations([]);
                                        }}
                                        className="flex-column-gap"
                                        style={{ padding: '0.5rem', cursor: 'pointer', display: 'flex', gap: '8px' }}
                                    >
                                        {s.line?.color && (
                                            <span
                                                style={{
                                                    width: 10,
                                                    height: 10,
                                                    borderRadius: '50%',
                                                    backgroundColor: s.line.color
                                                }}
                                            />
                                        )}
                                        <div>
                                            <div>{s.title}</div>
                                            <div className="small-muted">
                                                {s.line?.title}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
                {/* Price filters */}
                <div style={{ display: 'flex', gap: 8, alignItems: 'center', minWidth: 220 }}>
                    <input
                        type="number"
                        step="0.01"
                        placeholder="Цена от"
                        value={priceFromInput}
                        onChange={(e) => setPriceFromInput(e.target.value)}
                        className="form-input"
                        style={{ width: '110px' }}
                    />
                    <input
                        type="number"
                        step="0.01"
                        placeholder="до"
                        value={priceToInput}
                        onChange={(e) => setPriceToInput(e.target.value)}
                        className="form-input"
                        style={{ width: '110px' }}
                    />
                </div>

                <div style={{position: "relative", flex: 1}}>
                    <input
                        type="text"
                        value={query}
                        onChange={(e) => {
                            setQuery(e.target.value);
                            setHasSearched(false);
                        }}
                        onFocus={() => suggestions.length && setShowSuggestions(true)}
                        onBlur={() => setShowSuggestions(false)}
                        placeholder="Ищем питомца..."
                        className="form-input"
                        style={{ fontSize: '1rem' }}
                    />

                    {showSuggestions && suggestions.length > 0 && (
                        <div ref={queryDropdownRef} className="autocomplete-dropdown">
                             {suggestions.map((s, idx) => (
                                <div
                                    key={idx}
                                    onMouseDown={() => {
                                        setQuery(s.title);
                                        setShowSuggestions(false);
                                    }}
                                    style={{ padding: "0.5rem 0.75rem", cursor: "pointer", borderBottom: "1px solid #eee" }}
                                >
                                    {s.title}
                                </div>
                             ))}
                         </div>
                     )}
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    className="btn btn-primary"
                    style={{ padding: '0.75rem 2rem' }}
                >
                    {loading ? 'Ищем...' : 'Найти'}
                </button>
            </form>

            {error && (
                <div className="error-box">
                    {error}
                </div>
            )}

            {hasSearched && results.length === 0 && !loading && !error && (
                <p>Ничего не найдено по запросу "{query}"</p>
            )}

            {results.length > 0 && (
                <div className="listings-grid">
                    {results.map((listing) => {
                        const listingPhotos = listingsPhotos[listing.id] || [];
                        const firstPhoto = listingPhotos[0];

                        return (
                            <Link
                                key={listing.id}
                                to={`/listings/${listing.id}`}
                                className="link-card"
                            >
                                {firstPhoto ? (
                                    <img
                                        src={resolveApiUrl(firstPhoto)}
                                        alt={listing.title}
                                        style={{ height: '200px', objectFit: 'cover', display: 'block' }}
                                    />
                                ) : (
                                    <div className="listing-placeholder">
                                        Нет фото
                                    </div>
                                )}
                                <div className="card-body">
                                    <h3 style={{ margin: '0 0 0.5rem 0' }}>{listing.title}</h3>
                                    <p className="small-muted" style={{ margin: '0.5rem 0', fontSize: '0.9rem' }}>
                                        {listing.description?.substring(0, 100)}
                                        {listing.description && listing.description.length > 100 ? '...' : ''}
                                    </p>
                                </div>
                            </Link>
                        );

                    })}
                </div>
            )}
        </div>
    );
};
