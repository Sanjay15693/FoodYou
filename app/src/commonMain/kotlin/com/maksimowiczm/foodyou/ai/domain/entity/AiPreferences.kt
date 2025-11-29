package com.maksimowiczm.foodyou.ai.domain.entity

import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferences

/**
 * User preferences for AI-assisted features.
 *
 * @param geminiApiKey The Google AI (Gemini) API key for making requests to the Gemini API.
 */
data class AiPreferences(val geminiApiKey: String?) : UserPreferences {
    val isGeminiEnabled: Boolean
        get() = !geminiApiKey.isNullOrBlank()
}
