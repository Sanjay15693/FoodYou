package com.maksimowiczm.foodyou.app.ui.food.diary.ai

import com.maksimowiczm.foodyou.ai.domain.entity.ParsedFoodEntry
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodHistory
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.usecase.CreateProductUseCase
import com.maksimowiczm.foodyou.food.domain.usecase.CreateProductError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock.System as SystemClock
import com.maksimowiczm.foodyou.common.result.Result
import com.maksimowiczm.foodyou.common.result.Err
import com.maksimowiczm.foodyou.common.result.Ok

/**
 * Creates a product from AI-parsed food entry and returns the product ID and suggested measurement.
 *
 * @return A [com.maksimowiczm.foodyou.common.result.Result] containing either a [Pair] of [FoodId.Product] and [Measurement] on success,
 * or a [CreateProductError] on failure.
 */
suspend fun createProductFromParsedFood(
    parsedEntry: ParsedFoodEntry,
    createProductUseCase: CreateProductUseCase,
): Result<Pair<FoodId.Product, Measurement>, CreateProductError> {
    val servingWeight =
        if (parsedEntry.itemCount != null && parsedEntry.itemCount > 0) {
            parsedEntry.weightGrams / parsedEntry.itemCount
        } else {
            null
        }

    val result = createProductUseCase.create(
        name = parsedEntry.foodName,
        brand = null,
        barcode = null,
        note = "Created by AI Assistant from: \"${parsedEntry.quantity}\"",
        isLiquid = parsedEntry.isLiquid,
        packageWeight = null,
        servingWeight = servingWeight,
        source = FoodSource(type = FoodSource.Type.User, url = null),
        nutritionFacts = parsedEntry.nutritionFacts,
        history = FoodHistory.Created(Instant.DISTANT_PAST),
    )

    return when (result) {
        is Result.Success -> {
            val measurement =
                if (parsedEntry.isLiquid) {
                    Measurement.Milliliter(parsedEntry.weightGrams)
                } else {
                    Measurement.Gram(parsedEntry.weightGrams)
                }
            Ok(Pair(result.data, measurement))
        }
        is Result.Error -> Err(result.error) // Extract the error from the result
    }
}
