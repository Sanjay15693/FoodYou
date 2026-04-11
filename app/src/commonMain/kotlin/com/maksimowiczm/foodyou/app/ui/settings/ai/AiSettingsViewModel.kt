package com.maksimowiczm.foodyou.app.ui.settings.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.log.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AiSettingsViewModel(
    private val aiPreferencesRepository: UserPreferencesRepository<AiPreferences>,
    private val geminiApiClient: GeminiApiClient,
    private val logger: Logger,
) : ViewModel() {
    val aiPreferences =
        aiPreferencesRepository
            .observe()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AiPreferences(geminiApiKey = null),
            )

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _modelsError = MutableStateFlow<String?>(null)
    val modelsError: StateFlow<String?> = _modelsError.asStateFlow()

    fun loadModels() {
        val apiKey = aiPreferences.value.geminiApiKey
        if (apiKey.isNullOrBlank()) {
            _availableModels.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoadingModels.value = true
            _modelsError.value = null
            try {
                val models = geminiApiClient.listModels(apiKey)
                _availableModels.value = models
                    .filter { it.supportedGenerationMethods.contains("generateContent") }
                    .map { it.name.removePrefix("models/") }
                logger.d("AiSettingsViewModel") { "Loaded ${_availableModels.value.size} models" }
            } catch (e: Exception) {
                logger.e("AiSettingsViewModel", e) { "Failed to load models: ${e.message}" }
                _modelsError.value = e.message ?: "Failed to load models"
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            aiPreferencesRepository.update { copy(geminiApiKey = apiKey.takeIf { it.isNotBlank() }) }
        }
    }

    fun updateModel(model: String) {
        viewModelScope.launch {
            aiPreferencesRepository.update { copy(geminiModel = model.takeIf { it.isNotBlank() }) }
        }
    }
}

@Composable
internal fun rememberAiSettingsViewModel(
    aiPreferencesRepository: UserPreferencesRepository<AiPreferences>,
    geminiApiClient: GeminiApiClient,
    logger: Logger,
): AiSettingsViewModel {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        AiSettingsViewModel(aiPreferencesRepository, geminiApiClient, logger)
    }
    return viewModel
}
