package com.flame.astronaut.assistant

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonTransformingSerializer

// The AI payload (`data.answer` of POST /ask and `answer` of each message in GET /chats/{id}).
// Every field is nullable with a default, so a missing/null field never crashes the app.
// Casual or out-of-scope questions may return only "answer" — the UI renders only what is non-empty.

@Serializable
data class EvidenceItem(
    @SerialName("experiment_id") val experimentId: String? = null,
    @SerialName("oxygen_concentration") val oxygenConcentration: Double? = null,
    @SerialName("flame_temperature") val flameTemperature: Double? = null,
    val source: String? = null,
    val finding: String? = null,
)

@Serializable
data class PredictionInputs(
    @SerialName("oxygen_concentration_pct") val oxygenConcentrationPct: Double? = null,
    @SerialName("pressure_kpa") val pressureKpa: Double? = null,
    @SerialName("flow_velocity_cm_s") val flowVelocityCmS: Double? = null,
    @SerialName("ambient_temperature_c") val ambientTemperatureC: Double? = null,
    val material: String? = null,
    @SerialName("material_type") val materialType: String? = null,
    @SerialName("flow_configuration") val flowConfiguration: String? = null,
)

@Serializable
data class PredictionItem(
    @SerialName("experiment_id") val experimentId: String? = null,
    val fire: Boolean? = null,
    val label: String? = null,
    val probability: Double? = null,
    val riskBand: String? = null,
    val threshold: Double? = null,
    val model: String? = null,
    val inputs: PredictionInputs? = null,
    val note: String? = null,
)

@Serializable
data class Answerability(
    val answerable: Boolean? = null,
    val decision: String? = null,
    val reason: String? = null,
    @SerialName("top_score") val topScore: Double? = null,
    @SerialName("query_coverage") val queryCoverage: Double? = null,
    @SerialName("candidate_count") val candidateCount: Int? = null,
)

/** Older server builds returned `prediction` as a single object or null; accept all three shapes. */
@OptIn(ExperimentalSerializationApi::class)
object PredictionListSerializer : JsonTransformingSerializer<List<PredictionItem>>(ListSerializer(PredictionItem.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> element
        is JsonNull -> JsonArray(emptyList())
        else -> JsonArray(listOf(element))
    }
}

@Serializable
data class AskResponse(
    val question: String? = null,
    val answer: String? = null,
    @SerialName("experimental_evidence") val experimentalEvidence: List<EvidenceItem>? = null,
    @Serializable(with = PredictionListSerializer::class) val prediction: List<PredictionItem>? = null,
    @SerialName("observed_trends") val observedTrends: List<String>? = null,
    val interpretation: String? = null,
    @SerialName("safety_insight") val safetyInsight: String? = null,
    val limitations: List<String>? = null,
    val sources: List<String>? = null,
    @SerialName("model_used") val modelUsed: String? = null,
    val answerability: Answerability? = null,
    @SerialName("prediction_notice") val predictionNotice: String? = null,
)

object AssistantJson {
    val instance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun parse(raw: String): AskResponse = instance.decodeFromString(AskResponse.serializer(), raw.trim().removePrefix("\uFEFF"))
    fun parseOrNull(raw: String?): AskResponse? = raw?.let { runCatching { parse(it) }.getOrNull() }
}

const val DEFAULT_PREDICTION_NOTICE =
    "ML predictions use simulated training data and are not certified fire-safety guidance."
