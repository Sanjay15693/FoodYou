package com.maksimowiczm.foodyou.ai.infrastructure.gemini

import com.maksimowiczm.foodyou.common.log.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Client for interacting with the Google Gemini API.
 *
 * @param httpClient The Ktor HTTP client for making requests.
 * @param logger Logger for debugging and error tracking.
 */
internal class GeminiApiClient(
    private val httpClient: HttpClient,
    private val logger: Logger,
) {
    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val DEFAULT_MODEL = "gemini-2.5-flash-preview-09-2025"
        private const val API_KEY_HEADER = "x-goog-api-key"
        private const val TAG = "GeminiApiClient"
    }

    /**
     * Generates content using the Gemini API.
     *
     * @param prompt The text prompt to send to the model.
     * @param apiKey The Google AI API key.
     * @param model The model to use (default: gemini-1.5-flash).
     * @return The generated text response.
     * @throws GeminiApiException if the API request fails.
     */
    suspend fun generateContent(
        prompt: String,
        apiKey: String,
        model: String = DEFAULT_MODEL,
    ): String {
        logger.d(TAG) { "Generating content with model: $model" }

        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(
                    parts = listOf(GeminiRequest.Part(text = prompt)),
                    role = "user"
                )
            )
        )

        return try {
            val response = httpClient.post("$BASE_URL/$model:generateContent") {
                header(API_KEY_HEADER, apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.e(TAG) { "Gemini API error response: $errorBody" }
                val errorResponse = try {
                    response.body<GeminiErrorResponse>()
                } catch (e: Exception) {
                    logger.e(TAG, e) { "Failed to parse Gemini error response: $errorBody" }
                    throw GeminiApiException(
                        "Gemini API error: ${response.status.value} - $errorBody",
                        response.status.value,
                        e
                    )
                }
                throw GeminiApiException(
                    "Gemini API error: ${errorResponse.error.message}",
                    errorResponse.error.code
                )
            }

            val geminiResponse = response.body<GeminiResponse>()
            val generatedText = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw GeminiApiException("No content generated", null)

            logger.d(TAG) { "Successfully generated content" }
            generatedText
        } catch (e: GeminiApiException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, e) { "Failed to generate content: ${e.message}" }
            throw GeminiApiException("Failed to generate content: ${e.message}", null, e)
        }
    }
}

/**
 * Exception thrown when Gemini API requests fail.
 */
class GeminiApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)
