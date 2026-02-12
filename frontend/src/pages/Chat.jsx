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
  const [otherAvatarUrl, setOtherAvatarUrl] = useState(null);
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
    // при переходе в другой чат хотим снова автоскролиться вниз
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

  // периодическая синхронизация сообщений (на случай разрывов WS)
  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;

    if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    const intervalMs = wsConnected ? 600000 : 10000;
    if (!isTabVisible && !wsConnected) return;

    syncIntervalRef.current = setInterval(async () => {
      try {
        const data = await messengerAPI.getChatMessages(parseInt(chatId));
        setMessages((prev) => mergeMessagesById(prev, data));
        if (data && data.length > 0) {
          const lastMessage = data[data.length - 1];
          if (lastMessage?.id != null) {
            updateLastSeenMessage(parseInt(chatId), lastMessage.id);
          }
        }
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
    if (!isAuthenticated() || !chatId) return;
    if (!wsConnected && !isTabVisible) return;
    (async () => {
      try {
        const data = await messengerAPI.getChatMessages(parseInt(chatId));
        setMessages((prev) => mergeMessagesById(prev, data));
      } catch (_) {
        // ignore
      }
    })();
  }, [chatId, wsConnected, isTabVisible]);

  useEffect(() => {
    if (shouldAutoScrollRef.current) scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const updateAutoScrollState = () => {
    const el = messagesContainerRef.current;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    shouldAutoScrollRef.current = distanceToBottom < 200;
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
        if (!cancelled) setOtherProfile(profile);
      } catch (e) {
        if (!cancelled) setOtherProfile(null);
      }
    })();
    return () => { cancelled = true; };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id]);

  useEffect(() => {
    const otherUserId = chat?.user1Id === user?.id ? chat?.user2Id : chat?.user1Id;
    if (!otherUserId) { setOtherAvatarUrl(null); return; }
    let cancelled = false;
    (async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (!token) return;
        const response = await fetch(photosAPI.getAvatarByUserId(otherUserId), { headers: { Authorization: `Bearer ${token}` } });
        if (!response.ok) { if (!cancelled) setOtherAvatarUrl(null); return; }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        if (!cancelled) setOtherAvatarUrl(url);
      } catch (_) { if (!cancelled) setOtherAvatarUrl(null); }
    })();
    return () => { cancelled = true; if (otherAvatarUrl && otherAvatarUrl.startsWith('blob:')) URL.revokeObjectURL(otherAvatarUrl); };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id]);

  useEffect(() => {
    const otherUserId = chat?.user1Id === user?.id ? chat?.user2Id : chat?.user1Id;
    if (!otherUserId) return;
    if (otherProfile?.sellerProfileId) { setOtherSellerProfileId(otherProfile.sellerProfileId); return; }
    let cancelled = false;
    (async () => {
      try {
        const sellerProfile = await sellerAPI.getSellerProfile(otherUserId);
        if (!cancelled) setOtherSellerProfileId(sellerProfile?.id ?? null);
      } catch (_) { if (!cancelled) setOtherSellerProfileId(null); }
    })();
    return () => { cancelled = true; };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id, otherProfile?.sellerProfileId]);

  useEffect(() => {
    if (!chat?.listingId) { setListingPhotoUrl(null); return; }
    let cancelled = false;
    (async () => {
      try {
        const data = await photosAPI.getListingPhotos(chat.listingId);
        const first = data?.photos?.[0];
        if (!cancelled) {
          if (first) setListingPhotoUrl(resolveApiUrl(first)); else setListingPhotoUrl(null);
        }
      } catch (err) { if (!cancelled) setListingPhotoUrl(null); }
    })();
    return () => { cancelled = true; };
  }, [chat?.listingId]);

  const loadMessages = async () => {
    try {
      const data = await messengerAPI.getChatMessages(parseInt(chatId));
      setMessages((prev) => mergeMessagesById(prev, data));
      const me = Number(user?.id);
      const firstUnread = (data || []).find((m) => m?.id != null && Number(m.senderId) !== me && m.isRead === false);
      setUnreadBoundaryId(firstUnread?.id != null ? String(firstUnread.id) : null);
      if (data && data.length > 0) {
        const lastMessage = data[data.length - 1];
        if (lastMessage?.id != null) updateLastSeenMessage(parseInt(chatId), lastMessage.id);
      }
      await messengerAPI.markMessagesAsRead(parseInt(chatId));
      markChatRead(parseInt(chatId));
    } catch (err) {
      console.error('Failed to load messages:', err);
      setError('Failed to load messages');
    } finally {
      setLoading(false);
    }
  };

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
      try { await messengerAPI.markMessagesAsRead(parseInt(chatId)); markChatRead(parseInt(chatId)); } catch (_) {}
    }, 250);
  };

  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;
    return subscribe((message) => {
      if (message?.type === 'read_receipt') {
        const currentChatId = parseInt(chatId);
        if (Number(message.chatId) !== currentChatId) return;
        setMessages((prev) => prev.map((m) => (Number(m.senderId) === Number(user?.id) ? { ...m, isRead: true } : m)));
        return;
      }
      if (!message?.id || !message?.chatId) return;
      const currentChatId = parseInt(chatId);
      if (Number(message.chatId) !== currentChatId) return;
      setMessages((prev) => mergeMessagesById(prev, [message]));
      updateLastSeenMessage(currentChatId, message.id);
      if (message?.senderId != null && Number(message.senderId) !== Number(user?.id)) scheduleMarkRead();
      scrollToBottom();
    });
  }, [chatId, user?.id, subscribe]);

  const sendMessage = async () => {
    if (!newMessage.trim()) return;
    try {
      const message = { type: 'send_message', chatId: parseInt(chatId), content: newMessage.trim() };
      if (send(message)) {
        setNewMessage('');
      } else {
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

  if (!isAuthenticated()) return <div>Войдите, чтобы увидеть чаты</div>;
  if (loading) return <div>Грузим...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!chat) return <div>Chat not found</div>;

  const otherUserId = chat.user1Id === user?.id ? chat.user2Id : chat.user1Id;
  const otherName = otherProfile?.shopName || otherProfile?.fullName || `Пользователь #${otherUserId}`;
  const isSeller = Boolean(user?.isSeller);
  const resolvedSellerProfileId = otherProfile?.sellerProfileId ?? otherSellerProfileId;
  const profileLink = resolvedSellerProfileId ? `/seller/profile/view/${resolvedSellerProfileId}` : `/user/profile/${otherUserId}`;
  const isListingChat = chat?.listingId != null;
  const listingTitle = isListingChat ? (chat?.listingTitle || 'Объявление') : otherName;
  const listingLink = isListingChat ? `/listings/${chat.listingId}` : null;

  return (
    <div className="chat-shell">
      <div className="chat-header">
        {isListingChat ? (
          listingPhotoUrl ? (
            <img src={listingPhotoUrl} alt="Listing" className="chat-avatar-lg" />
          ) : (
            <div className="chat-avatar-lg" />
          )
        ) : otherAvatarUrl ? (
          <img src={otherAvatarUrl} alt="User avatar" className="chat-avatar-round" />
        ) : (
          <div className="chat-avatar-round" />
        )}
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            {isSeller ? (
              <>
                <Link to={profileLink} style={{ textDecoration: 'none', color: '#3498db', fontSize: '1.2rem', fontWeight: 'bold' }}>{otherName}</Link>
                {isListingChat && (
                  <Link to={listingLink} style={{ textDecoration: 'none', color: '#666', fontSize: '0.95rem' }}>{listingTitle}</Link>
                )}
              </>
            ) : (
              <>
                {listingLink ? (
                  <Link to={listingLink} style={{ textDecoration: 'none', color: '#3498db', fontSize: '1.2rem', fontWeight: 'bold' }}>{listingTitle}</Link>
                ) : (
                  <Link to={profileLink} style={{ textDecoration: 'none', color: '#3498db', fontSize: '1.2rem', fontWeight: 'bold' }}>{listingTitle}</Link>
                )}
                {isListingChat && (
                  <Link to={profileLink} style={{ textDecoration: 'none', color: '#666', fontSize: '0.95rem' }}>{otherName}</Link>
                )}
              </>
            )}
          </div>
          <div className="small-muted">{wsConnected ? '🟢 Подключено' : '🔴 Отключено'}</div>
        </div>
      </div>

      <div className="chat-body" ref={messagesContainerRef} onScroll={updateAutoScrollState}>
        {messages.map((msg) => {
          const isOwn = Number(msg.senderId) === Number(user?.id);
          const statusMark = isOwn ? (msg.isRead ? '✓✓' : '✓') : null;
          const isBoundary = unreadBoundaryId != null && String(msg.id) === String(unreadBoundaryId);
          return (
            <div key={msg.id} style={{ display: 'flex', flexDirection: 'column', alignItems: isOwn ? 'flex-end' : 'flex-start' }}>
              {isBoundary && (
                <div className="small-muted" style={{ textAlign: 'center', margin: '0.5rem 0' }}>
                  <span className="card-body" style={{ display: 'inline-block', padding: '0.25rem 0.75rem', borderRadius: '999px', backgroundColor: '#f0f0f0', border: '1px solid #e0e0e0' }}>
                    Непрочитанные с {new Date(msg.createdAt).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })}
                  </span>
                </div>
              )}

              <div className={`chat-message ${isOwn ? 'own' : 'other'}`}>
                <div>{msg.content}</div>
                <div className="small-muted" style={{ fontSize: '0.75rem', marginTop: '0.25rem', display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                  {statusMark && <span className="read-mark">{statusMark}</span>}
                  <span
                    className={isOwn ? 'msg-time-own' : ''}
                    style={isOwn ? { color: 'var(--color-bg)', fontWeight: 600 } : undefined}
                  >
                    {new Date(msg.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', hour12: false })}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      <div className="chat-input-row">
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input type="text" value={newMessage} onChange={(e) => setNewMessage(e.target.value)} onKeyPress={handleKeyPress} placeholder="Введите сообщение..." className="chat-input" />
          <button onClick={sendMessage} disabled={!newMessage.trim()} className="btn btn-primary" style={{ padding: '0.75rem 1.5rem' }}>Отправить</button>
        </div>
      </div>
    </div>
  );
};

