import { useState } from 'react';
import { searchAPI } from '../api/search';
import { Link } from 'react-router-dom';

export const Search = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError('');

    try {
      const data = await searchAPI.searchListings(query);
      setResults(data);
    } catch (err) {
      setError('Failed to search listings');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Search Listings</h2>
      <form onSubmit={handleSearch} style={{ marginBottom: '2rem', display: 'flex', gap: '1rem' }}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search for pets..."
          style={{ flex: 1, padding: '0.75rem', fontSize: '1rem', borderRadius: '4px', border: '1px solid #ddd' }}
        />
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
          {loading ? 'Searching...' : 'Search'}
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

      {results.length === 0 && !loading && query && !error && (
        <p>No results found for "{query}"</p>
      )}

      {results.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
          {results.map((listing) => (
            <Link
              key={listing.id}
              to={`/listings/${listing.id}`}
              style={{
                border: '1px solid #ddd',
                borderRadius: '8px',
                padding: '1rem',
                textDecoration: 'none',
                color: 'inherit',
                display: 'block'
              }}
            >
              <h3>{listing.title}</h3>
              <p>{listing.description}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

