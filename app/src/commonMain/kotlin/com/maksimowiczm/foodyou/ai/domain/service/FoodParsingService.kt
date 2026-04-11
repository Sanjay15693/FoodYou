package com.maksimowiczm.foodyou.ai.domain.service

import com.maksimowiczm.foodyou.ai.domain.entity.ParsedFoodEntry
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiException
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.log.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for parsing natural language food descriptions into structured food entries using AI.
 *
 * This service uses the Gemini API to convert user-provided food descriptions (e.g., "2 slices of
 * pizza") into structured data with nutrition information.
 */
internal class FoodParsingService(
    private val geminiApiClient: GeminiApiClient,
    private val logger: Logger,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val TAG = "FoodParsingService"

    /**
     * Parses a natural language food description into structured food entries.
     *
     * @param description Natural language description of food (e.g., "2 slices of pizza and a coke").
     * @return List of parsed food entries.
     * @throws FoodParsingException if parsing fails.
     */
    suspend fun parseFoodDescription(
        description: String,
        apiKey: String,
        model: String? = null,
    ): List<ParsedFoodEntry> {
        logger.d(TAG) { "Parsing food description: $description" }

        if (apiKey.isBlank()) {
            throw FoodParsingException("Google AI API key is empty")
        }

        val prompt = buildPrompt(description)

        return try {
            val response = geminiApiClient.generateContent(prompt, apiKey, model)
            parseResponse(response)
        } catch (e: GeminiApiException) {
            logger.e(TAG, e) { "Gemini API error: ${e.message}" }
            throw FoodParsingException("AI service error: ${e.message}", e)
        } catch (e: Exception) {
            logger.e(TAG, e) { "Unexpected error: ${e.message}" }
            throw FoodParsingException("Failed to parse food: ${e.message}", e)
        }
    }

    private fun buildPrompt(description: String): String =
        """
        You are a nutrition expert. Parse the following food description and return a JSON array of food items with their nutrition information.
        
        Food description: "$description"
        
        Return ONLY a valid JSON array with this exact structure (no additional text):
        [
          {
            "foodName": "string (name of the food item)",
            "quantity": "string (serving size, e.g., '2 slices', '1 cup')",
            "itemCount": number (number of items, e.g., 2 for '2 slices', default 1),
            "isLiquid": boolean (true if the food is a liquid like milk, juice, soda; false otherwise),
            "weightGrams": number (estimated weight in grams for solids, or volume in ml for liquids),
            "protein": number (protein in grams per 100g/ml),
            "carbs": number (carbohydrates in grams per 100g),
            "fat": number (fat in grams per 100g),
            "fiber": number (fiber in grams per 100g, can be 0 if unknown),
            "sugar": number (sugar in grams per 100g, can be 0 if unknown)
          }
        ]
        
        Important:
        - Return nutrition values per 100g (or 100ml for liquids)
        - Provide reasonable estimates based on common nutrition data
        - If you cannot parse the description, return an empty array []
        - Return ONLY the JSON array, no markdown formatting or additional text
        """
            .trimIndent()

    private fun parseResponse(response: String): List<ParsedFoodEntry> {
        logger.d(TAG) { "Parsing AI response: $response" }

        return try {
            val foodItems = json.decodeFromString<List<FoodItemResponse>>(response.trim())
            val parsedEntries = foodItems.map { it.toParsedFoodEntry() }
            logger.d(TAG) { "Parsed ${parsedEntries.size} food entries from AI response" }
            parsedEntries
        } catch (e: Exception) {
            logger.e(TAG, e) { "Failed to parse JSON response: ${e.message}" }
            throw FoodParsingException("Failed to parse AI response: ${e.message}", e)
        }
    }

    @Serializable
    private data class FoodItemResponse(
        val foodName: String,
        val quantity: String,
        val itemCount: Int = 1,
        val isLiquid: Boolean = false,
        val weightGrams: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val fiber: Double = 0.0,
        val sugar: Double = 0.0,
    ) {
        fun toParsedFoodEntry(): ParsedFoodEntry {
            val calculatedCalories = (protein * 4) + (carbs * 4) + (fat * 9)
            return ParsedFoodEntry(
                foodName = foodName,
                quantity = quantity,
                itemCount = itemCount,
                isLiquid = isLiquid,
                weightGrams = weightGrams,
                nutritionFacts =
                    NutritionFacts(
                        energy = NutrientValue.from(calculatedCalories),
                        proteins = NutrientValue.from(protein),
                        carbohydrates = NutrientValue.from(carbs),
                        fats = NutrientValue.from(fat),
                        dietaryFiber = NutrientValue.from(fiber),
                        sugars = NutrientValue.from(sugar),
                    ),
            )
        }
    }
}

/**
 * Exception thrown when food parsing fails.
 */
class FoodParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)
