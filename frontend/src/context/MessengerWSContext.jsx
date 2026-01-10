import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from './AuthContext';

const MessengerWSContext = createContext(null);

export const useMessengerWS = () => {
  const ctx = useContext(MessengerWSContext);
  if (!ctx) throw new Error('useMessengerWS must be used within MessengerWSProvider');
  return ctx;
};

export const MessengerWSProvider = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const [connected, setConnected] = useState(false);
  const [unreadChatIds, setUnreadChatIds] = useState(() => new Set());
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const shouldReconnectRef = useRef(true);
  const listenersRef = useRef(new Set());

  const subscribe = (handler) => {
    listenersRef.current.add(handler);
    return () => listenersRef.current.delete(handler);
  };

  const emit = (msg) => {
    for (const fn of listenersRef.current) {
      try {
        fn(msg);
      } catch (e) {
        // ignore listener errors
      }
    }
  };

  const connect = () => {
    const userId = user?.id;
    if (!userId) return;

    if (wsRef.current && (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING)) {
      return;
    }

    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    // Один WS на приложение (messenger1 напрямую)
    const wsUrl = `ws://localhost:8081/ws/chat?userId=${userId}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data?.error) return;
        emit(data);
      } catch (_) {
        // ignore
      }
    };

    ws.onerror = () => {
      setConnected(false);
    };

    ws.onclose = () => {
      setConnected(false);
      if (wsRef.current === ws) wsRef.current = null;
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = setTimeout(() => {
        if (shouldReconnectRef.current && isAuthenticated()) connect();
      }, 1500);
    };
  };

  useEffect(() => {
    if (!isAuthenticated() || !user?.id) return;
    shouldReconnectRef.current = true;
    connect();
    return () => {
      shouldReconnectRef.current = false;
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (wsRef.current) {
        try { wsRef.current.close(); } catch (_) {}
        wsRef.current = null;
      }
      setConnected(false);
    };
  }, [user?.id]);

  const send = (payload) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return false;
    try {
      wsRef.current.send(JSON.stringify(payload));
      return true;
    } catch (_) {
      return false;
    }
  };

  const markChatUnread = (chatId) => {
    const id = Number(chatId);
    if (!Number.isFinite(id)) return;
    setUnreadChatIds((prev) => {
      const next = new Set(prev);
      next.add(id);
      return next;
    });
  };

  const markChatRead = (chatId) => {
    const id = Number(chatId);
    if (!Number.isFinite(id)) return;
    setUnreadChatIds((prev) => {
      if (!prev.has(id)) return prev;
      const next = new Set(prev);
      next.delete(id);
      return next;
    });
  };

  const setUnreadFromChats = (chats) => {
    const next = new Set();
    for (const c of chats || []) {
      const id = Number(c?.id);
      const unread = Number(c?.unreadCount || 0);
      if (Number.isFinite(id) && unread > 0) next.add(id);
    }
    setUnreadChatIds(next);
  };

  const value = useMemo(() => ({
    connected,
    subscribe,
    send,
    // unread indicator
    hasUnread: unreadChatIds.size > 0,
    unreadChatIds,
    markChatUnread,
    markChatRead,
    setUnreadFromChats,
  }), [connected, unreadChatIds]);

  return <MessengerWSContext.Provider value={value}>{children}</MessengerWSContext.Provider>;
};


