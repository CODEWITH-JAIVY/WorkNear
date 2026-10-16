import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createApiClient, ApiError, buildQuery, type SessionStore } from '../src/api/client';
import type { Session } from '../src/types';

const sess = (n: number): Session => ({ accessToken: `acc${n}`, refreshToken: `ref${n}`, userId: 7, userType: 'CUSTOMER' });

function memoryStore(initial: Session | null): SessionStore & { current: Session | null } {
  const s = { current: initial, async get() { return s.current; }, async set(v: Session | null) { s.current = v; } };
  return s;
}
const json = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), { status });

type Call = { url: string; init: RequestInit };
function fakeFetch(handler: (c: Call, n: number) => Response | Promise<Response>) {
  const calls: Call[] = [];
  const impl = (async (url: string, init: RequestInit) => {
    calls.push({ url, init });
    return handler({ url, init }, calls.length);
  }) as unknown as typeof fetch;
  return { impl, calls };
}
const auth = (c: Call) => (c.init.headers as Record<string, string>).Authorization;

test('attaches the bearer token and builds the URL from the live base URL', async () => {
  const f = fakeFetch(() => json(200, [{ id: 1 }]));
  let base = 'http://a:8080';
  const api = createApiClient({ getBaseUrl: () => base, store: memoryStore(sess(1)), fetchImpl: f.impl });
  await api.get('/api/jobs/mine');
  base = 'http://b:9090'; // changed at runtime from the Server screen
  await api.get('/api/jobs/mine');
  assert.equal(f.calls[0]!.url, 'http://a:8080/api/jobs/mine');
  assert.equal(f.calls[1]!.url, 'http://b:9090/api/jobs/mine');
  assert.equal(auth(f.calls[0]!), 'Bearer acc1');
});

test('401 -> refresh once -> retry succeeds with the new token, and the new session is stored', async () => {
  const store = memoryStore(sess(1));
  const f = fakeFetch((c) => {
    if (c.url.includes('/api/auth/refresh')) return json(200, sess(2));
    return auth(c) === 'Bearer acc2' ? json(200, { ok: true }) : json(401);
  });
  const api = createApiClient({ getBaseUrl: () => 'http://x', store, fetchImpl: f.impl });
  const out = await api.get<{ ok: boolean }>('/api/jobs/mine');
  assert.deepEqual(out, { ok: true });
  assert.equal(store.current?.accessToken, 'acc2');
  assert.ok(f.calls[1]!.url.includes('refreshToken=ref1'));
  assert.equal(f.calls.length, 3);
});

test('concurrent 401s share a single refresh call', async () => {
  const store = memoryStore(sess(1));
  let refreshCalls = 0;
  const f = fakeFetch(async (c) => {
    if (c.url.includes('/api/auth/refresh')) { refreshCalls++; await new Promise((r) => setTimeout(r, 20)); return json(200, sess(2)); }
    return auth(c) === 'Bearer acc2' ? json(200, {}) : json(401);
  });
  const api = createApiClient({ getBaseUrl: () => 'http://x', store, fetchImpl: f.impl });
  await Promise.all([api.get('/a'), api.get('/b'), api.get('/c')]);
  assert.equal(refreshCalls, 1);
});

test('failed refresh clears the session, notifies, and surfaces a clear error', async () => {
  const store = memoryStore(sess(1));
  let expired = 0;
  const f = fakeFetch((c) => (c.url.includes('/api/auth/refresh') ? json(401) : json(401)));
  const api = createApiClient({ getBaseUrl: () => 'http://x', store, fetchImpl: f.impl, onSessionExpired: () => expired++ });
  await assert.rejects(api.get('/api/jobs/mine'), (e: ApiError) => e.status === 401 && /session expired/i.test(e.message));
  assert.equal(store.current, null);
  assert.equal(expired, 1);
});

test('auth:false requests never trigger a refresh (a wrong password is just a 401 with a message)', async () => {
  const store = memoryStore(sess(1));
  const f = fakeFetch(() => json(401, { error: 'Invalid credentials' }));
  const api = createApiClient({ getBaseUrl: () => 'http://x', store, fetchImpl: f.impl });
  await assert.rejects(api.post('/api/auth/login', { body: {}, auth: false }),
    (e: ApiError) => e.status === 401 && e.message === 'Invalid credentials');
  assert.equal(f.calls.length, 1);
  assert.equal(auth(f.calls[0]!), undefined); // no token sent on auth:false calls
});

test('extracts the backend {error} message for 409s and falls back for empty bodies', async () => {
  const f1 = fakeFetch(() => json(409, { error: 'Already expressed interest in this job' }));
  const a1 = createApiClient({ getBaseUrl: () => 'http://x', store: memoryStore(sess(1)), fetchImpl: f1.impl });
  await assert.rejects(a1.post('/x'), (e: ApiError) => e.status === 409 && e.message === 'Already expressed interest in this job');

  const f2 = fakeFetch(() => json(500));
  const a2 = createApiClient({ getBaseUrl: () => 'http://x', store: memoryStore(sess(1)), fetchImpl: f2.impl });
  await assert.rejects(a2.get('/x'), (e: ApiError) => e.status === 500 && /500/.test(e.message));
});

test('a network failure names the server address so a wrong IP is obvious', async () => {
  const impl = (async () => { throw new TypeError('Network request failed'); }) as unknown as typeof fetch;
  const api = createApiClient({ getBaseUrl: () => 'http://10.0.2.2:8080', store: memoryStore(null), fetchImpl: impl });
  await assert.rejects(api.get('/x', { auth: false }), (e: ApiError) => e.status === 0 && e.message.includes('http://10.0.2.2:8080'));
});

test('multipart uploads do not set Content-Type (so the boundary is generated)', async () => {
  const f = fakeFetch(() => json(200, { url: 'u', key: 'k' }));
  const api = createApiClient({ getBaseUrl: () => 'http://x', store: memoryStore(sess(1)), fetchImpl: f.impl });
  const form = new FormData();
  form.append('folder', 'job-attachments');
  await api.post('/api/media/upload', { form });
  assert.equal((f.calls[0]!.init.headers as Record<string, string>)['Content-Type'], undefined);
  assert.ok(f.calls[0]!.init.body instanceof FormData);
});

test('buildQuery skips empty values and encodes the rest', () => {
  assert.equal(buildQuery({ a: 1, b: undefined, c: null, d: '', e: 'x y' }), '?a=1&e=x%20y');
  assert.equal(buildQuery({}), '');
  assert.equal(buildQuery(undefined), '');
});
