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

  const value = useMemo(() => ({
    connected,
    subscribe,
    send,
  }), [connected]);

  return <MessengerWSContext.Provider value={value}>{children}</MessengerWSContext.Provider>;
};


