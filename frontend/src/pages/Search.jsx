import {useState, useEffect, useMemo, useRef} from 'react';
import {searchAPI} from '../api/search';
import {photosAPI} from '../api/photos';
import { resolveApiUrl } from '../api/axios';
import {Link, useLocation} from 'react-router-dom';
import { citiesAPI } from '../api/cities';
import { metroAPI } from '../api/metro';
import { animalAPI } from '../api/animal';
import { GENDER_LABELS, GenderEnum } from '../util/gender';
import { AGE_LABELS, AgeEnum } from '../util/age';

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
    const [animalTypes, setAnimalTypes] = useState([]);
    const [animalTypesLoading, setAnimalTypesLoading] = useState(false);
    const [selectedTypeIds, setSelectedTypeIds] = useState([]);
    const [breedQuery, setBreedQuery] = useState('');
    const [breedSuggestions, setBreedSuggestions] = useState([]);
    const [showBreedSuggestions, setShowBreedSuggestions] = useState(false);
    const [selectedBreeds, setSelectedBreeds] = useState([]);
    const [selectedGenders, setSelectedGenders] = useState([]);
    const [selectedAges, setSelectedAges] = useState([]);
    const [showTypeBar, setShowTypeBar] = useState(false);
    const [showAgeBar, setShowAgeBar] = useState(false);
    const breedDropdownRef = useRef(null);
    const lastBreedTypeIdRef = useRef(null);

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

    useEffect(() => {
        const onKey = (e) => {
            if (e.key === 'Escape') {
                setShowSuggestions(false);
                setShowCitySuggestions(false);
                setMetroStations([]);
                setShowBreedSuggestions(false);
            }
        };

        const onClick = (e) => {
            const root = rootRef.current;
            if (!root) return;
            if (!root.contains(e.target)) {
                setShowSuggestions(false);
                setShowCitySuggestions(false);
                setMetroStations([]);
                setShowBreedSuggestions(false);
            }
        };

        document.addEventListener('keydown', onKey);
        document.addEventListener('mousedown', onClick);
        return () => {
            document.removeEventListener('keydown', onKey);
            document.removeEventListener('mousedown', onClick);
        };
    }, []);

    const selectedTypes = useMemo(
        () => animalTypes.filter((t) => selectedTypeIds.includes(t.id)),
        [animalTypes, selectedTypeIds]
    );

    const activeBreedType = useMemo(() => {
        if (selectedTypes.length !== 1) return null;
        return selectedTypes[0]?.hasBreed ? selectedTypes[0] : null;
    }, [selectedTypes]);

    useEffect(() => {
        const loadTypes = async () => {
            try {
                setAnimalTypesLoading(true);
                const data = await animalAPI.getTypes();
                setAnimalTypes(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error('Animal types load error', e);
                setAnimalTypes([]);
            } finally {
                setAnimalTypesLoading(false);
            }
        };
        loadTypes();
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
    }, [selectedCity, metroId, priceFromInput, priceToInput, selectedTypeIds, selectedBreeds, selectedGenders, selectedAges]);

    useEffect(() => {
        const nextId = activeBreedType?.id ?? null;
        if (lastBreedTypeIdRef.current !== nextId) {
            setSelectedBreeds([]);
            setBreedQuery('');
            setBreedSuggestions([]);
            setShowBreedSuggestions(false);
            lastBreedTypeIdRef.current = nextId;
        }
    }, [activeBreedType]);

    useEffect(() => {
        if (!activeBreedType) {
            setBreedQuery('');
            setBreedSuggestions([]);
            setShowBreedSuggestions(false);
            setSelectedBreeds([]);
            return;
        }
        if (breedQuery.length < 2) {
            setBreedSuggestions([]);
            setShowBreedSuggestions(false);
            return;
        }

        const timeout = setTimeout(async () => {
            try {
                const data = await animalAPI.searchBreeds(
                    breedQuery,
                    activeBreedType.id
                );
                setBreedSuggestions(Array.isArray(data) ? data : []);
                setShowBreedSuggestions(true);
            } catch (e) {
                console.error('Breed autocomplete error', e);
                setBreedSuggestions([]);
            }
        }, 300);

        return () => clearTimeout(timeout);
    }, [breedQuery, activeBreedType]);

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

        const normalize = (s) => {
            if (s == null || s === '') return null;
            return s.replace(',', '.');
        };
        const priceFromNormalized = normalize(priceFromInput);
        const priceToNormalized = normalize(priceToInput);

        const parseIf = (s) => (s == null ? null : (isNaN(Number(s)) ? NaN : s));
        const pfParsed = parseIf(priceFromNormalized);
        const ptParsed = parseIf(priceToNormalized);

        if ((pfParsed !== null && isNaN(Number(pfParsed))) || (ptParsed !== null && isNaN(Number(ptParsed)))) {
            setError('Цена должна быть числом (используйте точку как разделитель).');
            return;
        }

        if (pfParsed !== null && ptParsed !== null && Number(pfParsed) > Number(ptParsed)) {
            setError('priceFrom не может быть больше priceTo.');
            return;
        }

        setHasSearched(true);
        setLoading(true);
        setError('');

        try {
            const metroParam = (selectedCity?.hasMetro && metroId != null) ? metroId : null;
            const typesParam = selectedTypes.map((t) => t.title);

            const data = await searchAPI.searchListings(
                searchText,
                0,
                10,
                {
                    city: selectedCity?.id ?? null,
                    metro: metroParam,
                    priceFrom: pfParsed !== null ? String(pfParsed) : null,
                    priceTo: ptParsed !== null ? String(ptParsed) : null,
                    types: typesParam.length ? typesParam : null,
                    breeds: selectedBreeds.length ? selectedBreeds : null,
                    genders: selectedGenders.length ? selectedGenders : null,
                    ages: selectedAges.length ? selectedAges : null
                }
            );

            if (Array.isArray(data)) {
                setResults(data);
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

    const toggleType = (id) => {
        setSelectedTypeIds((prev) =>
            prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
        );
    };

    const toggleGender = (value) => {
        setSelectedGenders((prev) =>
            prev.includes(value) ? prev.filter((item) => item !== value) : [...prev, value]
        );
    };

    const toggleAge = (value) => {
        setSelectedAges((prev) =>
            prev.includes(value) ? prev.filter((item) => item !== value) : [...prev, value]
        );
    };

    const addBreed = (title) => {
        setSelectedBreeds((prev) => (prev.includes(title) ? prev : [...prev, title]));
        setBreedQuery('');
        setBreedSuggestions([]);
        setShowBreedSuggestions(false);
    };

    const removeBreed = (title) => {
        setSelectedBreeds((prev) => prev.filter((item) => item !== title));
    };

    const hasActiveFilters = selectedCity || metroId || priceFromInput || priceToInput || selectedTypeIds.length || selectedBreeds.length || selectedGenders.length || selectedAges.length;

    return (
        <div className="min-h-screen bg-slate-50">
            <div className="bg-gradient-to-r from-blue-600 to-blue-700 text-white py-12">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <h1 className="text-4xl font-bold mb-2">Найди своего питомца</h1>
                    <p className="text-blue-100">Поиск и фильтрация по нужным параметрам</p>
                </div>
            </div>

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" ref={rootRef}>
                <form onSubmit={handleSearch} className="space-y-6 mb-8">
                    <div className="bg-white rounded-lg shadow-sm p-6 border border-slate-200">
                        <div className="flex gap-3 mb-6">
                            <div className="flex-1 relative">
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
                                    className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-base"
                                />

                                {showSuggestions && suggestions.length > 0 && (
                                    <div ref={queryDropdownRef} className="absolute top-full left-0 right-0 mt-2 bg-white border border-slate-300 rounded-lg shadow-lg z-10 max-h-64 overflow-y-auto">
                                        {suggestions.map((s, idx) => (
                                            <div
                                                key={idx}
                                                onMouseDown={() => {
                                                    setQuery(s.title);
                                                    setShowSuggestions(false);
                                                }}
                                                className="px-4 py-3 hover:bg-slate-100 cursor-pointer border-b border-slate-100 last:border-0 transition-colors"
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
                                className="px-8 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors disabled:opacity-50 flex items-center gap-2 whitespace-nowrap"
                            >
                                {loading ? 'Ищем...' : 'Найти'}
                            </button>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                            <div className="relative">
                                <label className="block text-sm font-medium text-slate-700 mb-2">Город</label>
                                <input
                                    type="text"
                                    placeholder="Выберите город"
                                    value={cityQuery}
                                    onChange={(e) => {
                                        setCityQuery(e.target.value);
                                        setSelectedCity(null);
                                    }}
                                    onFocus={() => citySuggestions.length && setShowCitySuggestions(true)}
                                    onBlur={() => setShowCitySuggestions(false)}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />

                                {showCitySuggestions && citySuggestions.length > 0 && (
                                    <div ref={cityDropdownRef} className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-300 rounded-lg shadow-lg z-10 max-h-48 overflow-y-auto">
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
                                                className="px-3 py-2 hover:bg-slate-100 cursor-pointer border-b border-slate-100 last:border-0 transition-colors text-sm"
                                            >
                                                {city.title}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {selectedCity?.hasMetro && (
                                <div className="relative">
                                    <label className="block text-sm font-medium text-slate-700 mb-2">Метро</label>
                                    <input
                                        type="text"
                                        placeholder="Выберите станцию"
                                        value={metroQuery}
                                        onChange={(e) => {
                                            setMetroQuery(e.target.value);
                                            setMetroId(null);
                                        }}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    />

                                    {metroLoading && (
                                        <div className="absolute right-3 top-9 text-xs text-slate-400">
                                            ...
                                        </div>
                                    )}

                                    {metroStations.length > 0 && (
                                        <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-300 rounded-lg shadow-lg z-10 max-h-48 overflow-y-auto">
                                            {metroStations.map(s => (
                                                <div
                                                    key={s.id}
                                                    onMouseDown={() => {
                                                        setMetroQuery(s.title);
                                                        setMetroId(s.id);
                                                        setMetroStations([]);
                                                    }}
                                                    className="px-3 py-2 hover:bg-slate-100 cursor-pointer border-b border-slate-100 last:border-0 transition-colors text-sm flex items-center gap-2"
                                                >
                                                    {s.line?.color && (
                                                        <span
                                                            className="w-3 h-3 rounded-full flex-shrink-0"
                                                            style={{backgroundColor: s.line.color}}
                                                        />
                                                    )}
                                                    <div>
                                                        <div>{s.title}</div>
                                                        <div className="text-xs text-slate-500">{s.line?.title}</div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">Цена от</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    placeholder="мин"
                                    value={priceFromInput}
                                    onChange={(e) => setPriceFromInput(e.target.value)}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">Цена до</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    placeholder="макс"
                                    value={priceToInput}
                                    onChange={(e) => setPriceToInput(e.target.value)}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />
                            </div>

                        </div>

                        <div className="flex flex-wrap gap-6 justify-between">
                            <div className="min-w-[220px]">
                                <button
                                    type="button"
                                    onClick={() => setShowTypeBar(!showTypeBar)}
                                    className="flex items-center gap-2 text-sm font-medium text-slate-700 hover:text-blue-600 transition-colors"
                                >
                                    Типы животных {selectedTypeIds.length > 0 && <span className="ml-1 bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full text-xs font-semibold">{selectedTypeIds.length}</span>}
                                    <span className={`transition-transform ${showTypeBar ? 'rotate-180' : ''}`}>▾</span>
                                </button>
                                {showTypeBar && (
                                    <div className="mt-3 p-4 bg-slate-50 rounded-lg border border-slate-200 flex flex-wrap gap-3">
                                        {animalTypesLoading && <div className="text-sm text-slate-500">Загрузка типов...</div>}
                                        {!animalTypesLoading && animalTypes.length === 0 && (
                                            <div className="text-sm text-slate-500">Типы не найдены</div>
                                        )}
                                        {animalTypes.map((type) => (
                                            <label key={type.id} className="flex items-center gap-2 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedTypeIds.includes(type.id)}
                                                    onChange={() => toggleType(type.id)}
                                                    className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                                />
                                                <span className="text-sm text-slate-700">{type.title}</span>
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {activeBreedType && (
                                <div className="min-w-[260px]">
                                    <label className="block text-sm font-medium text-slate-700 mb-2">
                                        Породы ({activeBreedType.title})
                                    </label>
                                    <div className="relative">
                                        <input
                                            type="text"
                                            value={breedQuery}
                                            onChange={(e) => setBreedQuery(e.target.value)}
                                            onFocus={() => breedSuggestions.length && setShowBreedSuggestions(true)}
                                            onBlur={() => setShowBreedSuggestions(false)}
                                            placeholder="Введите породу"
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        />
                                        {showBreedSuggestions && breedSuggestions.length > 0 && (
                                            <div
                                                ref={breedDropdownRef}
                                                className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-300 rounded-lg shadow-lg z-10 max-h-48 overflow-y-auto"
                                            >
                                                {breedSuggestions.map((breed) => (
                                                    <div
                                                        key={breed.id}
                                                        onMouseDown={() => addBreed(breed.title)}
                                                        className="px-3 py-2 hover:bg-slate-100 cursor-pointer border-b border-slate-100 last:border-0 transition-colors text-sm"
                                                    >
                                                        {breed.title}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                    {selectedBreeds.length > 0 && (
                                        <div className="mt-3 flex flex-wrap gap-2">
                                            {selectedBreeds.map((breed) => (
                                                <span
                                                    key={breed}
                                                    className="inline-flex items-center gap-2 px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium"
                                                >
                                                    {breed}
                                                    <button
                                                        type="button"
                                                        onClick={() => removeBreed(breed)}
                                                        className="hover:text-blue-900 transition-colors"
                                                    >
                                                        ×
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="min-w-[220px]">
                                <label className="block text-sm font-medium text-slate-700 mb-3">Пол</label>
                                <div className="flex flex-wrap gap-3">
                                    {Object.values(GenderEnum).filter((g) => g !== GenderEnum.ANY).map((gender) => (
                                        <label key={gender} className="flex items-center gap-2 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={selectedGenders.includes(gender)}
                                                onChange={() => toggleGender(gender)}
                                                className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                            />
                                            <span className="text-sm text-slate-700">{GENDER_LABELS[gender] ?? gender}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>

                            <div className="min-w-[220px]">
                                <button
                                    type="button"
                                    onClick={() => setShowAgeBar(!showAgeBar)}
                                    className="flex items-center gap-2 text-sm font-medium text-slate-700 hover:text-blue-600 transition-colors"
                                >
                                    Возраст {selectedAges.length > 0 && <span className="ml-1 bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full text-xs font-semibold">{selectedAges.length}</span>}
                                    <span className={`transition-transform ${showAgeBar ? 'rotate-180' : ''}`}>▾</span>
                                </button>
                                {showAgeBar && (
                                    <div className="mt-3 p-4 bg-slate-50 rounded-lg border border-slate-200 flex flex-wrap gap-3">
                                        {Object.entries(AgeEnum).map(([key, value]) => (
                                            <label key={key} className="flex items-center gap-2 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedAges.includes(key)}
                                                    onChange={() => toggleAge(key)}
                                                    className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                                />
                                                <span className="text-sm text-slate-700">{AGE_LABELS[value] ?? key}</span>
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        {hasActiveFilters && (
                            <div className="mt-6 pt-4 border-t border-slate-200 flex items-center gap-2">
                                <span className="text-sm text-slate-600">Активные фильтры:</span>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setSelectedCity(null);
                                        setCityQuery('');
                                        setMetroQuery('');
                                        setMetroId(null);
                                        setPriceFromInput('');
                                        setPriceToInput('');
                                        setSelectedTypeIds([]);
                                        setSelectedBreeds([]);
                                        setSelectedGenders([]);
                                        setSelectedAges([]);
                                    }}
                                    className="text-sm text-blue-600 hover:text-blue-700 font-medium underline"
                                >
                                    Очистить все
                                </button>
                            </div>
                        )}
                    </div>
                </form>

                {error && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                        <div className="text-red-600 font-semibold">Ошибка</div>
                        <div className="text-red-700">{error}</div>
                    </div>
                )}

                {hasSearched && loading && (
                    <div className="flex justify-center items-center py-12">
                        <div className="text-center">
                            <div className="text-blue-600 mx-auto mb-4">Загрузка...</div>
                            <p className="text-slate-600">Поиск в процессе...</p>
                        </div>
                    </div>
                )}

                {hasSearched && !loading && results.length === 0 && !error && (
                    <div className="text-center py-12">
                        <div className="text-6xl mb-4">🔍</div>
                        <h3 className="text-xl font-semibold text-slate-700 mb-2">Ничего не найдено</h3>
                        <p className="text-slate-600">По запросу "{query}" результатов не найдено. Попробуйте другие параметры поиска.</p>
                    </div>
                )}

                {results.length > 0 && (
                    <div>
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-2xl font-bold text-slate-800">Результаты поиска</h2>
                            <span className="text-sm text-slate-600">Найдено: <span className="font-semibold text-slate-800">{results.length}</span></span>
                        </div>

                        <div className="listings-grid">
                            {results.map((listing) => {
                                const listingPhotos = listingsPhotos[listing.id] || [];
                                const firstPhoto = listingPhotos[0];

                                return (
                                    <div key={listing.id} className="listing-card">
                                        {firstPhoto ? (
                                            <img
                                                src={resolveApiUrl(firstPhoto)}
                                                alt="Listing cover"
                                                className="listing-image"
                                            />
                                        ) : (
                                            <div className="listing-placeholder">
                                                Нет фото
                                            </div>
                                        )}
                                        <div className="listing-content">
                                            <h3>
                                                {listing.title || 'Без названия'}
                                            </h3>
                                            <p>
                                                {listing.description?.substring(0, 90)}
                                                {listing.description && listing.description.length > 90
                                                    ? '...'
                                                    : ''}
                                            </p>
                                            <p>
                                                <strong>Цена:</strong> <span className="tag-price">{listing.price} ₽</span>
                                            </p>
                                            <p>
                                                <strong>Город:</strong> {listing.cityTitle || '—'}
                                            </p>
                                            <Link
                                                to={`/listings/${listing.id}`}
                                                className="btn btn-secondary"
                                                style={{ display: 'inline-block', marginTop: '0.5rem', fontSize: '0.9rem' }}
                                            >
                                                Посмотреть
                                            </Link>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
