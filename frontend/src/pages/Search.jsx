import {useState, useEffect} from 'react';
import {searchAPI} from '../api/search';
import {photosAPI} from '../api/photos';
import { resolveApiUrl } from '../api/axios';
import {Link, useLocation} from 'react-router-dom';
import { citiesAPI } from '../api/cities';
import { metroAPI } from '../api/metro';
import { useRef } from 'react';

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
    const [priceFromInput, setPriceFromInput] = useState('');
    const [priceToInput, setPriceToInput] = useState('');

    useEffect(() => {
        if (!query.trim()) {
            setSuggestions([]);
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
            <form onSubmit={handleSearch} style={{marginBottom: '2rem', display: 'flex', gap: '1rem'}}>
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
                        onBlur={() => setTimeout(() => setShowCitySuggestions(false), 150)}
                        style={{ padding: '0.75rem', width: '100%' }}
                    />

                    {showCitySuggestions && citySuggestions.length > 0 && (
                        <div style={{
                            position: 'absolute',
                            top: '100%',
                            left: 0,
                            right: 0,
                            background: 'white',
                            border: '1px solid #ddd',
                            zIndex: 10
                        }}>
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

                                    style={{
                                        padding: '0.5rem',
                                        cursor: 'pointer'
                                    }}
                                >
                                    {city.title}
                                </div>
                            ))}
                        </div>
                    )}

                </div>
                {selectedCity && (
                    <div style={{ marginBottom: '1rem', fontSize: '0.9rem', color: '#555' }}>
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
                            style={{ padding: '0.75rem', width: '100%' }}
                        />

                        {metroLoading && <div>Загрузка...</div>}

                        {metroStations.length > 0 && (
                            <div style={{
                                position: 'absolute',
                                top: '100%',
                                left: 0,
                                right: 0,
                                background: 'white',
                                border: '1px solid #ddd',
                                zIndex: 10,
                                maxHeight: '200px',
                                overflowY: 'auto'
                            }}>
                                {metroStations.map(s => (
                                    <div
                                        key={s.id}
                                        onMouseDown={() => {
                                            setMetroQuery(s.title);
                                            setMetroId(s.id);
                                            setMetroStations([]);
                                        }}
                                        style={{
                                            padding: '0.5rem',
                                            cursor: 'pointer',
                                            display: 'flex',
                                            gap: '8px'
                                        }}
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
                                            <div style={{ fontSize: 12, color: '#666' }}>
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
                        style={{ padding: '0.5rem', width: '110px' }}
                    />
                    <input
                        type="number"
                        step="0.01"
                        placeholder="до"
                        value={priceToInput}
                        onChange={(e) => setPriceToInput(e.target.value)}
                        style={{ padding: '0.5rem', width: '110px' }}
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
                        onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                        placeholder="Ищем питомца..."
                        style={{width: "100%", padding: "0.75rem", fontSize: "1rem"}}
                    />

                    {showSuggestions && suggestions.length > 0 && (
                        <div style={{
                            position: "absolute",
                            top: "100%",
                            left: 0,
                            right: 0,
                            background: "white",
                            border: "1px solid #ddd",
                            borderRadius: "4px",
                            zIndex: 10
                        }}>
                            {suggestions.map((s, idx) => (
                                <div
                                    key={idx}
                                    onMouseDown={() => {
                                        setQuery(s.title);
                                        setShowSuggestions(false);
                                    }}
                                    style={{
                                        padding: "0.5rem 0.75rem",
                                        cursor: "pointer",
                                        borderBottom: "1px solid #eee"
                                    }}
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
                    style={{
                        padding: '0.75rem 2rem',
                        backgroundColor: '#3498db',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        fontSize: '1rem',
                        cursor: loading ? 'not-allowed' : 'pointer'
                    }}
                >
                    {loading ? 'Ищем...' : 'Найти'}
                </button>
            </form>

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
                </div>
            )}

            {hasSearched && results.length === 0 && !loading && !error && (
                <p>Ничего не найдено по запросу "{query}"</p>
            )}

            {results.length > 0 && (
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                    gap: '1.5rem'
                }}>
                    {results.map((listing) => {
                        const listingPhotos = listingsPhotos[listing.id] || [];
                        const firstPhoto = listingPhotos[0];

                        return (
                            <Link
                                key={listing.id}
                                to={`/listings/${listing.id}`}
                                style={{
                                    border: '1px solid #ddd',
                                    borderRadius: '8px',
                                    overflow: 'hidden',
                                    textDecoration: 'none',
                                    color: 'inherit',
                                    display: 'block',
                                    transition: 'transform 0.2s, box-shadow 0.2s',
                                    backgroundColor: 'white'
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-4px)';
                                    e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = 'none';
                                }}
                            >
                                {firstPhoto ? (
                                    <img
                                        src={resolveApiUrl(firstPhoto)}
                                        alt={listing.title}
                                        style={{
                                            width: '100%',
                                            height: '200px',
                                            objectFit: 'cover',
                                            display: 'block'
                                        }}
                                    />
                                ) : (
                                    <div style={{
                                        width: '100%',
                                        height: '200px',
                                        backgroundColor: '#f0f0f0',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        color: '#999'
                                    }}>
                                        Нет фото
                                    </div>
                                )}
                                <div style={{padding: '1rem'}}>
                                    <h3 style={{margin: '0 0 0.5rem 0'}}>{listing.title}</h3>
                                    <p style={{margin: '0.5rem 0', color: '#666', fontSize: '0.9rem'}}>
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
