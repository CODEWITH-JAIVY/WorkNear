import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { Session, UserType } from '../types';
import { api, loadSavedBaseUrl, onSessionExpired, sessionStore } from './runtime';

interface AuthValue {
  session: Session | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (b: { email: string; mobile: string; password: string; userType: UserType }) => Promise<void>;
  logout: () => Promise<void>;
}

const Ctx = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    (async () => {
      await loadSavedBaseUrl();
      setSession(await sessionStore.get());
      setReady(true);
    })();
    onSessionExpired(() => setSession(null));
    return () => onSessionExpired(null);
  }, []);

  const adopt = useCallback(async (s: Session) => {
    await sessionStore.set(s);
    setSession(s);
  }, []);

  const value = useMemo<AuthValue>(() => ({
    session, ready,
    login: async (email, password) => adopt(await api.auth.login({ email, password })),
    signup: async (b) => adopt(await api.auth.signup(b)),
    logout: async () => { await sessionStore.set(null); setSession(null); },
  }), [session, ready, adopt]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthValue {
  const v = useContext(Ctx);
  if (!v) throw new Error('useAuth outside AuthProvider');
  return v;
}
