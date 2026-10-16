# WorkNear — React Native (Expo) test app

A working client for the WorkNear backend, built so you can exercise the whole flow end to end:
catalog → post a job → matched workers are notified live → they show interest → the customer
compares profiles and hires one → completion. Also contractor bulk-hiring and KYC review.

Expo SDK 57 · React Native 0.86 · TypeScript · React Navigation 7.

## Run it

1. **Start the backend** (`../backend`): `cp .env.example .env`, fill it in, `docker compose up --build`.
   The gateway listens on port **8080**.
2. **Point the app at it.** The app needs the gateway's address *as seen from the device*:

   | Where the app runs | Server URL |
   |---|---|
   | Android emulator | `http://10.0.2.2:8080` (default) |
   | iOS simulator | `http://localhost:8080` (default) |
   | Real phone (Expo Go) | `http://<your PC's LAN IP>:8080`, phone and PC on the same Wi-Fi |

   Set it in `.env` (`EXPO_PUBLIC_API_URL=...`) or at runtime: **Login screen → Server settings →
   Save and test connection**. That button calls the public catalog endpoint, so it tells you
   immediately whether the gateway and catalog-service are reachable.
3. `npm install` then `npx expo start`, and open it in Expo Go or an emulator.

## End-to-end test script

Use two devices/emulators (or log out and back in on one). Customer and worker share a default
test location in Noida so they are inside each other's 5 km radius without real GPS.

1. **Admin** — Sign up → *Admin (test only)*. Leave it logged in on a second device, or come back to it in step 3.
2. **Worker** — Sign up → *I am a worker*. Profile tab: name, tick a few services (try *Electrician* and *Fan Installation/Repair*), pick gender, save. Then Profile → **KYC documents → Submit placeholder documents**.
3. **Admin** — KYC review → approve all three documents. (The worker is only matched once all three are approved.)
4. **Worker** — Home tab → switch **online**. Watch for "Live notifications connected".
5. **Customer** — Sign up → *I need work done*. Home → category → *Electrician* → Post job.
6. **Worker** — the job appears under *Jobs near you* → **I'm interested** (optionally with a message).
7. **Customer** — the job shows the interested worker's profile (and a notification arrives) → **Hire this worker**.
8. **Worker** — sees "You got job #N". **Customer** → *Mark work as completed*.
9. **Bulk** — Customer Home → *Bulk hire* → post a requirement; worker's *Group jobs* tab shows it.

If live notifications are not connected, the worker's Home screen has *Open a job by number*, so you
can still test interest/hire without the WebSocket.

## What was verified, and what was not

Verified here: `npm run typecheck` (strict TypeScript, clean), `npm test` (18 passing unit tests for
the API client — token refresh, single-flight refresh, error messages — and the date/URL/catalog
helpers), a Metro bundle of the whole app (882 modules), and a script that checks every one of the
28 endpoints the app calls against the backend's controller mappings and the gateway's routes.

**Not verified:** the app has not been run on a device or emulator, and the backend has not been
booted, in my environment. Treat the first real run as the real test — I expect small runtime issues.

## Known gaps — read before relying on this

- **Anyone can register as ADMIN, and the gateway does not check roles on `/api/kyc/admin/**` or `/api/admin/**`.** The app only shows the "Admin" option in dev builds, but the *backend* hole is real: any logged-in user can approve any KYC document. Fix before launch.
- **Workers can look "online" with a stale position.** Location pings are foreground-only (every 20 s while the Home screen is mounted) and matching does not ignore stale pings yet. Logging out marks a worker offline; just closing the app does not.
- The gateway's auth endpoints are rate-limited to 10 requests/minute — repeated test logins can hit it.
- Android cleartext HTTP is enabled (`app.json`) so a plain-HTTP dev backend works. Remove it and use HTTPS for any real build.
- The bulk feed makes one request per service the worker offers (max 10), because the endpoint matches one code at a time.
- Photos and video can be attached; **voice notes cannot** (no recorder yet). Attachment upload needs S3 configured on the backend — if it isn't, the app says so and lets you post without.

## Not built yet

In-app chat, payments/wallet, ratings, Google sign-in, push notifications when the app is closed
(only live WebSocket while it's open), background location, Hindi/voice-first UI.

## Layout

```
src/api/        client.ts (fetch + refresh + errors), endpoints.ts (typed calls)
src/state/      auth, location, notifications (WebSocket), runtime (server URL + secure storage)
src/screens/    auth, customer, labour, admin, common
src/components/ ui kit, ServicePicker (336-service catalog), LabourCard
src/utils/      pure helpers (urls, schedule, catalog, backoff) — unit tested
tests/          node:test suites
```
