package com.flame.astronaut.push

import com.flame.astronaut.domain.FireAlert
import com.flame.astronaut.domain.RiskLevel
import java.time.Instant

/**
 * The FCM payload sent by POST /simulate is not documented yet, so this parses defensively: every field is
 * optional, several key names are accepted, and anything missing is simply left out of the UI.
 * (MainActivity / FlameMessagingService log the real keys so the shape can be confirmed.)
 */
fun parsePushAlert(
    data: Map<String, String>,
    title: String?,
    body: String?,
    messageId: String?,
    fallbackModule: String?,
): FireAlert {
    fun first(vararg keys: String) = keys.firstNotNullOfOrNull { k -> data[k]?.takeIf { it.isNotBlank() } }
    fun num(vararg keys: String) = first(*keys)?.toDoubleOrNull()

    val risk = when (first("risk", "risk_level", "level", "riskBand")?.uppercase()) {
        "LOW" -> RiskLevel.LOW
        "MED", "MEDIUM" -> RiskLevel.MED
        else -> RiskLevel.HIGH // a fire alert defaults to the most serious level
    }
    return FireAlert(
        id = first("alertId", "alert_id", "id") ?: messageId ?: "PUSH-${System.currentTimeMillis()}",
        module = first("module", "assigned_module") ?: fallbackModule ?: "your module",
        detectedAtMillis = first("detectedAt", "detected_at", "time")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: System.currentTimeMillis(),
        material = first("material"),
        rack = first("rack")?.removePrefix("RACK ")?.removePrefix("Rack "),
        o2Percent = num("o2", "oxygen"),
        coPpm = num("co")?.toInt(),
        airflow = num("airflow"),
        risk = risk,
        temperature = num("temperature"),
        message = body?.takeIf { it.isNotBlank() } ?: first("message", "body") ?: title,
    )
}
