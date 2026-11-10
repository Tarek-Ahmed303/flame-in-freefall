# HANDOFF — Flame in Freefall (Android app "Astronaut Emergency")

> **To the new assistant:** read this whole file first. It is the full memory of the previous chat (the new chat starts empty).
> It contains **no secrets** (no tokens, API keys, passwords, OTPs, or the Firebase service-account key) — never add any.
> Last updated: Oct 24 2026. If you change decisions or finish items, update the relevant section here.

---

## 0. Quick start for you
1. The project folder is **`C:\Users\Abd_Eraman\FlameInFreefall`** (NOT `D:\nasa` — that folder is read-only for this Windows user; the old chat's working dir was `D:\nasa` but all work happened in `FlameInFreefall`).
2. Build: `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` then `gradlew.bat assembleDebug` (first run is slow, later ~1–2 min). Lint: `gradlew.bat lintDebug` (currently **0 errors**, ~16 harmless warnings about newer library versions).
3. APK: `app\build\outputs\apk\debug\app-debug.apk`.
4. A Samsung phone is usually connected via USB (adb serial `RZCX91V2S0Z`). **Read §12 (phone safety rules) before touching it.**
5. Current state in one line: the app is fully wired to the team's real backend (Laravel), verified on the phone; the remaining blockers are on the backend/AI side (§10).

---

## 1. People & project
- **User:** Abdelrahman Hesham — the *mobile* developer (Kotlin + Jetpack Compose). Email on the test account is his own Gmail (do not use it anywhere).
- **Team** (the user manages communication; he pastes their messages/files to the chat and relays my messages back):
  - Backend: **Menna Walied** (Laravel; runs the server on her laptop exposed through a temporary Cloudflare tunnel).
  - Data engineering: 1 person. AI: 2 people (a RAG research assistant + an "agent"). Frontend: web dashboard (not our concern).
- **Project:** NASA Space Apps 2026 challenge "Flame in Freefall: AI-powered fire safety insights from microgravity combustion data". Mobile app "Astronaut Emergency": an astronaut inside a spacecraft receives a **fire alert (FCM push)** → gets **step-by-step extinguishing instructions** → can talk to an **AI research assistant** (voice + text). The mobile app must NOT duplicate the web's "Explore Data" / "Report Generator".
- Data is currently **mock/synthetic** (real NASA data not loaded yet). The AI shows a "not certified safety guidance" notice.
- Hackathon demo is planned for **Nov 14–15** (from a team message).

## 2. How the user likes to work (important)
- Writes in **Egyptian Arabic**, informal. **Reply in Egyptian Arabic**, short and honest. Technical terms/code in English. **The app UI is English-only** (decision: Arabic/RTL dropped to simplify).
- Wants things **done**, not asked about; but wants me to **say plainly what is done, what is verified, and what is blocked**. He often asks "did you finish?" — answer truthfully (e.g. "built but not run on a device").
- He pastes long English specs (prompts written for an AI) and says "do this" — implement exactly, say deviations.
- He forwards my text to teammates (Telegram/WhatsApp) — when asked "what do I tell X", write a ready-to-paste block.
- Do not send/publish anything outside the machine on his behalf; he sends messages himself.
- He asked me not to touch things already done; focus on what remains.

## 3. History (what happened, in order)
1. Received the UI handoff zip (10 screens, HANDOFF.md, screenshots, HTML source). Built all screens in Compose with **fake repositories** behind interfaces (MVVM + Navigation).
2. Splash mini-game ported from the HTML canvas (chickens + spaceship). Splash resized so it doesn't hide the status lines.
3. Alerts: removed the auto-alert; added a **BACKGROUND ALERT** demo card on Home (delay → alert even when app is in background); alert never rings before login.
4. Backend team sent docs for an OLD FastAPI "mobile API" (X-API-Key, SSE chat, /risk, /steps…). I integrated it, then it was **replaced** by the Laravel backend (Postman collection "pyronova") — the old integration was deleted.
5. FCM: Firebase project `pyronova-81113`; `google-services.json` received (registered with the typo package `com.flame.astronuat`, so applicationId was changed to match). Token shown in Settings (debug).
6. Three design edits applied: new **flame intro splash** (Canvas), **Crew ID removed** from sign-up, **transparent navigation bar** (edge-to-edge).
7. AI team sent RAG output samples → the **AI Assistant** feature (answer card with collapsible sections, sources bottom sheet, push-to-talk, TTS). First version talked to `/ask` directly with Room history; later rewritten to the backend (`/chats`, `/ask`), Room removed.
8. Real backend integrated (auth, OTP, FCM token sync, guide, simulate, chats). Tested on the phone with the real backend (see §11).
9. Fixes found by lint/tests: alarm sound API-28 crash fixed (MediaPlayer), BOM char, clean error messages, splash telemetry honesty, FAB position, notification channel mitigation.

## 4. Environment & paths
| Thing | Value |
|---|---|
| Project | `C:\Users\Abd_Eraman\FlameInFreefall` |
| Code namespace | `com.flame.astronaut` |
| **applicationId** | `com.flame.astronuat` (typo, must match Firebase `google-services.json`; to fix: re-add Android app `com.flame.astronaut` in Firebase, replace `app/google-services.json`, change `applicationId`) |
| minSdk / target / compile | 26 / 35 / 35 |
| Kotlin / AGP / Compose BOM | 2.1.20 / 8.9.1 / 2025.04.00; Retrofit 3.0.0, OkHttp 4.12, kotlinx-serialization 1.8.0, DataStore 1.1.2, Firebase BOM 33.7.0 |
| Android SDK | `C:\Users\Abd_Eraman\AppData\Local\Android\Sdk` (no `cmdline-tools`, so no avdmanager; no emulator AVD) |
| adb | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`; phone serial `RZCX91V2S0Z` (Samsung A25, One UI, Arabic locale, 3-button nav) |
| Design handoff | `Downloads\flame-in-freefall-ui-handoff_1.zip` (screens `design/screenshots/*.png`, markup `design/source/*.dc.html`, tokens inside) |
| **Reference docs (copied into the project)** | `docs/` — `pyronova_collection_sanitized.json` (**the backend contract**), `shared-context.md`, AI samples (`final_response.json`, `Final_output.txt`, `output.txt`, `response_1792681686567.json`), and the UI design zip `flame-in-freefall-ui-handoff_1.zip` (unzip to see `design/screenshots`, `design/source`). The Firebase service-account key is deliberately NOT here. |
| Original team files | `Downloads\Telegram Desktop\`: `pyronova_collection_sanitized.json` (**the contract**), `shared-context.md`, `MOBILE_API.md`/`FINAL_REPORT.md`/`MOBILE_HANDOFF.md` (OLD API — obsolete), AI samples (`final_response.json`, `Final_output.txt`, `output.txt`, `response_*.json`) |
| Extra docs in project | `API_REQUIREMENTS.md`, `API_ENDPOINTS_USED.md` (both describe the OLD API; kept only as history — can be deleted) |
| Shell notes | PowerShell 5.1 (edits via Get/Set-Content can corrupt non-ASCII), Git Bash available. Build logs were written to temp files. |

## 5. Configuration (single place: `app/build.gradle.kts`)
- `BASE_URL`: default `https://gap-concerned-sorted-pending.trycloudflare.com/` — **temporary tunnel to Menna's laptop; changes whenever she restarts it**. Override in `local.properties` (`flame.baseUrl=...`) or `-PflameBaseUrl=...`. Always keep a trailing `/`.
- `USE_FAKE` (default `false`): `flame.useFake=true` in `local.properties` → every repository returns mock data (offline demo). Uses `assets/fake_ask_response.json` for the assistant.
- `local.properties` currently only has `sdk.dir` (+ commented overrides).
- Firebase: `app/google-services.json` exists; the google-services plugin is applied only if the file exists (build works without it).
- OkHttp: connect 15 s, read/write 90 s (LLM is slow). Bearer-token interceptor; Accept: application/json.
- Debug-only UI (hidden in release): Simulate Emergency card, BACKGROUND ALERT card, Settings → "Device · push token" (copy token). Also shown when `USE_FAKE=true`.

## 6. Architecture & file index (`app/src/main/java/com/flame/astronaut/`)
Pattern: **MVVM + repository interfaces**; real (`data/real`) vs fake (`data/fake`) chosen **only** in `di/AppContainer.kt`.
- `FlameApp.kt` (sets `Locale.US`, builds container) · `MainActivity.kt` (edge-to-edge, show-when-locked, handles FCM-tap intent extras → alert, notification permission).
- `domain/Domain.kt` — models (`FireAlert` with optional fields, `UserProfile`, `AssignedModule {HARMONY,KIBO}`, `Step`, `GuideContent`, chat models) + interfaces: `AuthRepository`, `EmergencyRepository`, `GuideRepository`, `ChatRepository`, `RiskRepository`, `HealthRepository`. `FireAlert.summaryLine()` builds text only from fields present.
- `net/` — `Network.kt` (OkHttp/Retrofit, 401 → `SessionManager.loggedOut`), `PyronovaApi.kt` (all endpoints), `Dto.kt`, `TokenStore.kt` (DataStore; token AES-GCM encrypted by an Android Keystore key), `ApiErrors.kt` (`Throwable.userMessage()`: shows Laravel `message`/first field error; **hides internal details** like cURL/host/stack/paths and 5xx bodies).
- `data/real/RealRepositories.kt` — `RealAuthRepository` (login/register/OTP/forgot/reset/me/reset-module/fcm sync), `RealChatRepository`, `RealGuideRepository` (network → cache file `filesDir/guide_cache.json` → `assets/guide_fallback.json`; never empty), `RealEmergencyRepository` (alerts come from FCM `onPushAlert`; `simulate()` = `POST /simulate`; scheduled demo = delay then `/simulate`), `RealHealthRepository` (GET base URL, any status <500 = backend reachable), `RealRiskRepository` (empty: no telemetry endpoint).
- `data/fake/FakeRepositories.kt` — demo implementations.
- `assistant/` — `AskModels.kt` (`AskResponse`, evidence, prediction (tolerant serializer: list/object/null), answerability…), `AssistantViewModels.kt` (`AssistantListViewModel` for `GET /chats`; `AssistantChatViewModel`: opens `GET /chats/{id}`; **new chat created lazily**: `POST /chats` then `POST /ask {question, chat_id}`; optional nav arg `q` auto-asks), `AssistantScreens.kt` (list, chat, answer card, sources bottom sheet), `Voice.kt` (`SpeechInput` push-to-talk, `Speaker` TTS; read-aloud reads ONLY the safety insight, or the first 2 sentences of the answer if empty).
- `push/` — `FlameMessagingService` (logs FCM keys under Logcat tag `FlamePush`; values only in debug), `PushPayload.kt` (`parsePushAlert`, very defensive: payload shape wasn't documented), `PushTokens.kt`.
- `emergency/EmergencyCoordinator.kt` — owns the active alert; plays alarm (**MediaPlayer** loop, USAGE_ALARM) + SOS vibration; posts high-priority notification with full-screen intent (channel `fire_alerts`); **no sound/vibration/notification until the user is signed in** (persisted flag `flame_session/signed_in`, set from nav route).
- `ui/` — `auth/AuthScreens.kt` (Login, SignUp, VerifyOtp, ForgotPassword + their ViewModels), `home/HomeScreen.kt`, `guide/GuideScreen.kt`, `settings/SettingsScreen.kt`, `chat/ChatScreen.kt` (**the emergency STEPS screen**; name is historical), `emergency/EmergencyScreens.kt` (full-screen alert + in-app banner), `splash/` (`IntroSplashScreen` new flame animation, `SplashScreen` + `MiniGame`), `nav/AppNavHost.kt`, `components/` (Starfield, ChannelInput HUD field, ChamferedButton, HazardStripe, PulseMic, Waveform, FlameBottomNav, Icons as ImageVectors), `theme/Theme.kt` (tokens: bg `#04060B`, orange `#FF7A2F`, cyan `#5BC8F5`, red `#FF4538`, green `#3DDC97`; fonts Chakra Petch / IBM Plex Sans / IBM Plex Mono downloaded into `res/font`), `Format.kt` (`fmtUS`).
- Resources: `assets/guide_fallback.json` (the 6 real steps), `assets/fake_ask_response.json`, `res/values{,-v28,-v29}/themes.xml` (transparent bars, contrast enforcement off), manifest (INTERNET, VIBRATE, POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT, WAKE_LOCK, RECORD_AUDIO, `<queries>` for speech/TTS, FCM service, default FCM channel = `fire_alerts`). No `usesCleartextTraffic` (tunnel is HTTPS).

## 7. Screens, navigation & behaviors
Routes (`Routes` in `AppNavHost.kt`): `intro → splash → login | home` · `login ⇄ signup → otp/{verificationId}/{email} → home` · `login → forgot` · `home` (bottom nav: Home, AI, Guide, Settings) · `assistant` (chat list) → `assistant/chat/{conversationId}?q={q}` (0 = new chat) · `steps` (emergency) · `alert` (full-screen).
- **Intro splash** (3.2 s, one Canvas + one `Animatable`): dark→orange fade with a face, then the full-screen orange shrinks into a flame character (cubic Bézier morph), idle bob/blink, then goes to the game splash. Status-bar icons are light (faint on the light ground — user was told; flip to dark if he asks).
- **Game splash:** chickens/spaceship canvas game; 3 status lines from a real reachability check: "Station backend link" (real), "Flame AI core" (= same reachability), "Fire-risk telemetry" = **"NOT CONNECTED"** in real mode (no telemetry endpoint). Auto-continues; goes to **Home if a token exists, else Login**.
- **Login:** email + passcode, error text under fields (backend message), "Forgot passcode?", "Request access". Fields start empty. "Trust this device" / fingerprint button are visual only.
- **Sign-up:** CH-01 name, CH-02 email, CH-03 passcode (≥8, client check), CH-04 confirm, module chips HARMONY/KIBO (enum `AssignedModule` — extend there), "allow critical alerts" checkbox (visual only). **No Crew ID** (removed everywhere). → `POST /register` → OTP screen (4–6 digits) → `POST /register/verify-otp` → token + user → FCM token synced → Home.
- **Forgot:** email → `POST /forgot-password`; step 2: paste reset code + new passcode → `POST /reset-password`.
- **Home:** greeting + initials from the profile; chip with module; "Fire risk status" card shows **"Live telemetry isn't connected yet"** (no endpoint); Talk to AI card → assistant list; debug cards (Simulate, scheduled alert 10 s–5 min).
- **Assistant list:** `GET /chats`, newest first, empty/loading/error states, refreshes on resume; no delete (no API). **Chat:** user bubbles right (orange); assistant answer card on the left with sections **Answer (open), Experimental evidence, Prediction (risk chip HIGH red/MEDIUM amber/LOW green + probability + fire/no fire), Observed trends, Interpretation, Safety insight (highlighted, open), Limitations**, shown only when non-empty; sources chips (`EXP-0036`…) open a bottom sheet with the evidence + prediction inputs; `prediction_notice` shown when a prediction exists; banner if `answerability.answerable == false`; machine tags like `insufficient_evidence` are prettified; typing indicator, error bubble with Retry, send disabled while loading; mic = push-to-talk (hold) filling the input; "Read aloud".
- **Emergency flow:** FCM arrives → `EmergencyCoordinator` (alarm + notification if backgrounded) → nav shows `alert` full-screen "incoming call" style (Acknowledge / Silence) → Acknowledge → `steps` screen: red header (module, rack if any, elapsed timer from receive time), alert card (only fields present + backend message), **6 steps from `GET /guide`** with Mark done / progress / "Protocol complete", an "Ask the AI assistant…" dock (typing + send opens the assistant with the question; mic opens the assistant) and a small floating chat button.
- **Guide:** the 6 real steps (cached; works offline). The material-risk/oxygen cards exist only when `USE_FAKE`.
- **Settings:** profile from server (name, email, assigned module with **Change** → `PUT /reset-module`), local-only toggles (voice, alerts), Sign out (`POST /logout` + clear token). Debug-only push-token section.

## 8. Backend contract (Laravel; Bearer token on everything except auth)
`POST /register {name,email,password,password_confirmation,assigned_module}` → `{message, verification_id}` · `POST /register/verify-otp {verification_id,otp}` → `{message, token, user}` · `POST /login {email,password}` → `{message, token, user{id,name,email,assigned_module,fcm_token…}}` · `POST /logout` · `POST /forgot-password {email}` → `{status}` · `POST /reset-password {token,email,password,password_confirmation}` · `GET /me` → `{name, module, email}` (note `module`, not `assigned_module`) · `PUT /fcm-token {fcm_token}` · `PUT /reset-module {assigned_module}` · `GET /chats` → `{chats:[{id,title|null,updated_at…}]}` · `POST /chats` → `{chat:{id…}}` · `GET /chats/{id}` → `{chat:{…, messages:[{id,message,answer:{AI json}}]}}` · `POST /ask {question,chat_id}` → `{message,chat_id,title,data:{answer:{AI json}}}` (the Postman lists it as GET by mistake) · `GET /guide` (**public**, 6 items `{id,step_order,title,body}`) · `POST /simulate` → `{message, module}`; errors if no registered device for the module.
- Error shapes (verified live): **422** `{"message":"The provided credentials are incorrect.","errors":{"email":["…"]}}`; **401** `{"message":"Unauthenticated."}`; forgot-password with an unknown email reveals "We can't find a user…".
- AI answer JSON (`data.answer`): `question, answer, experimental_evidence[{experiment_id,oxygen_concentration,flame_temperature,source,finding}], prediction (list|null; items: experiment_id,fire,label,probability,riskBand,threshold,model,inputs{…},note), observed_trends[], interpretation, safety_insight, limitations[], sources[], model_used, answerability{answerable,decision,reason,…}, prediction_notice`. Greetings/out-of-scope: only `answer` filled, `model_used = conversation_handler`.
- **FCM from `/simulate` (observed live):** message **with a `notification` block** (title `Fire detected-HARMONY`, body `A fire simulation has been triggered in your module.`) and `data = {action: "Open AI Steps"}`. Arrives ~5 s after the call. In **foreground** `onMessageReceived` runs → full-screen alert. In **background** the *system* shows it as a plain notification on the default FCM channel (importance default, no alarm/full-screen) and our service is not called (verified with `dumpsys notification`); tapping it opens the app and `MainActivity.handleIntent` raises the alert from the intent extras.

## 9. Verified vs not verified
**Verified on the phone against the real backend:** splash reachability; login error path ("The provided credentials are incorrect."); sign-up/forgot screens render; the user created an account and logged in; session persists across app restarts (opens Home); Home/Settings show real profile; Guide shows the real 6 steps; `POST /simulate` → FCM → full-screen alarm → Silence/Acknowledge → steps screen (6 steps) → ask from dock opens the assistant and sends the question; clean error text when `/ask` fails; background FCM behavior (see §8). Build OK; lint 0 errors.
**Not verified:** a real AI answer rendered in the card (the AI server was down; the card was verified earlier with the sample JSON in fake mode, incl. bottom sheet, persistence, collapsible sections); mic/TTS audio; change-module and reset-password end-to-end; background/locked **full alarm** (needs data-only push); Android 14+ full-screen-intent permission behavior; gesture navigation mode.

## 10. Blocked items / what others must do
1. **AI service unreachable from the backend:** `POST /ask` → cURL error 6 "Could not resolve host: …trycloudflare.com" (the backend's AI tunnel died/changed). Menna must update the AI URL in her `.env`; also she should return clean errors. The app already shows "The service is unavailable right now. Please try again in a moment." + Retry.
2. **Push must be data-only + high priority** (no `notification` block) with strings: `type="FIRE_ALERT", title, body, alertId, module` (+ optional material, rack, o2, co, airflow, risk, detectedAt). Otherwise background/locked devices only get a plain notification. App-side mitigation already added: manifest default FCM channel → `fire_alerts`.
3. **Stable backend URL** (tunnel is temporary) — update `BASE_URL`.
4. Missing endpoints: telemetry/current station readings (Home risk card), acknowledge/silence/resolve/alert-crew, `detectedAt` in the alert (elapsed timer currently starts at receive time).
5. **Security:** the Firebase **service-account JSON** was shared over chat by the team. It belongs on the backend only; never put it in the app; if it left the team, they should rotate it. (I read it once, did not use it, and never copied it into the project.)
6. AI-quality notes already sent: unstable JSON schema; `safety_insight` must not imply "keep O₂ above 14 %" (cabin nominal is 19.5–23.5 %); fix dense retrieval (`Dense Rank: None`); `prediction` echoed its own input; emergency answers must be short/speakable; the planned **agent** must be read-only in emergency mode and confirm side-effecting actions.
7. Package-name typo (`astronuat`): optional cleanup via a new Firebase app + json.

## 11. Decisions log (why things are the way they are)
- English-only app; Arabic toggle/RTL removed; UI forced LTR; numbers forced to 0–9 (phone locale is Arabic).
- Steps come from `GET /guide` (6 items), with cache + bundled fallback so the crew is never left without steps offline. Old "4 steps / per-material" ideas are gone.
- Chat history is server-side (no Room). New chat is created on the first question to avoid empty chats.
- No mock data in real mode: telemetry card says "not connected"; material-risk/oxygen cards and "Last drill/MET" are demo-only.
- Alerts are gated until signed in (no sound before login, stops on sign-out).
- Errors never show internals (cURL, hosts, stack traces).
- Voice (STT/TTS) is **on-device** (Android SpeechRecognizer/TextToSpeech); no voice endpoints. "Hey Flame" wake phrase is a visual toggle only (not implemented).
- Token stored encrypted (Keystore AES-GCM in DataStore).
- Demo triggers only in debug/fake builds.

## 12. Phone safety rules (learned the hard way — follow them)
- Before **any** tap/swipe/key on the phone: (a) `adb shell dumpsys telephony.registry | grep mCallState` must be `0` (an incoming call was once answered by a blind tap); (b) the focused window must be **our package** `com.flame.astronuat` (`dumpsys window | grep mCurrentFocus`). **Never read or screenshot other apps' screens** (WhatsApp, notification shade, etc.) — if a screenshot accidentally shows private content, ignore it and say nothing about its content. Don't pull the notification shade.
- Never tap by guessed coordinates; read the UI tree (`uiautomator dump`) and tap element centers, or tap a position verified in a fresh screenshot.
- The user must be told before alarms/sound are triggered (Simulate rings the phone).
- Do not create accounts on the team's server or trigger real emails without the user's OK (he made his own test account). Failed-login probes with a fake email were done (harmless).
- Git Bash mangles `/sdcard` paths: capture with `adb -s SERIAL exec-out screencap -p > file.png` (screenshots are 1080×2340; the viewer shows them 923×2000, multiply by 1.17).

### Helper scripts (recreate in `/tmp` under Git Bash if missing)
`ui.py` — reads the UI tree only when our package is focused:
```python
import re,subprocess
adb=r"C:\Users\Abd_Eraman\AppData\Local\Android\Sdk\platform-tools\adb.exe"; S="RZCX91V2S0Z"; PKG="com.flame.astronuat"
def focus():
    out=subprocess.run([adb,"-s",S,"shell","dumpsys","window"],capture_output=True).stdout.decode("utf-8","ignore")
    return next((l for l in out.splitlines() if "mCurrentFocus" in l),"")
def dump():
    if PKG not in focus(): return ""          # never read another app
    return subprocess.run([adb,"-s",S,"exec-out","uiautomator","dump","/dev/tty"],capture_output=True).stdout.decode("utf-8","ignore")
def nodes(xml):
    r=[]
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',xml):
        t,d,x1,y1,x2,y2=m.groups()
        if t or d: r.append((t or d,(int(x1)+int(x2))//2,(int(y1)+int(y2))//2))
    return r
```
`tap.py` — guarded taps (run python with `PYTHONIOENCODING=utf-8`):
```python
import sys,subprocess,time; sys.path.insert(0,"/tmp"); import ui
def call_active():
    out=subprocess.run([ui.adb,"-s",ui.S,"shell","dumpsys","telephony.registry"],capture_output=True).stdout.decode("utf-8","ignore")
    return any(l.split("=")[1].strip()!="0" for l in out.splitlines() if l.strip().startswith("mCallState="))
def screen(): return ui.nodes(ui.dump())
def guard():
    if call_active(): print("ABORT: call active"); sys.exit(2)
def tap_xy(x,y): guard(); subprocess.run([ui.adb,"-s",ui.S,"shell","input","tap",str(x),str(y)])
def tap_text(label):
    guard()
    for t,x,y in screen():
        if label in t: tap_xy(x,y); time.sleep(1.5); return True
    print("NOT FOUND:",label); return False
```
Typing: `adb shell input text "Hello%sworld"` (`%s` = space). Install: `adb -s SERIAL install -r app-debug.apk`; launch: `adb shell monkey -p com.flame.astronuat -c android.intent.category.LAUNCHER 1`; logs: `adb logcat -d -s FlamePush AndroidRuntime:E`; check our notifications only: `adb shell dumpsys notification --noredact | grep -A40 "pkg=com.flame.astronuat" | grep -E "channel=|android.title|android.text"`.

## 13. Gotchas
- PowerShell `Get-Content/Set-Content/ReadAllText` can corrupt `₂ — · …`; use the Write/Edit tools, or `[IO.File]::ReadAllText($f,[Text.UTF8Encoding]::new($false))`. After editing a file via PowerShell, **Read it again** before using the Edit tool.
- Kotlin: no `return` in default parameter values; `String.format` uses the phone locale → use `fmtUS()`; Retrofit 3 converter import is `retrofit2.converter.kotlinx.serialization.asConverterFactory`.
- DataStore reads in the OkHttp interceptor use `runBlocking` (on OkHttp's thread; fine).
- `MainActivity.handleIntent` runs on every recreate; de-duplication relies on the FCM message id in `RealEmergencyRepository.seen`.
- The old files `API_REQUIREMENTS.md` / `API_ENDPOINTS_USED.md` in the project root describe the **old** API — ignore.

## 14. Test checklist (use when the backend/AI are back)
1. Login (existing account) → Home shows name/module; kill + reopen → still signed in.
2. Home → Talk to AI → New chat → a real question → answer card (sections, sources sheet, notice, read-aloud, mic).
3. Safety Guide → 6 steps; airplane mode → still shows (cache).
4. Home (debug) → Simulate Emergency → FCM → alert screen → Acknowledge → steps → Mark done ×6 → "Protocol complete".
5. Background: schedule 10 s, press Home → with a **data-only** push the full alarm should appear; check `FlamePush` log.
6. Locked screen: lock the phone, trigger → alarm over the lock screen (Android 14+: allow "full-screen intents" for the app in system settings).
7. Settings → Change module → verify `GET /me` shows it; Sign out → back to Login; stale token → 401 → Login.
8. Test gesture navigation mode and a release build (HTTPS only).

## 15. Backlog / ideas
- Re-verify the Assistant with real AI data; handle the AI "agent" events (tool status) if the team adds streaming.
- Optional screens proposed to the user: Incident Log, Drill Mode, Hands-Free Mode, Notifications center, Equipment locator.
- Implement "Hey Flame" wake phrase; polish STT/TTS (Arabic dropped).
- Real telemetry for Home when an endpoint exists; acknowledge/resolve calls; `detectedAt`.
- Release hardening: stable HTTPS URL, correct package id + new `google-services.json`, signed release, maybe R8.
