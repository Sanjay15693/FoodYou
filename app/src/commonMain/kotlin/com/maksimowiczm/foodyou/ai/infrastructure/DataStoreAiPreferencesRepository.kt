package com.maksimowiczm.foodyou.ai.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.common.infrastructure.datastore.AbstractDataStoreUserPreferencesRepository
import com.maksimowiczm.foodyou.common.infrastructure.datastore.set

internal class DataStoreAiPreferencesRepository(dataStore: DataStore<Preferences>) :
    AbstractDataStoreUserPreferencesRepository<AiPreferences>(dataStore) {
    override fun Preferences.toUserPreferences(): AiPreferences =
        AiPreferences(geminiApiKey = this[AiPreferencesKeys.GeminiApiKey])

    override fun MutablePreferences.applyUserPreferences(updated: AiPreferences) {
        this[AiPreferencesKeys.GeminiApiKey] = updated.geminiApiKey
    }
}

private object AiPreferencesKeys {
    val GeminiApiKey = stringPreferencesKey("ai:gemini_api_key")
}
