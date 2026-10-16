import type { Session } from '../types';

export interface SessionStore {
  get(): Promise<Session | null>;
  set(session: Session | null): Promise<void>;
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public body?: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface ClientOptions {
  getBaseUrl: () => string;
  store: SessionStore;
  fetchImpl?: typeof fetch;
  onSessionExpired?: () => void;
  timeoutMs?: number;
}

export interface RequestOptions {
  body?: unknown;
  form?: FormData;
  query?: Record<string, string | number | boolean | null | undefined>;
  auth?: boolean; // default true
}

export function buildQuery(query?: RequestOptions['query']): string {
  if (!query) return '';
  const parts = Object.entries(query)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return parts.length ? `?${parts.join('&')}` : '';
}

function messageFrom(body: unknown, fallback: string): string {
  if (body && typeof body === 'object') {
    const b = body as Record<string, unknown>;
    if (typeof b.error === 'string') return b.error;
    if (typeof b.message === 'string') return b.message;
  }
  if (typeof body === 'string' && body.trim()) return body.slice(0, 200);
  return fallback;
}

export function createApiClient(opts: ClientOptions) {
  const doFetch: typeof fetch = opts.fetchImpl ?? ((...a) => fetch(...a));
  const timeoutMs = opts.timeoutMs ?? 15_000;
  let refreshing: Promise<Session | null> | null = null;

  async function raw(method: string, path: string, session: Session | null, o: RequestOptions): Promise<Response> {
    const url = opts.getBaseUrl() + path + buildQuery(o.query);
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (session && o.auth !== false) headers.Authorization = `Bearer ${session.accessToken}`;
    let body: BodyInit | undefined;
    if (o.form) {
      body = o.form; // fetch sets the multipart boundary itself — never set Content-Type here
    } else if (o.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(o.body);
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      return await doFetch(url, { method, headers, body, signal: controller.signal });
    } catch (e) {
      const aborted = (e as { name?: string }).name === 'AbortError';
      throw new ApiError(0, aborted
        ? `Request timed out. Is the server reachable at ${opts.getBaseUrl()}?`
        : `Cannot reach the server at ${opts.getBaseUrl()}. Check the address and your network.`);
    } finally {
      clearTimeout(timer);
    }
  }

  // Single-flight: several requests hitting 401 together share ONE refresh call. Refresh tokens
  // are issued per call, so racing refreshes would be wasteful at best.
  function refreshSession(current: Session): Promise<Session | null> {
    if (!refreshing) {
      refreshing = (async () => {
        try {
          const res = await raw('POST', '/api/auth/refresh', null, {
            query: { refreshToken: current.refreshToken }, auth: false,
          });
          if (!res.ok) throw new Error('refresh rejected');
          const data = (await res.json()) as { accessToken: string; refreshToken: string; userId: number; userType: Session['userType'] };
          const next: Session = {
            accessToken: data.accessToken, refreshToken: data.refreshToken,
            userId: data.userId, userType: data.userType,
          };
          await opts.store.set(next);
          return next;
        } catch {
          await opts.store.set(null);
          opts.onSessionExpired?.();
          return null;
        } finally {
          refreshing = null;
        }
      })();
    }
    return refreshing;
  }

  async function parse(res: Response): Promise<unknown> {
    const text = await res.text();
    if (!text) return undefined;
    try { return JSON.parse(text); } catch { return text; }
  }

  async function request<T>(method: string, path: string, o: RequestOptions = {}): Promise<T> {
    let session = await opts.store.get();
    let res = await raw(method, path, session, o);

    if (res.status === 401 && o.auth !== false && session) {
      session = await refreshSession(session);
      if (!session) throw new ApiError(401, 'Your session expired. Please log in again.');
      res = await raw(method, path, session, o);
    }

    const body = await parse(res);
    if (!res.ok) {
      const fallback = res.status === 401 ? 'Not authorised' : `Request failed (${res.status})`;
      throw new ApiError(res.status, messageFrom(body, fallback), body);
    }
    return body as T;
  }

  return {
    get: <T>(path: string, o?: RequestOptions) => request<T>('GET', path, o),
    post: <T>(path: string, o?: RequestOptions) => request<T>('POST', path, o),
    put: <T>(path: string, o?: RequestOptions) => request<T>('PUT', path, o),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
