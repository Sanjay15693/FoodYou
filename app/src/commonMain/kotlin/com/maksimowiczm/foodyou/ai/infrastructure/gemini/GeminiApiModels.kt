package com.maksimowiczm.foodyou.ai.infrastructure.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for Gemini API generateContent endpoint.
 */
@Serializable
internal data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
) {
    @Serializable
    data class Content(val parts: List<Part>, val role: String = "user")

    @Serializable data class Part(val text: String)

    @Serializable
    data class GenerationConfig(
        val temperature: Double? = null,
        val topK: Int? = null,
        val topP: Double? = null,
        val maxOutputTokens: Int? = null,
        val responseMimeType: String? = null,
    )
}

/**
 * Response model for Gemini API generateContent endpoint.
 */
@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata? = null,
) {
    @Serializable
    data class Candidate(
        val content: Content,
        val finishReason: String? = null,
        val safetyRatings: List<SafetyRating>? = null,
    )

    @Serializable data class Content(val parts: List<Part>, val role: String)

    @Serializable data class Part(val text: String)

    @Serializable
    data class SafetyRating(
        val category: String,
        val probability: String,
    )

    @Serializable
    data class UsageMetadata(
        val promptTokenCount: Int,
        val candidatesTokenCount: Int,
        val totalTokenCount: Int,
    )
}

/**
 * Error response from Gemini API.
 */
@Serializable
internal data class GeminiErrorResponse(
    val error: Error,
) {
    @Serializable
    data class Error(
        val code: Int,
        val message: String,
        val status: String,
    )
}
