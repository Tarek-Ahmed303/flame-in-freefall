package com.flame.astronaut.net

import com.flame.astronaut.assistant.AssistantJson
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * What the user sees for a failed call. Only the backend's `message` (or the first validation error) is used —
 * a debug server may add a stack trace to the body; it is never shown.
 */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> httpMessage()
    is SocketTimeoutException -> "The server took too long to answer. Please try again."
    is UnknownHostException, is ConnectException -> "Can't reach the server. Check your connection and try again."
    is IOException -> "Network problem. Check your connection and try again."
    is SerializationException -> "Unexpected response from the server."
    else -> message?.takeIf { it.isNotBlank() && it.length < 160 } ?: "Something went wrong. Please try again."
}

private fun HttpException.httpMessage(): String {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = body?.let { runCatching { AssistantJson.instance.decodeFromString(ApiErrorBody.serializer(), it) }.getOrNull() }
    val firstField = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
    // Never show internal details (cURL errors, hosts, SQL, class names, paths) that a server error may carry.
    val text = (firstField ?: parsed?.message)?.takeIf { it.isNotBlank() && !it.looksInternal() }
    if (text != null) return text
    return when (code()) {
        401 -> "Your session has expired. Please sign in again."
        403 -> "You don't have permission to do that."
        404 -> "Not found."
        422 -> "Please check the information you entered."
        429 -> "Too many attempts. Please wait a moment."
        in 500..599 -> "The service is unavailable right now. Please try again in a moment."
        else -> "Request failed (${code()})."
    }
}

private val internalMarkers = listOf(
    "curl", "http://", "https://", "sqlstate", "exception", "stack", "vendor", "resolve host", "connection refused",
    "timed out", "guzzle", "illuminate", "\\", ".php", "trycloudflare",
)

private fun String.looksInternal(): Boolean = length > 220 || internalMarkers.any { contains(it, ignoreCase = true) }

/** Friendly rewrite for the one known /simulate failure. */
fun String.friendlySimulateMessage(): String =
    if (contains("No registered devices", ignoreCase = true))
        "No device is registered for your module yet. Sign in on this phone (so it can register for notifications) and try again."
    else this
