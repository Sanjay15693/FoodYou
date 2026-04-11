package com.maksimowiczm.foodyou.app.ui.settings.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.common.log.Logger

@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    aiPreferencesRepository: UserPreferencesRepository<AiPreferences> = koinInject(),
    geminiApiClient: GeminiApiClient = koinInject(),
    logger: Logger = koinInject(),
) {
    val viewModel = rememberAiSettingsViewModel(aiPreferencesRepository, geminiApiClient, logger)
    val preferences by viewModel.aiPreferences.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val modelsError by viewModel.modelsError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!preferences.geminiApiKey.isNullOrBlank()) {
            viewModel.loadModels()
        }
    }

    AiSettingsScreen(
        onBack = onBack,
        apiKey = preferences.geminiApiKey ?: "",
        onApiKeyChange = viewModel::updateApiKey,
        selectedModel = preferences.geminiModel,
        availableModels = availableModels,
        isLoadingModels = isLoadingModels,
        modelsError = modelsError,
        onModelChange = viewModel::updateModel,
        onRetryLoadModels = viewModel::loadModels,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsScreen(
    onBack: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String?,
    availableModels: List<String>,
    isLoadingModels: Boolean,
    modelsError: String?,
    onModelChange: (String) -> Unit,
    onRetryLoadModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var apiKeyVisible by remember { mutableStateOf(false) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val currentApiKey = apiKeyInput.ifBlank { apiKey }
    val shouldLoadModels = currentApiKey.isNotBlank() && availableModels.isEmpty() && !isLoadingModels

    LaunchedEffect(currentApiKey) {
        if (shouldLoadModels) {
            onRetryLoadModels()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.headline_ai_assistant)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.ai_settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    onApiKeyChange(it)
                },
                label = { Text(stringResource(Res.string.label_gemini_api_key)) },
                placeholder = { Text(stringResource(Res.string.placeholder_gemini_api_key)) },
                supportingText = { Text(stringResource(Res.string.supporting_gemini_api_key)) },
                visualTransformation =
                    if (apiKeyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector =
                                if (apiKeyVisible) Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff,
                            contentDescription =
                                if (apiKeyVisible) stringResource(Res.string.hide_api_key)
                                else stringResource(Res.string.show_api_key),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = stringResource(Res.string.ai_settings_how_to_get_key),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            Text(
                text = stringResource(Res.string.label_model_selection),
                style = MaterialTheme.typography.titleMedium,
            )

            if (currentApiKey.isBlank()) {
                Text(
                    text = stringResource(Res.string.model_selection_requires_api_key),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isLoadingModels) {
                Text(
                    text = stringResource(Res.string.loading_models),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (modelsError != null) {
                Text(
                    text = modelsError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onRetryLoadModels) {
                    Text(stringResource(Res.string.retry))
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedModel ?: stringResource(Res.string.model_default),
                        readOnly = true,
                        label = { Text(stringResource(Res.string.label_gemini_model)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                    ) {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    onModelChange(model)
                                    modelDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
