import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { messengerAPI } from '../api/messenger';
import { userAPI } from '../api/user';
import { useAuth } from '../context/AuthContext';
import { useMessengerWS } from '../context/MessengerWSContext';

export const Chat = () => {
  const { chatId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const { connected: wsConnected, subscribe, send } = useMessengerWS();
  const [chat, setChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [otherProfile, setOtherProfile] = useState(null);
  const syncIntervalRef = useRef(null);
  const markReadTimeoutRef = useRef(null);
  const messagesEndRef = useRef(null);

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

  // Надёжность: периодически синхронизируем последние сообщения по HTTP.
  // Это компенсирует любые краткие разрывы WS/пропуски событий: сообщение может не прийти в моменте,
  // но гарантированно появится через несколько секунд без F5.
  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;

    if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    syncIntervalRef.current = setInterval(async () => {
      try {
        const data = await messengerAPI.getChatMessages(parseInt(chatId));
        setMessages((prev) => mergeMessagesById(prev, data));
        // Если пользователь находится в чате, считаем всё полученное прочитанным
        scheduleMarkRead();
      } catch (_) {
        // ignore
      }
    }, 4000);

    return () => {
      if (syncIntervalRef.current) {
        clearInterval(syncIntervalRef.current);
        syncIntervalRef.current = null;
      }
    };
  }, [chatId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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

    return () => {
      cancelled = true;
    };
  }, [chat?.id, chat?.user1Id, chat?.user2Id, user?.id]);

  const loadMessages = async () => {
    try {
      const data = await messengerAPI.getChatMessages(parseInt(chatId));
      setMessages((prev) => mergeMessagesById(prev, data));
      await messengerAPI.markMessagesAsRead(parseInt(chatId));
    } catch (err) {
      console.error('Failed to load messages:', err);
      setError('Failed to load messages');
    } finally {
      setLoading(false);
    }
  };

  const scheduleMarkRead = () => {
    if (!chatId) return;
    if (markReadTimeoutRef.current) clearTimeout(markReadTimeoutRef.current);
    markReadTimeoutRef.current = setTimeout(async () => {
      try {
        await messengerAPI.markMessagesAsRead(parseInt(chatId));
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
    return <div>Please login to view chat</div>;
  }

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!chat) return <div>Chat not found</div>;

  const otherUserId = chat.user1Id === user?.id ? chat.user2Id : chat.user1Id;
  const otherName = otherProfile?.shopName || otherProfile?.fullName || `Пользователь #${otherUserId}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 200px)' }}>
      <div style={{ padding: '1rem', borderBottom: '1px solid #ddd', backgroundColor: '#f9f9f9' }}>
        <h3>{otherName}</h3>
        <div style={{ fontSize: '0.9rem', color: '#666' }}>
          {wsConnected ? '🟢 Подключено' : '🔴 Отключено'}
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
      >
        {messages.map((msg) => {
          const isOwn = msg.senderId === user?.id;
          const statusMark = isOwn ? (msg.isRead ? '✓✓' : '✓') : null;
          return (
            <div
              key={msg.id}
              style={{
                alignSelf: isOwn ? 'flex-end' : 'flex-start',
                maxWidth: '70%',
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

