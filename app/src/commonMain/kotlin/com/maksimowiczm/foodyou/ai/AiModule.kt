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
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.log.Logger
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
        single<UserPreferencesRepository<AiPreferences>> {
            DataStoreAiPreferencesRepository(get())
        }
        
        // Register HttpClient with JSON serialization
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
            }
        }
        
        // Register Logger
        single<Logger> { 
            object : Logger {
                override fun d(tag: String, throwable: Throwable?, message: () -> String) {
                    println("[$tag] DEBUG: ${message()}")
                    throwable?.printStackTrace()
                }
                override fun w(tag: String, throwable: Throwable?, message: () -> String) {
                    println("[$tag] WARN: ${message()}")
                    throwable?.printStackTrace()
                }
                override fun e(tag: String, throwable: Throwable?, message: () -> String) {
                    println("[$tag] ERROR: ${message()}")
                    throwable?.printStackTrace()
                }
                override fun i(tag: String, throwable: Throwable?, message: () -> String) {
                    println("[$tag] INFO: ${message()}")
                    throwable?.printStackTrace()
                }
            }
        }
        
        // Register GeminiApiClient
        factory { 
            GeminiApiClient(
                httpClient = get(),
                logger = get()
            ) 
        }
        
        // Register FoodParsingService
        factory { 
            FoodParsingService(
                geminiApiClient = get(),
                logger = get()
            ) 
        }
    })
}
