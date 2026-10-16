import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { NotificationEvent, Session } from '../types';
import { nextBackoffMs } from '../utils/backoff';
import { toWsUrl } from '../utils/urls';
import { getBaseUrl } from './runtime';

interface NotificationsValue {
  events: NotificationEvent[];
  connected: boolean;
  clear: () => void;
}
// React Native's WebSocket accepts a third `options` argument with headers; the DOM typings
// that TypeScript picks up here don't declare it, so describe the constructor we actually use.
type RNWebSocket = new (url: string, protocols?: string | string[] | null, options?: { headers?: Record<string, string> }) => WebSocket;

const Ctx = createContext<NotificationsValue>({ events: [], connected: false, clear: () => {} });

// Live push from notification-service, through the gateway's /ws/notifications route.
// The gateway checks the JWT on the WebSocket handshake, so the token goes in an Authorization
// header (React Native's WebSocket supports custom headers as a third constructor argument).
export function NotificationsProvider({ session, children }: { session: Session; children: React.ReactNode }) {
  const [events, setEvents] = useState<NotificationEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const attempt = useRef(0);

  useEffect(() => {
    let socket: WebSocket | null = null;
    let timer: ReturnType<typeof setTimeout> | null = null;
    let stopped = false;

    const connect = () => {
      if (stopped) return;
      const url = `${toWsUrl(getBaseUrl())}/ws/notifications?userId=${session.userId}`;
      socket = new (WebSocket as unknown as RNWebSocket)(url, null, { headers: { Authorization: `Bearer ${session.accessToken}` } });
      socket.onopen = () => { attempt.current = 0; setConnected(true); };
      socket.onmessage = (m) => {
        try {
          const data = JSON.parse(String(m.data)) as Record<string, unknown>;
          if (typeof data.type !== 'string') return; // not one of our events
          const event: NotificationEvent = { ...data, type: data.type, receivedAt: Date.now() };
          setEvents((prev) => [event, ...prev].slice(0, 50));
        } catch { /* ignore non-JSON frames */ }
      };
      const retry = () => {
        setConnected(false);
        if (stopped) return;
        timer = setTimeout(connect, nextBackoffMs(attempt.current++));
      };
      socket.onclose = retry;
      socket.onerror = () => { /* onclose follows and handles the retry */ };
    };

    connect();
    return () => {
      stopped = true;
      if (timer) clearTimeout(timer);
      socket?.close();
      setConnected(false);
    };
  }, [session.userId, session.accessToken]);

  const clear = useCallback(() => setEvents([]), []);
  const value = useMemo(() => ({ events, connected, clear }), [events, connected, clear]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export const useNotifications = () => useContext(Ctx);
