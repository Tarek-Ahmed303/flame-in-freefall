# Endpoints the Android app calls today (source of truth = the code)

Header on every call: `X-API-Key: <key>`. Base URL from `local.properties` (`flame.baseUrl`). JSON, English only.
If you rename/change anything here, tell mobile BEFORE deploying — the app parses these exact fields.

## 1. GET /health
Response read by the app: `status` ("ok" = backend OK), `llm` ("down" = AI core shown as FALLBACK).

## 2. GET /alerts/active   (polled every 3 s)
Response: array of alerts. Fields read:
`id, label, material, module, rack, readings{o2, co, airflow, temperature}, riskLevel ("LOW|MED|HIGH"), riskIndex, updatedAt`
A new `id` the app has not seen = a new fire alert → full-screen alarm.

## 3. POST /dev/alerts   (Simulate Emergency, scheduled demo alert, closing an alert)
Request: `{ alertId, material, module, rack, label, active, readings{o2, co, airflow, temperature} }`
- Simulate / schedule: `alertId = "SIM-<epoch>"`, material "PMMA (acrylic)", module "Destiny Lab", rack "RACK LAB1O3", o2 19.0, co 110, airflow 0.06, temperature 74.
- Resolve: `{ alertId, active: false }` (this is how the app closes an alert — we need a real `resolve` endpoint).
Response: the alert object (same shape as /alerts/active).

## 4. POST /steps
Request: `{ alertId, material, o2, co, airflow, module, rack, temperature }`
Response read: `steps[] { title, body }`, `level`. App shows however many steps are returned (UI supports 6).
If the call fails the app shows a built-in 6-step fallback (Protect Yourself, Report the Fire to the Crew, Isolate Ignition Sources, Suppress the Fire, Check the Atmosphere, Restore Safe Conditions).

## 5. POST /risk   (Home gauge, every 5 s)
Request: same fields as /steps (alertId optional). With no active alert the app sends fixed baseline readings
(material "PMMA (acrylic)", o2 21.0, co 0, airflow 0.14, module "Destiny Lab", rack "RACK LAB1O3", temperature 22).
Response read: `index` (0–100), `level`, `trend`, `trendText`, `updatedAt`.

## 6. GET /chat/suggestions?mode=normal|emergency&alertId=
Response read: `suggestions[] { id, text }`.

## 7. POST /chat   (SSE by default, retry once with ?stream=false on error)
Request: `{ message, mode: "normal"|"emergency", alertId, history: [{role:"user"|"assistant", content}] (last 6), via: "text" }`
Headers: `Accept: text/event-stream`, URL `…/chat?stream=true`. Retry: `POST /chat?stream=false` with `Accept: application/json`.
SSE events read: `token {delta}`, `card {tag, level, text, rows[{icon,label,value}], sources[]}`, `done {text, cards[], meta}`, `error`.
`done` is treated as the final answer. Card tags are shown as-is (underscores → spaces). Icon keys used:
flame, smoke, oxygen, co, fan, airflow, module, rack, thermometer, pressure, material, warning, check, mask, extinguisher, door, power, clock, info, book.

## 8. FCM push (not an HTTP endpoint)
Data-only, priority high:
`{ type:"FIRE_ALERT", alertId, material, module, rack, o2, co, airflow, risk, detectedAt (ISO-8601), temperature? }` — all values strings.

---

# NOT implemented in the app yet (proposals — name them as you like and tell mobile)
- Auth: login / register / refresh / logout / GET /me  (login is still fake)
- POST /devices { fcmToken, platform, appVersion }
- Alert actions: acknowledge, silence, resolve, alert-crew
- `detectedAt` on the alert object (the elapsed timer currently starts when the app first sees the alert)
- Current station telemetry for Home when there is no alert (e.g. GET /telemetry/current)
- GET /guide (Safety Guide is still fake data)
