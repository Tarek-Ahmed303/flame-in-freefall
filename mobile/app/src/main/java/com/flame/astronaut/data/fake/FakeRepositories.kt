package com.flame.astronaut.data.fake

import android.content.Context
import com.flame.astronaut.assistant.AskResponse
import com.flame.astronaut.assistant.AssistantJson
import com.flame.astronaut.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Demo / offline implementations. Only used when BuildConfig.USE_FAKE is true (flame.useFake=true in local.properties).

class FakeEmergencyRepository(
    private val scope: CoroutineScope,
    private val keepAwake: (Long) -> Unit = {},
) : EmergencyRepository {

    private val _alerts = MutableSharedFlow<AlertEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val alerts: Flow<AlertEvent> = _alerts.asSharedFlow()

    private val _current = MutableStateFlow<FireAlert?>(null)
    override val currentAlert: StateFlow<FireAlert?> = _current.asStateFlow()

    private val _scheduledAt = MutableStateFlow<Long?>(null)
    override val scheduledAt: StateFlow<Long?> = _scheduledAt.asStateFlow()
    private var scheduleJob: Job? = null

    override val notices: Flow<String> = emptyFlow()

    override suspend fun simulate(): Result<String> {
        fire(AlertPresentation.BANNER)
        return Result.success("Simulated locally (demo mode).")
    }

    override fun scheduleAlert(delayMs: Long) {
        scheduleJob?.cancel()
        keepAwake(delayMs + 5_000)
        _scheduledAt.value = System.currentTimeMillis() + delayMs
        scheduleJob = scope.launch {
            delay(delayMs)
            _scheduledAt.value = null
            fire(AlertPresentation.FULL_SCREEN)
        }
    }

    override fun cancelScheduled() {
        scheduleJob?.cancel()
        _scheduledAt.value = null
    }

    override suspend fun resolve() {
        _current.value = null
    }

    override fun onPushAlert(alert: FireAlert) {
        _current.value = alert
        _alerts.tryEmit(AlertEvent(alert, AlertPresentation.FULL_SCREEN))
    }

    private fun fire(presentation: AlertPresentation) {
        val alert = FireAlert(
            id = "demo-${System.currentTimeMillis()}",
            module = "Harmony",
            detectedAtMillis = System.currentTimeMillis() - 47_000,
            material = "PMMA", rack = "LAB1O3", o2Percent = 19.0, coPpm = 38, airflow = 0.08,
        )
        _current.value = alert
        _alerts.tryEmit(AlertEvent(alert, presentation))
    }
}

class FakeGuideRepository : GuideRepository {
    override suspend fun getGuide() = GuideContent(
        fireSteps = DEFAULT_STEPS,
        materials = listOf(
            MaterialRisk("PMMA (acrylic)", .86f, RiskLevel.HIGH),
            MaterialRisk("Polycarbonate", .72f, RiskLevel.HIGH),
            MaterialRisk("Cotton fabric", .54f, RiskLevel.MED),
            MaterialRisk("Nomex", .16f, RiskLevel.LOW),
        ),
        o2Now = 21.3,
    )

    companion object {
        val DEFAULT_STEPS = listOf(
            Step("Protect Yourself", "Protect yourself from smoke and hazardous combustion products using approved protective equipment and following emergency procedures."),
            Step("Report the Fire to the Crew", "Immediately alert the crew and follow the spacecraft fire-reporting and warning procedures."),
            Step("Isolate Ignition Sources", "Isolate affected equipment from its power source only when directed by the spacecraft approved emergency procedures."),
            Step("Suppress the Fire", "Use approved fire-suppression equipment according to crew training and spacecraft-specific procedures."),
            Step("Check the Atmosphere", "Check for smoke and harmful combustion products before considering the surrounding atmosphere safe."),
            Step("Restore Safe Conditions", "Resume normal operations only after the atmosphere and affected equipment have been assessed and declared safe under approved procedures."),
        )
    }
}

