package com.maksimowiczm.foodyou.ai.domain.entity

import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts

/**
 * Represents a food entry parsed from natural language input by AI.
 *
 * @param foodName The name of the food item.
 * @param quantity The quantity/serving size (e.g., "2 slices", "1 cup").
 * @param weightGrams The estimated weight in grams.
 * @param nutritionFacts The nutrition facts per 100g.
 */
data class ParsedFoodEntry(
    val foodName: String,
    val quantity: String,
    val weightGrams: Double,
    val nutritionFacts: NutritionFacts,
)
