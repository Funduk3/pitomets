import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../api/auth';
import { userAPI } from '../api/user';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setLoading(false);
      return;
    }

    const loadUser = async () => {
      try {
        const profile = await userAPI.getCurrentProfile();
        setUser(profile);
      } catch (err) {
        console.error('Failed to load user profile:', err);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, []);

  const login = async (email, passwordHash) => {
    try {
      const response = await authAPI.login(email, passwordHash);
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      const profile = await userAPI.getCurrentProfile();
      setUser(profile);
      return response;
    } catch (error) {
      throw error;
    }
  };

  const register = async (email, passwordHash, fullName) => {
    try {
      const response = await authAPI.register(email, passwordHash, fullName);
      return response;
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await authAPI.logout(refreshToken);
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
    }
  };

  const isAuthenticated = () => {
    return !!localStorage.getItem('accessToken');
  };

  const value = {
    user,
    setUser,
    login,
    register,
    logout,
    isAuthenticated,
    loading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

