package com.flame.astronaut.net

import com.flame.astronaut.assistant.AskResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire models for the backend (Postman collection "pyronova"). Everything is optional/defaulted so a missing
// field never crashes the app.

// ---- auth
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("assigned_module") val assignedModule: String,
)

@Serializable
data class RegisterResponse(
    val message: String? = null,
    @SerialName("verification_id") val verificationId: String? = null,
)

@Serializable
data class VerifyOtpRequest(
    @SerialName("verification_id") val verificationId: String,
    val otp: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

/** `assigned_module` comes from /login and /register/verify-otp, `module` from /me. */
@Serializable
data class UserDto(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("assigned_module") val assignedModule: String? = null,
    val module: String? = null,
)

@Serializable
data class AuthResponse(
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null,
)

/** `message` for most endpoints, `status` for forgot/reset password. */
@Serializable
data class MessageResponse(val message: String? = null, val status: String? = null)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class FcmTokenRequest(@SerialName("fcm_token") val fcmToken: String)

@Serializable
data class ResetModuleRequest(@SerialName("assigned_module") val assignedModule: String)

// ---- chats
@Serializable
data class ChatMessageDto(
    val id: Long? = null,
    val message: String? = null,
    val answer: AskResponse? = null,
)

@Serializable
data class ChatDto(
    val id: Long = 0,
    val title: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val messages: List<ChatMessageDto>? = null,
)

@Serializable
data class ChatsResponse(val chats: List<ChatDto>? = null)

@Serializable
data class ChatEnvelope(val message: String? = null, val chat: ChatDto? = null)

@Serializable
data class AskBody(
    val question: String,
    @SerialName("chat_id") val chatId: Long? = null,
)

@Serializable
data class AskData(val answer: AskResponse? = null)

@Serializable
data class AskEnvelope(
    val message: String? = null,
    @SerialName("chat_id") val chatId: Long? = null,
    val title: String? = null,
    val data: AskData? = null,
)

// ---- guide / simulate / errors
@Serializable
data class GuideStepDto(
    val id: Long? = null,
    @SerialName("step_order") val stepOrder: Int? = null,
    val title: String? = null,
    val body: String? = null,
)

@Serializable
data class SimulateResponse(val message: String? = null, val module: String? = null)

/** Laravel validation/error body. The `trace`/`file` keys a debug server adds are never read. */
@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
)
