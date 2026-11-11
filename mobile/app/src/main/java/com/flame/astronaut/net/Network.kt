package com.flame.astronaut.net

import com.flame.astronaut.BuildConfig
import com.flame.astronaut.assistant.AssistantJson
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.receiveAsFlow

/** Signals that the server rejected our token (401) — the app clears the session and goes to the login screen. */
class SessionManager(private val tokens: TokenStore) {
    // A Channel keeps the event until the nav host collects it (a 401 can arrive before it starts listening).
    private val _loggedOut = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    val loggedOut: kotlinx.coroutines.flow.Flow<Unit> = _loggedOut.receiveAsFlow()

    fun onUnauthorized() {
        runBlocking { tokens.clear() }
        _loggedOut.trySend(Unit)
    }
}

/**
 * OkHttp + Retrofit for the backend. BASE_URL comes from BuildConfig (the temporary tunnel changes — see
 * app/build.gradle.kts). Timeouts: connect 15 s, read/write 90 s because the LLM is slow.
 */
class Network(private val tokens: TokenStore, private val session: SessionManager) {

    private val publicPaths = setOf("register", "register/verify-otp", "login", "forgot-password", "reset-password")

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath.trim('/')
            val isPublic = path in publicPaths
            val builder = original.newBuilder().header("Accept", "application/json")
            val token = if (isPublic) null else tokens.currentToken()
            if (token != null) builder.header("Authorization", "Bearer $token")
            val response = chain.proceed(builder.build())
            // 401 on a protected call with a token = expired/revoked session
            if (response.code == 401 && token != null) session.onUnauthorized()
            response
        }
        .build()

    val api: PyronovaApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(AssistantJson.instance.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PyronovaApi::class.java)
}
