package com.maksimowiczm.foodyou.app.ui.food.diary.ai

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.ai.domain.entity.ParsedFoodEntry
import com.maksimowiczm.foodyou.ai.domain.service.FoodParsingException
import com.maksimowiczm.foodyou.ai.domain.service.FoodParsingService
import com.maksimowiczm.foodyou.common.log.Logger
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.domain.userpreferences.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AiFoodInputViewModel(
    private val foodParsingService: FoodParsingService,
    private val aiPreferencesRepository: UserPreferencesRepository<AiPreferences>,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AiFoodInputUiState>(AiFoodInputUiState.Input)
    val uiState: StateFlow<AiFoodInputUiState> = _uiState.asStateFlow()

    fun parseFoodDescription(description: String) {
        if (description.isBlank()) {
            _uiState.value = AiFoodInputUiState.Error("Please enter a food description")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiFoodInputUiState.Loading

            try {
                // Check if API key is configured
                val preferences = aiPreferencesRepository.get()
                if (!preferences.isGeminiEnabled) {
                    _uiState.value =
                        AiFoodInputUiState.Error(
                            "Google AI API key not configured. Please configure it in Settings > AI Assistant."
                        )
                    return@launch
                }

                val parsedEntries = foodParsingService.parseFoodDescription(description, preferences.geminiApiKey!!)

                if (parsedEntries.isEmpty()) {
                    _uiState.value =
                        AiFoodInputUiState.Error(
                            "Could not parse food description. Please try rephrasing or be more specific."
                        )
                } else {
                    _uiState.value = AiFoodInputUiState.Success(parsedEntries)
                }
            } catch (e: FoodParsingException) {
                logger.e("AiFoodInputViewModel", e) { "Failed to parse food: ${e.message}" }
                _uiState.value = AiFoodInputUiState.Error(e.message ?: "Failed to parse food")
            } catch (e: Exception) {
                logger.e("AiFoodInputViewModel", e) { "Unexpected error: ${e.message}" }
                _uiState.value = AiFoodInputUiState.Error("An unexpected error occurred")
            }
        }
    }

    fun reset() {
        _uiState.value = AiFoodInputUiState.Input
    }
}

@Immutable
internal sealed interface AiFoodInputUiState {
    data object Input : AiFoodInputUiState

    data object Loading : AiFoodInputUiState

    data class Success(val parsedEntries: List<ParsedFoodEntry>) : AiFoodInputUiState

    data class Error(val message: String) : AiFoodInputUiState
}
