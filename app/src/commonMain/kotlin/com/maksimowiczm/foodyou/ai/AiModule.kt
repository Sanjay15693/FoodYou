package com.maksimowiczm.foodyou.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.ai.domain.service.FoodParsingService
import com.maksimowiczm.foodyou.ai.infrastructure.DataStoreAiPreferencesRepository
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.common.infrastructure.koin.userPreferencesRepositoryOf
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.io.IOException

// Create a DataStore instance using preferencesDataStore delegate
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_preferences"
)

fun Module.aiModule() {
    // Include the data store module for preferences
    includes(module {
        // Register the DataStore for preferences
        single {
            androidContext().dataStore
        }
        
        // Register the AiPreferences repository
        userPreferencesRepositoryOf<AiPreferences, DataStore<Preferences>>(
            constructor = { dataStore -> DataStoreAiPreferencesRepository(dataStore) }
        )
        
        // Register other dependencies
        factoryOf(::GeminiApiClient)
        factoryOf(::FoodParsingService)
    })
}
