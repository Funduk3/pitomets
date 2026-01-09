import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { messengerAPI } from '../api/messenger';
import { useAuth } from '../context/AuthContext';

export const Chat = () => {
  const { chatId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const [chat, setChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const shouldReconnectRef = useRef(true);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (!isAuthenticated() || !chatId) return;
    loadChat();
    loadMessages();
    // WS подключаем отдельным эффектом, когда точно есть user.id
  }, [chatId]);

  useEffect(() => {
    if (!isAuthenticated() || !chatId || !user?.id) return;
    shouldReconnectRef.current = true;
    connectWebSocket();
    return () => {
      shouldReconnectRef.current = false;
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [chatId, user?.id]);

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

  const loadMessages = async () => {
    try {
      const data = await messengerAPI.getChatMessages(parseInt(chatId));
      setMessages(data);
      await messengerAPI.markMessagesAsRead(parseInt(chatId));
    } catch (err) {
      console.error('Failed to load messages:', err);
      setError('Failed to load messages');
    } finally {
      setLoading(false);
    }
  };

  const connectWebSocket = () => {
    if (!user?.id) return;

    // Не плодим соединения: если уже OPEN/CONNECTING — выходим.
    // Важно: если сокет "завис" в CLOSING, лучше принудительно закрыть и создать новый,
    // иначе можно получить 2 активных сокета и дубль входящих сообщений.
    if (wsRef.current) {
      if (
        wsRef.current.readyState === WebSocket.OPEN ||
        wsRef.current.readyState === WebSocket.CONNECTING
      ) {
        return;
      }

      try {
        wsRef.current.close();
      } catch (_) {
        // ignore
      } finally {
        wsRef.current = null;
      }
    }

    // Сбрасываем отложенный reconnect, чтобы не получить параллельные коннекты
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    // WebSocket через monolit не будет работать напрямую, нужно будет проксировать
    // Пока используем прямой WebSocket к messenger1 с userId в query параметрах
    const wsUrl = `ws://localhost:8081/ws/chat?userId=${user.id}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      // Отправляем userId для аутентификации
      // В реальности нужно будет проксировать WebSocket через monolit
      console.log('WebSocket connected');
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        setMessages((prev) => {
          // Игнорируем сообщения не из текущего чата (WS шлёт по пользователю, не по чату)
          const currentChatId = parseInt(chatId);
          if (message?.chatId != null && Number(message.chatId) !== currentChatId) return prev;

          // Дедуп по id (нормализуем тип, чтобы "1" и 1 считались одинаковыми)
          const incomingId = message?.id != null ? String(message.id) : null;
          if (incomingId && prev.some((m) => m?.id != null && String(m.id) === incomingId)) {
            return prev;
          }

          // Если id нет — это не MessageResponse (ошибка/служебное) — не добавляем в ленту
          if (!incomingId) return prev;

          return [...prev, message];
        });
        scrollToBottom();
      } catch (err) {
        console.error('Failed to parse WebSocket message:', err);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setWsConnected(false);
    };

    ws.onclose = () => {
      setWsConnected(false);
      // Считаем соединение мёртвым, чтобы не копились ссылки на старые WS
      if (wsRef.current === ws) {
        wsRef.current = null;
      }
      // Попытка переподключения через 3 секунды
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      reconnectTimeoutRef.current = setTimeout(() => {
        if (shouldReconnectRef.current && isAuthenticated()) {
          connectWebSocket();
        }
      }, 3000);
    };
  };

  const sendMessage = async () => {
    if (!newMessage.trim()) return;

    try {
      const message = {
        type: 'send_message',
        chatId: parseInt(chatId),
        content: newMessage.trim(),
      };

      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify(message));
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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 200px)' }}>
      <div style={{ padding: '1rem', borderBottom: '1px solid #ddd', backgroundColor: '#f9f9f9' }}>
        <h3>Чат с пользователем #{otherUserId}</h3>
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
                }}
              >
                {new Date(msg.createdAt).toLocaleTimeString()}
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

