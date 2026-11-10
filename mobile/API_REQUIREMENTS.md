# Flame in Freefall — Mobile app: what we need from each track

Mobile (Android, Kotlin + Compose) is fully built against fake data. Every item below is what mobile needs to switch to real data with **zero UI changes**. The JSON shapes are *proposals* — if you already have different ones, send yours and mobile adapts; what matters is that every field below exists and is documented.

**Please send, from every track:** base URL(s) (staging + prod), an OpenAPI/Swagger or Postman collection, and a test account.

---

## 1. Backend

### 1.1 General
- Base URL, API versioning, HTTPS only, CORS not needed for mobile.
- Auth scheme for all calls (proposal: `Authorization: Bearer <accessToken>`), token lifetime, refresh flow.
- One **error format** for all endpoints, e.g. `{ "code": "INVALID_CREDENTIALS", "message": "...", "details": {} }` + the list of codes and HTTP statuses (400/401/403/404/409/422/429/5xx).
- Timeouts / rate limits (the chat especially), max payload sizes.
- A `GET /health` returning status of: backend, AI core, telemetry (the splash screen shows these 3 lines live).
- Timestamps: ISO-8601 UTC everywhere.

### 1.2 Auth
| Endpoint | Request | Response |
|---|---|---|
| `POST /auth/register` | `name, crewId ("CRW-0743"), email, passcode (min 10 chars), module (DESTINY/HARMONY/COLUMBUS/KIBO), allowCriticalOverride` | `accessToken, refreshToken, expiresIn, user` |
| `POST /auth/login` | `email, passcode, trustDevice` | same |
| `POST /auth/refresh` | `refreshToken` | new tokens |
| `POST /auth/logout` | refresh token | 204 |
| `GET /me`, `PATCH /me` | — / profile fields | `user = { id, name, role ("Flight Engineer"), crewId, email, module, shift ("Alpha"), language ("en"/"ar") }` |
| `POST /auth/forgot` | email | 204 (login screen has "Forgot passcode?") |

Questions: is email verification required (the sign-up screen has step "02 VERIFY")? What are the exact password rules and the error for an existing email/crewId? Is biometric login just a local unlock of the stored token (our assumption)?

### 1.3 Push notifications (critical — this is the core feature)
- **Android FCM**: mobile needs the Firebase project's `google-services.json` (package: `com.flame.astronaut`).
- `POST /devices` — register the device: `{ fcmToken, platform: "android", appVersion, language }`; `DELETE /devices/{id}` on logout.
- FCM message format for a fire alert. Must be a **high-priority data message** (so the app can raise the full-screen alarm even when killed/locked). Proposed data payload:
  `{ type:"FIRE_ALERT", alertId, material, module, rack, o2, co, airflow, risk:"HIGH", detectedAt }`