class FakeRiskRepository : RiskRepository {
    override fun observeRisk(): Flow<RiskState> = flow {
        var ago = 4
        while (true) {
            emit(
                RiskState(
                    index = 23, level = RiskLevel.LOW, o2 = 21.3, pressure = 101.2, airflow = 0.14, watchMaterial = "PMMA",
                    trend = listOf(.55f, .62f, .5f, .58f, .54f, .6f, .56f, .52f, .57f, .53f),
                    updatedSecondsAgo = ago, trendText = "Risk is stable.",
                ),
            )
            delay(1000)
            ago = (ago + 1) % 30
        }
    }
}

class FakeHealthRepository : HealthRepository {
    override suspend fun check(): HealthStatus {
        delay(800)
        return HealthStatus(backendOk = true, aiOnline = true)
    }
}

class FakeAuthRepository : AuthRepository {
    private val _user = MutableStateFlow<UserProfile?>(null)
    override val user: StateFlow<UserProfile?> = _user.asStateFlow()

    override fun isLoggedIn() = _user.value != null

    private var pendingName = "Demo Crew"
    private var pendingEmail = "demo@crew.station"
    private var pendingModule = AssignedModule.HARMONY

    override suspend fun register(name: String, email: String, password: String, passwordConfirmation: String, module: AssignedModule): Result<String> {
        delay(600)
        pendingName = name; pendingEmail = email; pendingModule = module
        return Result.success("demo-verification-id")
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<Unit> {
        delay(500)
        _user.value = UserProfile(pendingName, pendingEmail, pendingModule.name)
        return Result.success(Unit)
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        delay(600)
        _user.value = UserProfile("Demo Crew", email.ifBlank { "demo@crew.station" }, AssignedModule.HARMONY.name)
        return Result.success(Unit)
    }

    override suspend fun logout() { _user.value = null }
    override suspend fun forgotPassword(email: String) = Result.success("We have emailed your password reset link.")
    override suspend fun resetPassword(token: String, email: String, password: String, passwordConfirmation: String) = Result.success("Your password has been reset.")
    override suspend fun refreshProfile(): Result<UserProfile> = _user.value?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Not signed in"))

    override suspend fun resetModule(module: AssignedModule): Result<Unit> {
        _user.value = _user.value?.copy(module = module.name)
        return Result.success(Unit)
    }

    override suspend fun syncFcmToken() {}
}

/** In-memory chats; every answer is the sample AI response bundled in assets/fake_ask_response.json. */
class FakeChatRepository(private val context: Context) : ChatRepository {
    private data class Chat(val id: Long, var title: String?, val turns: MutableList<ChatTurn> = mutableListOf(), var updated: Long = System.currentTimeMillis())
    private val chats = mutableListOf<Chat>()
    private var nextId = 1L

    private fun sample(): AskResponse =
        context.assets.open("fake_ask_response.json").bufferedReader(Charsets.UTF_8).use { it.readText() }.let(AssistantJson::parse)

    override suspend fun chats(): Result<List<ChatSummary>> =
        Result.success(chats.sortedByDescending { it.updated }.map { ChatSummary(it.id, it.title, null) })

    override suspend fun chat(id: Long): Result<ChatDetail> =
        chats.firstOrNull { it.id == id }?.let { Result.success(ChatDetail(it.id, it.title, it.turns.toList())) }
            ?: Result.failure(IllegalStateException("Chat not found."))

    override suspend fun createChat(): Result<Long> = Chat(nextId++, null).also { chats.add(it) }.let { Result.success(it.id) }

    override suspend fun ask(question: String, chatId: Long): Result<AskResult> {
        delay(1200)
        val chat = chats.firstOrNull { it.id == chatId } ?: return Result.failure(IllegalStateException("Chat not found."))
        val answer = sample().copy(question = question)
        chat.turns.add(ChatTurn(chat.turns.size + 1L, question, answer))
        if (chat.title == null) chat.title = question.take(50)
        chat.updated = System.currentTimeMillis()
        return Result.success(AskResult(chat.id, chat.title, answer))
    }
}
