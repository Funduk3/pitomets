import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { messengerAPI } from '../api/messenger';
import { useAuth } from '../context/AuthContext';

export const Chats = () => {
  const { isAuthenticated, user } = useAuth();
  const [chats, setChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated()) {
      loadChats();
    }
  }, [isAuthenticated]);

  const loadChats = async () => {
    try {
      const data = await messengerAPI.getUserChats();
      setChats(data);
    } catch (err) {
      console.error('Failed to load chats:', err);
      setError('Failed to load chats');
    } finally {
      setLoading(false);
    }
  };

  if (!isAuthenticated()) {
    return <div>Please login to view your chats</div>;
  }

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <h2>Мои чаты</h2>
      {chats.length === 0 ? (
        <p>У вас пока нет чатов</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
          {chats.map((chat) => {
            const otherUserId = chat.user1Id === user?.id ? chat.user2Id : chat.user1Id;
            return (
              <Link
                key={chat.id}
                to={`/chats/${chat.id}`}
                style={{
                  display: 'block',
                  padding: '1rem',
                  border: '1px solid #ddd',
                  borderRadius: '8px',
                  textDecoration: 'none',
                  color: 'inherit',
                  backgroundColor: '#f9f9f9'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong>Чат с пользователем #{otherUserId}</strong>
                    <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.9rem', color: '#666' }}>
                      {new Date(chat.updatedAt).toLocaleString()}
                    </p>
                  </div>
                  <span style={{ fontSize: '1.5rem' }}>→</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
};

