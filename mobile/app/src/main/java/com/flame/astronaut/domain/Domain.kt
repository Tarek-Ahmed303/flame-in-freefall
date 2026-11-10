package com.flame.astronaut.domain

import com.flame.astronaut.assistant.AskResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// ---------- Models ----------

enum class RiskLevel { LOW, MED, HIGH }

/** Modules a crew member can be assigned to. Keep the list in one place — extend here when the backend adds more. */
enum class AssignedModule(val label: String) {
    HARMONY("Harmony"),
    KIBO("Kibo");

    companion object {
        fun fromApi(value: String?): AssignedModule? = entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}

data class UserProfile(val name: String, val email: String, val module: String)

/**
 * A fire alert. The FCM payload of POST /simulate is not documented yet, so everything except [id], [module]
 * and the time is optional and the UI only shows what is present.
 */
data class FireAlert(
    val id: String,
    val module: String,
    val detectedAtMillis: Long,
    val material: String? = null,
    val rack: String? = null,
    val o2Percent: Double? = null,
    val coPpm: Int? = null,
    val airflow: Double? = null,
    val risk: RiskLevel = RiskLevel.HIGH,
    val temperature: Double? = null,
    /** Free text from the push (notification body), used when structured fields are missing. */
    val message: String? = null,
)

/** One-line summary built only from the fields that exist. */
fun FireAlert.summaryLine(): String {
    val parts = buildList {
        if (material != null) add(if (rack != null) "$material at rack $rack" else material)
        else if (rack != null) add("Rack $rack")
        if (o2Percent != null) add("O₂ ${String.format(java.util.Locale.US, "%.1f", o2Percent)}%")
    }
    return when {
        parts.isNotEmpty() -> parts.joinToString(" · ")
        !message.isNullOrBlank() -> message
        else -> "Open the emergency steps."
    }
}

enum class AlertPresentation { FULL_SCREEN, BANNER }

data class AlertEvent(val alert: FireAlert, val presentation: AlertPresentation)

data class Step(val title: String, val body: String)

data class RiskState(
    val index: Int,
    val level: RiskLevel,
    val o2: Double,
    val pressure: Double,
    val airflow: Double,
    val watchMaterial: String,
    val trend: List<Float>,
    val updatedSecondsAgo: Int,
    val trendText: String = "",
)

data class MaterialRisk(val name: String, val fraction: Float, val level: RiskLevel)

/** [materials] and [o2Now] only exist in demo (fake) mode: the backend has no endpoint for them. */
data class GuideContent(
    val fireSteps: List<Step>,
    val materials: List<MaterialRisk>? = null,
    val o2Now: Double? = null,
)

data class HealthStatus(val backendOk: Boolean, val aiOnline: Boolean)

// ---------- AI assistant (via the backend) ----------

data class ChatSummary(val id: Long, val title: String?, val updatedAt: String?)
data class ChatTurn(val id: Long, val question: String, val answer: AskResponse?)
data class ChatDetail(val id: Long, val title: String?, val turns: List<ChatTurn>)
data class AskResult(val chatId: Long, val title: String?, val answer: AskResponse)

interface ChatRepository {
    suspend fun chats(): Result<List<ChatSummary>>
    suspend fun chat(id: Long): Result<ChatDetail>
    suspend fun createChat(): Result<Long>
    suspend fun ask(question: String, chatId: Long): Result<AskResult>
}

// ---------- Repository contracts (real = Retrofit, fake = demo; chosen once in AppContainer by BuildConfig.USE_FAKE) ----------

interface EmergencyRepository {
    /** Emits whenever a fire alert arrives (FCM push, or a local demo alert in fake mode). */
    val alerts: Flow<AlertEvent>

    /** The alert currently being handled, null when none. */
    val currentAlert: StateFlow<FireAlert?>

    /** Debug/demo: POST /simulate — the backend then pushes a FCM alert to this module's devices. Result = backend message. */
    suspend fun simulate(): Result<String>

    suspend fun resolve()

    /** Epoch millis at which a scheduled demo alert will fire, null when nothing is scheduled. */
    val scheduledAt: StateFlow<Long?>

    /** Demo: trigger a simulated alert after [delayMs] (works with the app in background / phone locked). */
    fun scheduleAlert(delayMs: Long)

    fun cancelScheduled()

    /** Messages for the user about demo triggers (e.g. "No device registered for this module"). */
    val notices: Flow<String>

    /** An alert delivered from outside the repository (FCM push). May be called from any thread. */
    fun onPushAlert(alert: FireAlert)
}

interface RiskRepository {
    fun observeRisk(): Flow<RiskState>
}

interface GuideRepository {
    /** Never throws and never returns an empty list: network → last cached copy → bundled fallback. */
    suspend fun getGuide(): GuideContent
}

interface HealthRepository {
    suspend fun check(): HealthStatus
}

interface AuthRepository {
    /** Cached profile of the signed-in user (null when signed out). */
    val user: StateFlow<UserProfile?>

    fun isLoggedIn(): Boolean

    /** Returns the verification id to use with [verifyOtp]. */
    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String, module: AssignedModule): Result<String>
    suspend fun verifyOtp(verificationId: String, otp: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun forgotPassword(email: String): Result<String>
    suspend fun resetPassword(token: String, email: String, password: String, passwordConfirmation: String): Result<String>
    suspend fun refreshProfile(): Result<UserProfile>
    suspend fun resetModule(module: AssignedModule): Result<Unit>

    /** PUT /fcm-token with the current device token (no-op when signed out or Firebase is not configured). */
    suspend fun syncFcmToken()
}
