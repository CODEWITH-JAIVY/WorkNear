import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { createApiClient, type SessionStore } from '../api/client';
import { createEndpoints } from '../api/endpoints';
import type { Session } from '../types';
import { normalizeBaseUrl } from '../utils/urls';

const SESSION_KEY = 'worknear.session';
const URL_KEY = 'worknear.apiUrl';

// Android emulators reach the host machine at 10.0.2.2; iOS simulators use localhost.
// A physical phone needs your PC's LAN IP — set EXPO_PUBLIC_API_URL or use the Server screen.
const DEFAULT_URL =
  process.env.EXPO_PUBLIC_API_URL ?? (Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080');

let baseUrl = normalizeBaseUrl(DEFAULT_URL);
export const getBaseUrl = () => baseUrl;

export async function loadSavedBaseUrl(): Promise<void> {
  const saved = await SecureStore.getItemAsync(URL_KEY);
  if (saved) baseUrl = normalizeBaseUrl(saved);
}

export async function saveBaseUrl(url: string): Promise<void> {
  baseUrl = normalizeBaseUrl(url);
  await SecureStore.setItemAsync(URL_KEY, baseUrl);
}

export const sessionStore: SessionStore = {
  async get() {
    const raw = await SecureStore.getItemAsync(SESSION_KEY);
    if (!raw) return null;
    try { return JSON.parse(raw) as Session; } catch { return null; }
  },
  async set(session) {
    if (session) await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session));
    else await SecureStore.deleteItemAsync(SESSION_KEY);
  },
};

let expiredListener: (() => void) | null = null;
export const onSessionExpired = (fn: (() => void) | null) => { expiredListener = fn; };

export const api = createEndpoints(
  createApiClient({ getBaseUrl, store: sessionStore, onSessionExpired: () => expiredListener?.() }),
);
