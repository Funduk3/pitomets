import { useMemo, useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { messengerAPI } from '../api/messenger';
import { resolveApiUrl } from '../api/axios';
import { userAPI } from '../api/user';
import { photosAPI } from '../api/photos';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';

export const Chats = () => {
  const { isAuthenticated, user } = useAuth();
  const { subscribe, setUnreadFromChats, markChatUnread } = useMessengerWS();
  const [chats, setChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [profilesByUserId, setProfilesByUserId] = useState({});
  const [listingPhotosByListingId, setListingPhotosByListingId] = useState({});
  const pendingChatFetchRef = useRef(new Set());
  const pendingListingPhotoFetchRef = useRef(new Set());

  useEffect(() => {
    if (isAuthenticated()) {
      loadChats();
    }
    return () => {};
  }, [isAuthenticated]);

  // Realtime: если мы на странице "Мои чаты" и пришло сообщение, обновляем превью/бейджи
  useEffect(() => {
    if (!isAuthenticated()) return;

    const unsubscribe = subscribe((msg) => {
      // read_receipt не влияет на список чатов
      if (msg?.type === 'read_receipt') return;

      // msg = MessageResponse из WS
      if (!msg?.chatId || msg?.id == null) return;

      // Если чат ещё не в списке — подтянем его через API и добавим
      const chatId = Number(msg.chatId);
      if (!Number.isFinite(chatId)) return;
      // глобальный индикатор: появился непрочитанный чат
      if (Number(msg.senderId) !== Number(user?.id)) {
        markChatUnread(chatId);
      }
      if (!pendingChatFetchRef.current.has(chatId)) {
        pendingChatFetchRef.current.add(chatId);
        (async () => {
          try {
            const chat = await messengerAPI.getChat(chatId);
            setChats((prev) => {
              if (prev.some((c) => c.id === chatId)) return prev;
              // lastMessage может быть null, но у нас есть msg — положим его как превью
              const nextChat = {
                ...chat,
                lastMessage: msg,
                updatedAt: msg.createdAt,
                // на всякий случай инкрементнем, если это не моё сообщение
                unreadCount:
                  Number(msg.senderId) !== Number(user?.id)
                    ? Math.max(Number(chat?.unreadCount || 0), 1)
                    : Number(chat?.unreadCount || 0),
              };
              return [nextChat, ...prev];
            });

            // подтянем профиль собеседника, если нет
            const myId = user?.id;
            const otherUserId = chat.user1Id === myId ? chat.user2Id : chat.user1Id;
            if (otherUserId != null) {
              setProfilesByUserId((prev) => (prev[otherUserId] != null ? prev : prev));
              if (profilesByUserId[otherUserId] == null) {
                try {
                  const profile = await userAPI.getUserProfile(otherUserId);
                  setProfilesByUserId((prev) => ({ ...prev, [otherUserId]: profile }));
                } catch (_) {
                  setProfilesByUserId((prev) => ({ ...prev, [otherUserId]: null }));
                }
              }
            }

            if (chat.listingId != null) {
              loadListingPhoto(chat.listingId);
            }
          } catch (e) {
            // если не получилось — на всякий случай просто обновим список целиком
            // (не делаем await, чтобы не блокировать UI)
            loadChats();
          } finally {
            pendingChatFetchRef.current.delete(chatId);
          }
        })();
      }

      setChats((prev) => {
        const idx = prev.findIndex((c) => c.id === msg.chatId);
        if (idx === -1) return prev;
        const next = [...prev];
        const chat = { ...next[idx] };
        chat.lastMessage = msg;
        chat.updatedAt = msg.createdAt;

        const me = user?.id;
        if (me != null && Number(msg.senderId) !== Number(me)) {
          chat.unreadCount = Number(chat.unreadCount || 0) + 1;
        }

        next[idx] = chat;
        return next;
      });
    });

    return unsubscribe;
  }, [user?.id, subscribe]);

  const loadChats = async () => {
    try {
      const data = await messengerAPI.getUserChats();
      setChats(data);
      setUnreadFromChats(data);

      // подтягиваем профили собеседников пачкой (для отображения имени человека/магазина)
      const myId = user?.id;
      const otherIds = Array.from(
        new Set(
          (data || [])
            .map((chat) => (chat.user1Id === myId ? chat.user2Id : chat.user1Id))
            .filter((id) => id != null)
        )
      );

      const missingIds = otherIds.filter((id) => profilesByUserId[id] == null);
      if (missingIds.length) {
        const results = await Promise.all(
          missingIds.map(async (id) => {
            try {
              const profile = await userAPI.getUserProfile(id);
              return [id, profile];
            } catch (e) {
              console.warn('Failed to load profile for userId', id, e);
              return [id, null];
            }
          })
        );

        setProfilesByUserId((prev) => {
          const next = { ...prev };
          for (const [id, profile] of results) next[id] = profile;
          return next;
        });
      }

      // Подтягиваем фото объявлений
      const listingIds = Array.from(
        new Set((data || []).map((c) => c.listingId).filter((id) => id != null))
      );
      listingIds.forEach((listingId) => loadListingPhoto(listingId));
    } catch (err) {
      console.error('Failed to load chats:', err);
      setError('Failed to load chats');
    } finally {
      setLoading(false);
    }
  };

  const loadListingPhoto = async (listingId) => {
    if (listingPhotosByListingId[listingId]) return;
    if (pendingListingPhotoFetchRef.current.has(listingId)) return;
    pendingListingPhotoFetchRef.current.add(listingId);
    try {
      const data = await photosAPI.getListingPhotos(listingId);
      const first = data?.photos?.[0];
      if (first) {
        const url = resolveApiUrl(first);
        setListingPhotosByListingId((prev) => ({ ...prev, [listingId]: url }));
      } else {
        setListingPhotosByListingId((prev) => ({ ...prev, [listingId]: null }));
      }
    } catch (err) {
      console.debug('Failed to load listing photo', listingId, err);
      setListingPhotosByListingId((prev) => ({ ...prev, [listingId]: null }));
    } finally {
      pendingListingPhotoFetchRef.current.delete(listingId);
    }
  };

  const getUserDisplayName = (profile, fallbackUserId) => {
    if (!profile) return `Пользователь #${fallbackUserId}`;
    return profile.shopName || profile.fullName || `Пользователь #${fallbackUserId}`;
  };

  const formatPreview = (text, maxLen = 60) => {
    if (!text) return '';
    const cleaned = String(text).replace(/\s+/g, ' ').trim();
    if (cleaned.length <= maxLen) return cleaned;
    return `${cleaned.slice(0, maxLen)}…`;
  };

  const sortedChats = useMemo(() => {
    const arr = [...(chats || [])];
    arr.sort((a, b) => {
      const aTs = a?.lastMessage?.createdAt ? Date.parse(a.lastMessage.createdAt) : Date.parse(a?.updatedAt || 0);
      const bTs = b?.lastMessage?.createdAt ? Date.parse(b.lastMessage.createdAt) : Date.parse(b?.updatedAt || 0);
      return (bTs || 0) - (aTs || 0);
    });
    return arr;
  }, [chats]);

  if (!isAuthenticated()) {
    return <div>Войдите, чтобы увидеть чаты</div>;
  }

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <h2>Мои чаты</h2>
      {chats.length === 0 ? (
        <p>У вас пока нет чатов</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
          {sortedChats.map((chat) => {
            const otherUserId = chat.user1Id === user?.id ? chat.user2Id : chat.user1Id;
            const profile = profilesByUserId[otherUserId];
            const displayName = getUserDisplayName(profile, otherUserId);
            const listingTitle = chat.listingTitle || 'Объявление';
            const listingPhoto = chat.listingId != null ? listingPhotosByListingId[chat.listingId] : null;
            const last = chat.lastMessage;
            const lastTime = last?.createdAt ? new Date(last.createdAt).toLocaleTimeString() : null;
            const lastText = last?.content ? formatPreview(last.content) : 'Нет сообщений';
            const unreadCount = Number(chat?.unreadCount || 0);
            const isUnread = unreadCount > 0;
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
                  {listingPhoto && (
                    <img 
                      src={listingPhoto} 
                      alt="Listing" 
                      style={{ 
                        width: '60px', 
                        height: '60px', 
                        borderRadius: '8px', 
                        objectFit: 'cover',
                        border: '2px solid #ddd',
                        flexShrink: 0
                      }} 
                    />
                  )}
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'baseline' }}>
                      <strong>{listingTitle}</strong>
                      {unreadCount > 0 && (
                        <span
                          style={{
                            marginLeft: '0.25rem',
                            backgroundColor: '#e74c3c',
                            color: 'white',
                            borderRadius: '999px',
                            padding: '0.1rem 0.5rem',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            lineHeight: 1.4,
                          }}
                        >
                          {unreadCount}
                        </span>
                      )}
                    </div>
                    <div style={{ marginTop: '0.25rem', fontSize: '0.85rem', color: '#888' }}>
                      {displayName} • #{otherUserId}
                    </div>

                    <div style={{ marginTop: '0.35rem', fontSize: '0.95rem', color: '#333' }}>
                      <span style={{ fontWeight: isUnread ? 700 : 400 }}>
                        {lastText}
                      </span>
                    </div>

                    <div style={{ marginTop: '0.25rem', fontSize: '0.85rem', color: '#666' }}>
                      {lastTime ? lastTime : new Date(chat.updatedAt).toLocaleString()}
                      {isUnread ? ' • новые' : ''}
                    </div>
                  </div>
                  <span style={{ fontSize: '1.5rem', flexShrink: 0 }}>→</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
};
