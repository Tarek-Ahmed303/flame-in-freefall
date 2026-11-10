# Shared context: Flame in Freefall (NASA Space Apps 2026)
Read first in any session. Update when decisions change. (Updated Oct 24)

## Product
- Challenge: "Flame in Freefall: AI-Powered Fire Safety Insights from Microgravity Combustion Data".
- Web (frontend teammate): dashboard, AI assistant, safety guidelines.
- Mobile (Abdelrahman, Kotlin + Compose, MVVM): "Astronaut Emergency" app. Fire alert (FCM) -> steps -> AI assistant (voice).

## Status (Oct 24)
- Backend (Menna, Laravel) finished and linked to the AI. Mobile -> backend -> AI -> backend -> mobile. Mobile never calls the AI directly.
- Testing now with MOCK data only; real NASA data not loaded yet.
- Base URL: https://gap-concerned-sorted-pending.trycloudflare.com (temporary tunnel to Menna's local server; works only while her server is on). Keep in BuildConfig, one place.

## Backend contract (Bearer token on everything except auth)
- POST /register {name,email,password,password_confirmation,assigned_module} -> verification_id (OTP by email)
- POST /register/verify-otp {verification_id,otp}
- POST /login {email,password} -> token,user,fcm_token
- POST /logout; POST /forgot-password {email}; POST /reset-password {token,email,password,password_confirmation}
- GET /me
- PUT /fcm-token {fcm_token}; PUT /reset-module {assigned_module} (e.g. HARMONY, KIBO; full list TBD)
- POST /chats (new chat); GET /chats -> {chats:[{id,user_id,title,created_at,updated_at}]}
- GET /chats/{id} -> {chat:{...,messages:[{id,chat_id,message,answer:{AI json}}]}}
- POST /ask {question,chat_id} -> {message,chat_id,title,data:{answer:{AI json}}}
- GET /guide -> [{id,step_order,title,body}] (single generic list, no type)
- POST /simulate -> sends FCM fire alert to devices of the user's module; error if no devices registered for the module.
- GONE / not seen in the new collection: /emergencies, /notifications, /steps?type=. Old per-type steps (electrical/chemical/hypoxia) are replaced by /guide unless Menna says otherwise.
- Chat history is server-side: no Room needed for chat.

## AI answer JSON (inside data.answer)
question, answer, experimental_evidence[], prediction (nullable), observed_trends[], interpretation, safety_insight, limitations[], sources[], model_used, answerability{...}, prediction_notice.
- Greetings/out-of-scope: only `answer` filled, rest empty/null (model_used = conversation_handler).
- Evidence comes from synthetic data until real NASA data is loaded. Always show prediction_notice when prediction exists.
- Slow LLM: client timeout 90s.

## Mobile UI decisions
- Splash 1: orange screen -> original flame character (black glossy eyes, no cheeks) -> auto-navigate to 2nd splash ~3.2s.
- Remove CREW ID field.
- Transparent navigation bar, edge-to-edge (Samsung: contrast enforcement off, targetSdk 35).

## Open items
- google-services.json / Firebase access (project pyronova-81113, package com.flame.astronaut). Blocking FCM.
- Stable base URL (tunnel is temporary).
- FCM data payload shape for /simulate; full module list.
- Real NASA data; verify steps text against official NASA/ISS source.
- Menna: remove real email/tokens from Postman, set APP_DEBUG=false (stack traces with local paths leak), fix typo `{{{base-url}}/me`.
