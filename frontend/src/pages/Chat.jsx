import { useState, useEffect, useLayoutEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { messengerAPI } from '../api/messenger';
import { resolveApiUrl } from '../api/axios';
import { userAPI } from '../api/user';
import { sellerAPI } from '../api/seller';
import { photosAPI } from '../api/photos';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';

export const Chat = () => {
  const { chatId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const { connected: wsConnected, subscribe, send, markChatRead, updateLastSeenMessage } = useMessengerWS();
  const [chat, setChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [otherProfile, setOtherProfile] = useState(null);
  const [listingPhotoUrl, setListingPhotoUrl] = useState(null);
  const [otherSellerProfileId, setOtherSellerProfileId] = useState(null);
  const [isTabVisible, setIsTabVisible] = useState(!document.hidden);
  const [unreadBoundaryId, setUnreadBoundaryId] = useState(null);
  const syncIntervalRef = useRef(null);
  const markReadTimeoutRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const messagesEndRef = useRef(null);
  const didInitialScrollRef = useRef(false);
  const shouldAutoScrollRef = useRef(true);

  const mergeMessagesById = (prev, incoming) => {
    if (!Array.isArray(incoming) || incoming.length === 0) return prev;
    const byId = new Map(prev.map((m) => [String(m.id), m]));
    for (const m of incoming) {
      if (m?.id == null) continue;
      byId.set(String(m.id), m);
    }
    return Array.from(byId.values()).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  };

  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;
    loadChat();
    loadMessages();
  }, [chatId]);

  useEffect(() => {
    // при переходе в другой чат хотим снова автоскроллиться вниз
    didInitialScrollRef.current = false;
    shouldAutoScrollRef.current = true;
    setUnreadBoundaryId(null);
    setLoading(true);
    setError('');
  }, [chatId]);

  useEffect(() => {
    if (!isAuthenticated() || !chatId || !user?.id) return;
    return () => {
      if (syncIntervalRef.current) {
        clearInterval(syncIntervalRef.current);
        syncIntervalRef.current = null;
      }
      if (markReadTimeoutRef.current) {
        clearTimeout(markReadTimeoutRef.current);
        markReadTimeoutRef.current = null;
      }
    };
  }, [chatId, user?.id]);

  useEffect(() => {
    const onVisibility = () => setIsTabVisible(!document.hidden);
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  // Надёжность: периодически синхронизируем последние сообщения по HTTP.
  // Это компенсирует любые краткие разрывы WS/пропуски событий: сообщение может не прийти в моменте,
  // но гарантированно появится через несколько секунд без F5.
  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;

    if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    // Когда WS подключён — синхронизируем редко (страховка).
    // Когда WS отключён — синхронизируем чаще, но только если вкладка видима.
    const intervalMs = wsConnected ? 60000 : 10000;
    if (!isTabVisible && !wsConnected) {
      // вкладка скрыта, а WS нет — не долбим сервер; догонит при фокусе/переподключении
      return;
    }

    syncIntervalRef.current = setInterval(async () => {
      try {
        const data = await messengerAPI.getChatMessages(parseInt(chatId));
        setMessages((prev) => mergeMessagesById(prev, data));
        // Регистрируем последнее видимое сообщение
        if (data && data.length > 0) {
          const lastMessage = data[data.length - 1];
          if (lastMessage?.id != null) {
            updateLastSeenMessage(parseInt(chatId), lastMessage.id);
          }
        }
        // Если пользователь находится в чате, считаем всё полученное прочитанным
        scheduleMarkRead();
      } catch (_) {
        // ignore
      }
    }, intervalMs);

    return () => {
      if (syncIntervalRef.current) {
        clearInterval(syncIntervalRef.current);
        syncIntervalRef.current = null;
      }
    };
  }, [chatId, wsConnected, isTabVisible]);

  useEffect(() => {
    if (shouldAutoScrollRef.current) {
      scrollToBottom();
    }
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const scrollToBottomInstant = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
  };

  const updateAutoScrollState = () => {
    const el = messagesContainerRef.current;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    shouldAutoScrollRef.current = distanceToBottom < 200; // px
  };

  const loadChat = async () => {
    try {
      const data = await messengerAPI.getChat(parseInt(chatId));
      setChat(data);
    } catch (err) {
      console.error('Failed to load chat:', err);
      setError('Failed to load chat');
    }
  };

  useEffect(() => {
    const otherUserId = chat?.user1Id === user?.id ? chat?.user2Id : chat?.user1Id;
    if (!otherUserId) return;
    let cancelled = false;

    (async () => {
      try {
        const profile = await userAPI.getUserProfile(otherUserId);
        if (!cancelled) {
          setOtherProfile(profile);
        }
      } catch (e) {
        if (!cancelled) setOtherProfile(null);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id]);

  useEffect(() => {
    const otherUserId = chat?.user1Id === user?.id ? chat?.user2Id : chat?.user1Id;
    if (!otherUserId) return;
    if (otherProfile?.sellerProfileId) {
      setOtherSellerProfileId(otherProfile.sellerProfileId);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const sellerProfile = await sellerAPI.getSellerProfile(otherUserId);
        if (!cancelled) {
          setOtherSellerProfileId(sellerProfile?.id ?? null);
        }
      } catch (_) {
        if (!cancelled) setOtherSellerProfileId(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id, otherProfile?.sellerProfileId]);

  useEffect(() => {
    if (!chat?.listingId) {
      setListingPhotoUrl(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const data = await photosAPI.getListingPhotos(chat.listingId);
        const first = data?.photos?.[0];
        if (!cancelled) {
          if (first) {
            const url = resolveApiUrl(first);
            setListingPhotoUrl(url);
          } else {
            setListingPhotoUrl(null);
          }
        }
      } catch (err) {
        if (!cancelled) setListingPhotoUrl(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [chat?.listingId]);

  const loadMessages = async () => {
    try {
      const data = await messengerAPI.getChatMessages(parseInt(chatId));
      setMessages((prev) => mergeMessagesById(prev, data));

      // Запоминаем "с какого места" были непрочитанные на момент входа в чат
      const me = Number(user?.id);
      const firstUnread = (data || []).find(
        (m) => m?.id != null && Number(m.senderId) !== me && m.isRead === false
      );
      setUnreadBoundaryId(firstUnread?.id != null ? String(firstUnread.id) : null);

      // Регистрируем последнее видимое сообщение для синхронизации
      if (data && data.length > 0) {
        const lastMessage = data[data.length - 1];
        if (lastMessage?.id != null) {
          updateLastSeenMessage(parseInt(chatId), lastMessage.id);
        }
      }

      // Скролл вниз делаем через useLayoutEffect (без видимого "прыжка"), тут ничего не делаем

      await messengerAPI.markMessagesAsRead(parseInt(chatId));
      // гасим глобальный индикатор непрочитанных для этого чата сразу, без refresh
      markChatRead(parseInt(chatId));
    } catch (err) {
      console.error('Failed to load messages:', err);
      setError('Failed to load messages');
    } finally {
      setLoading(false);
    }
  };

  // При первом открытии чата (или смене chatId) показываем НИЗ без видимого скролла.
  // useLayoutEffect выполняется до paint, поэтому пользователь не увидит "прыжок".
  useLayoutEffect(() => {
    if (!messagesContainerRef.current) return;
    if (loading) return;
    if (didInitialScrollRef.current) return;

    const el = messagesContainerRef.current;
    el.scrollTop = el.scrollHeight;
    didInitialScrollRef.current = true;
  }, [loading, chatId, messages.length]);

  const scheduleMarkRead = () => {
    if (!chatId) return;
    if (markReadTimeoutRef.current) clearTimeout(markReadTimeoutRef.current);
    markReadTimeoutRef.current = setTimeout(async () => {
      try {
        await messengerAPI.markMessagesAsRead(parseInt(chatId));
        markChatRead(parseInt(chatId));
      } catch (_) {
        // ignore
      }
    }, 250);
  };

  // Realtime: слушаем общие WS-сообщения и добавляем в текущий чат
  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;

    const unsubscribe = subscribe((message) => {
      // read receipt: помечаем мои сообщения как прочитанные
      if (message?.type === 'read_receipt') {
        const currentChatId = parseInt(chatId);
        if (Number(message.chatId) !== currentChatId) return;
        // если я не отправитель — мне не надо ничего менять
        setMessages((prev) =>
          prev.map((m) => (Number(m.senderId) === Number(user?.id) ? { ...m, isRead: true } : m))
        );
        return;
      }

      // обычное сообщение
      if (!message?.id || !message?.chatId) return;
      const currentChatId = parseInt(chatId);
      if (Number(message.chatId) !== currentChatId) return;

      setMessages((prev) => mergeMessagesById(prev, [message]));
      // Регистрируем последнее видимое сообщение
      updateLastSeenMessage(currentChatId, message.id);
      if (message?.senderId != null && Number(message.senderId) !== Number(user?.id)) {
        scheduleMarkRead();
      }
      scrollToBottom();
    });

    return unsubscribe;
  }, [chatId, user?.id, subscribe]);

  const sendMessage = async () => {
    if (!newMessage.trim()) return;

    try {
      const message = {
        type: 'send_message',
        chatId: parseInt(chatId),
        content: newMessage.trim(),
      };

      if (send(message)) {
        setNewMessage('');
      } else {
        // Fallback: отправка через HTTP
        await messengerAPI.createMessage(parseInt(chatId), newMessage.trim());
        setNewMessage('');
        loadMessages();
      }
    } catch (err) {
      console.error('Failed to send message:', err);
      alert('Failed to send message');
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!isAuthenticated()) {
    return <div>Войдите, чтобы увидеть чаты</div>;
  }

  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!chat) return <div>Chat not found</div>;

  const otherUserId = chat.user1Id === user?.id ? chat.user2Id : chat.user1Id;
  const otherName = otherProfile?.shopName || otherProfile?.fullName || `Пользователь #${otherUserId}`;
  const isSeller = Boolean(user?.isSeller);
  const resolvedSellerProfileId = otherProfile?.sellerProfileId ?? otherSellerProfileId;
  const profileLink = resolvedSellerProfileId
    ? `/seller/profile/view/${resolvedSellerProfileId}`
    : `/user/profile/${otherUserId}`;
  const listingTitle = chat?.listingTitle || 'Объявление';
  const listingLink = chat?.listingId ? `/listings/${chat.listingId}` : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 200px)' }}>
      <div style={{ padding: '1rem', borderBottom: '1px solid #ddd', backgroundColor: '#f9f9f9', display: 'flex', alignItems: 'center', gap: '1rem' }}>
        {listingPhotoUrl && (
          <img 
            src={listingPhotoUrl} 
            alt="Listing" 
            style={{ 
              width: '56px', 
              height: '56px', 
              borderRadius: '8px', 
              objectFit: 'cover',
              border: '2px solid #ddd'
            }} 
          />
        )}
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            {isSeller ? (
              <>
                <Link
                  to={profileLink}
                  style={{
                    textDecoration: 'none',
                    color: '#3498db',
                    fontSize: '1.2rem',
                    fontWeight: 'bold'
                  }}
                >
                  {otherName}
                </Link>
                {listingLink ? (
                  <Link
                    to={listingLink}
                    style={{
                      textDecoration: 'none',
                      color: '#666',
                      fontSize: '0.95rem'
                    }}
                  >
                    {listingTitle}
                  </Link>
                ) : (
                  <div style={{ fontSize: '0.95rem', color: '#666' }}>{listingTitle}</div>
                )}
              </>
            ) : (
              <>
                {listingLink ? (
                  <Link
                    to={listingLink}
                    style={{
                      textDecoration: 'none',
                      color: '#3498db',
                      fontSize: '1.2rem',
                      fontWeight: 'bold'
                    }}
                  >
                    {listingTitle}
                  </Link>
                ) : (
                  <div style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{listingTitle}</div>
                )}
                <Link
                  to={profileLink}
                  style={{
                    textDecoration: 'none',
                    color: '#666',
                    fontSize: '0.95rem'
                  }}
                >
                  {otherName}
                </Link>
              </>
            )}
          </div>
          <div style={{ fontSize: '0.9rem', color: '#666' }}>
            {wsConnected ? '🟢 Подключено' : '🔴 Отключено'}
          </div>
        </div>
      </div>

      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '1rem',
        }}
        ref={messagesContainerRef}
        onScroll={updateAutoScrollState}
      >
        {messages.map((msg) => {
          const isOwn = msg.senderId === user?.id;
          const statusMark = isOwn ? (msg.isRead ? '✓✓' : '✓') : null;
          const isBoundary = unreadBoundaryId != null && String(msg.id) === String(unreadBoundaryId);
          return (
            <div
              key={msg.id}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: isOwn ? 'flex-end' : 'flex-start',
              }}
            >
              {isBoundary && (
                <div
                  style={{
                    textAlign: 'center',
                    margin: '0.5rem 0',
                    color: '#666',
                    fontSize: '0.85rem',
                    alignSelf: 'center',
                  }}
                >
                  <span
                    style={{
                      display: 'inline-block',
                      padding: '0.25rem 0.75rem',
                      borderRadius: '999px',
                      backgroundColor: '#f0f0f0',
                      border: '1px solid #e0e0e0',
                    }}
                  >
                    Непрочитанные с {new Date(msg.createdAt).toLocaleString()}
                  </span>
                </div>
              )}

              <div
                style={{
                  maxWidth: '70%',
                  width: 'fit-content',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  backgroundColor: isOwn ? '#3498db' : '#ecf0f1',
                  color: isOwn ? 'white' : 'black',
                }}
              >
                <div>{msg.content}</div>
                <div
                  style={{
                    fontSize: '0.75rem',
                    marginTop: '0.25rem',
                    opacity: 0.7,
                    display: 'flex',
                    gap: '0.5rem',
                    justifyContent: 'flex-end',
                  }}
                >
                  {statusMark && <span>{statusMark}</span>}
                  <span>{new Date(msg.createdAt).toLocaleTimeString()}</span>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      <div style={{ padding: '1rem', borderTop: '1px solid #ddd', backgroundColor: '#f9f9f9' }}>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Введите сообщение..."
            style={{
              flex: 1,
              padding: '0.75rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
            }}
          />
          <button
            onClick={sendMessage}
            disabled={!newMessage.trim()}
            style={{
              padding: '0.75rem 1.5rem',
              backgroundColor: '#27ae60',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
            }}
          >
            Отправить
          </button>
        </div>
      </div>
    </div>
  );
};