- Who gets it (all crew in that module? everyone?), and the "Crew notified · 3 devices received alert" count — is there an endpoint for that?
- Alert lifecycle: `POST /alerts/trigger` (real detection **and** the app's "Simulate Emergency", with a flag like `simulated:true`), `POST /alerts/{id}/acknowledge`, `POST /alerts/{id}/silence`, `POST /alerts/{id}/resolve`, `POST /alerts/{id}/alert-crew` (the "ALERT CREW" button), `GET /alerts/active`, `GET /alerts?limit=` (history, "Last drill" on Home).
- Real-time channel while the app is open (WebSocket/SSE) for: new alert, alert resolved, risk updates. If not available mobile will poll — tell us the interval you accept.

### 1.4 Steps
- `GET /alerts/{id}/steps` → `[{ order, title, body }]` (currently 4 steps). Who produces them (AI per situation, or static protocol)?
- Step progress sync (so web/ground sees it): `POST /alerts/{id}/steps/{order}/complete`.

### 1.5 Chat (proxy to the AI)
See AI section for the contract. Backend must expose it to mobile, stream or not, and persist history: `GET /chat/history`, `DELETE /chat/history`.

### 1.6 Risk / Home
`GET /risk/current` (see Data section for fields) + real-time updates. `watchMaterial`, 6 h trend. Also `GET /me/mission` for "MET 142:07:33" (mission elapsed time) — or tell us how to compute it.

### 1.7 Guide (offline)
`GET /guide` with `version`/`updatedAt` or ETag so mobile caches it and shows "OFFLINE" safely. Contents: fire-response steps, material risk list, O₂ thresholds (see Data).

### 1.8 Settings sync (optional)
`GET/PUT /me/settings`: readAloud, wakePhrase, assistantVoice, speechRate, language, criticalAlerts, vibrationPattern, alarmVolume.

---

## 2. Data engineering

Mobile shows these numbers; we need the exact **definition, unit, range and update frequency** of each:

**Risk (Home screen)** — `GET /risk/current`
```json
{ "index": 23, "level": "LOW|MED|HIGH",
  "o2": 21.3, "pressure": 101.2, "airflow": 0.14,
  "watchMaterial": "PMMA",
  "trend": [ {"t":"ISO","v":0.55}, ... ],
  "updatedAt": "ISO" }
```
- Units (O₂ %, pressure kPa?, airflow m/s), valid ranges, thresholds that map `index` → LOW/MED/HIGH (design: gauge 0–100).
- How `watchMaterial` is chosen, trend window (6 h) and sampling step.
- Source of telemetry (real sensors vs. NASA datasets vs. simulated) and update rate.
- CO ppm (shown in the emergency card) — is it in the telemetry? Rising/falling flag (the "↑")?

**Alert payload** — material, module, rack naming format (`LAB1O3`), O₂, CO, airflow, risk level, detection time; the exact mock scenario values used in demos (PMMA, Destiny Lab, LAB1O3, O₂ 19.0 %, CO 38 ppm, airflow 0.08 m/s) and a way to inject them for demo.

**Guide content** — material list with risk score per material (PMMA, polycarbonate, cotton, Nomex …, score 0–1 + level) *at stated conditions* ("21 % O₂, low airflow"), O₂ threshold scale (<19.5 low · 19.5–23.5 nominal · >23.5 risk rises), the generation date of the "latest report".

**Chat cards** — the structured data behind AI answers (conditions with values/units, sources).

Also: data freshness/latency guarantees, what happens when a sensor is missing (null? last value + `stale:true`?).

---

## 3. AI (two people — please split: assistant/chat vs. risk & steps)

### 3.1 Chat
`POST /chat` → mobile sends `{ message, mode: "normal|emergency", alertId?, language: "en|ar", history?: [...] , via: "text|voice" }`.

Response (proposal):
```json
{ "text": "Yes, with your breathing mask on…",
  "cards": [ { "type":"risk_brief", "tag":"RISK BRIEF · POLYCARBONATE", "level":"HIGH",
               "text":"…",
               "rows":[ {"icon":"wind|o2|sheet|…","label":"Ventilation flow","value":"5–20 cm/s"} ],
               "sources":["NASA SoFIE","ISS FSS Manual"] } ],
  "actions": ["read_aloud","save_to_guide"] }
```
We need: the final list of card types and icon keys, whether the answer **streams** (SSE/chunked — recommended; replies are read aloud), max latency, languages (EN + AR), how `emergency` mode changes behaviour (short, imperative answers), whether the AI sees live telemetry/current alert automatically, safety guardrails and the fallback text when the model fails.

`GET /chat/suggestions` (the chips: "PMMA vs Nomex?", "Safe O₂ range"…) — static list or generated?

### 3.2 Steps generation
Given an alert (material, O₂, CO, airflow, module, rack) → ordered extinguishing steps `{ title (short), body (1–2 sentences, speakable) }`. Fixed count or variable? Does it change as readings change? Arabic version?

### 3.3 Risk model
The same model as the web dashboard: inputs, output index 0–100, level mapping, refresh rate, explanation text for "6 h trend · stable" (trend label: stable/rising/falling).

### 3.4 Voice
Decision needed: **speech-to-text / text-to-speech on device (Android built-in) or server-side?** Mobile default = on-device (works offline, lowest latency). If server-side: endpoints (`POST /stt` audio → text, `POST /tts` text → audio), formats, Arabic support, wake phrase "Hey Flame" stays on device. Also available assistant voices for the Settings screen ("Atlas · calm").

---

## 4. Product / NASA (decisions that block "no mistakes")
- Final wording of every user-facing text and the Arabic translations (we need AR strings for all screens if Arabic ships).
- Allowed behaviour offline (what works without network: guide yes; chat no?).
- Privacy: what telemetry/voice is stored and for how long.
- Real list of modules, roles, shifts for dropdowns; crew-ID format and validation.
- App/store/icon/branding, support contact, legal text (terms/privacy) for sign-up.
- Test scenarios the team agrees to demo (scripted alert times, accounts).

---

## What mobile will do when this arrives
1. Replace the 6 fake repositories with Retrofit implementations (only `di/AppContainer.kt` changes).
2. Secure token storage, auto-refresh, logout, global error/offline handling.
3. FCM receiver → same full-screen alarm flow already built.
4. Real chat (streaming), real steps, real risk polling/WebSocket, offline-cached guide.
5. Real STT/TTS, Arabic + RTL, reduce-motion, final QA against the design.
