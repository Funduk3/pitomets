import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const Home = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div>
      <h1>Welcome to Pitomets</h1>
      <p>Your trusted marketplace for pets</p>
      {!isAuthenticated() && (
        <div style={{ marginTop: '2rem' }}>
          <Link
            to="/register"
            style={{
              display: 'inline-block',
              padding: '1rem 2rem',
              backgroundColor: '#3498db',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px',
              marginRight: '1rem'
            }}
          >
            Get Started
          </Link>
          <Link
            to="/search"
            style={{
              display: 'inline-block',
              padding: '1rem 2rem',
              backgroundColor: '#27ae60',
              color: 'white',
              textDecoration: 'none',
              borderRadius: '4px'
            }}
          >
            Browse Listings
          </Link>
        </div>
      )}
    </div>
  );
};

