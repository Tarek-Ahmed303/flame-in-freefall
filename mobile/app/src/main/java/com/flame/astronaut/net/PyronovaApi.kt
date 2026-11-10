package com.flame.astronaut.net

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Backend contract (Postman collection "pyronova"). Bearer token + Accept: application/json are added by [Network]. */
interface PyronovaApi {
    @POST("register") suspend fun register(@Body body: RegisterRequest): RegisterResponse
    @POST("register/verify-otp") suspend fun verifyOtp(@Body body: VerifyOtpRequest): AuthResponse
    @POST("login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("logout") suspend fun logout(): MessageResponse
    @POST("forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse
    @POST("reset-password") suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse
    @GET("me") suspend fun me(): UserDto

    @PUT("fcm-token") suspend fun updateFcmToken(@Body body: FcmTokenRequest): MessageResponse
    @PUT("reset-module") suspend fun resetModule(@Body body: ResetModuleRequest): MessageResponse

    @GET("chats") suspend fun chats(): ChatsResponse
    @POST("chats") suspend fun createChat(): ChatEnvelope
    @GET("chats/{id}") suspend fun chat(@Path("id") id: Long): ChatEnvelope
    @POST("ask") suspend fun ask(@Body body: AskBody): AskEnvelope

    /** Raw body: parsed + cached by the repository. */
    @GET("guide") suspend fun guide(): ResponseBody

    @POST("simulate") suspend fun simulate(): SimulateResponse
}
