package com.maksimowiczm.foodyou.app.ui.settings.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AiSettingsViewModel(
    private val aiPreferencesRepository: UserPreferencesRepository<AiPreferences>,
) : ViewModel() {
    val aiPreferences =
        aiPreferencesRepository
            .observe()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AiPreferences(geminiApiKey = null),
            )

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            aiPreferencesRepository.update { copy(geminiApiKey = apiKey.takeIf { it.isNotBlank() }) }
        }
    }
}

@Composable
internal fun rememberAiSettingsViewModel(
    aiPreferencesRepository: UserPreferencesRepository<AiPreferences>,
): AiSettingsViewModel {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel { AiSettingsViewModel(aiPreferencesRepository) }
    return viewModel
}
