# Gemini Model Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add dynamic model selection to AI food parsing, fetching available models from Gemini API and allowing user to select from a dropdown.

**Architecture:** Add `listModels` endpoint to `GeminiApiClient`, extend `AiPreferences` with model field, update settings UI with dropdown, wire model through to parsing service.

**Tech Stack:** Kotlin, Ktor HTTP client, DataStore preferences, Jetpack Compose

---

## File Structure

| File | Action |
|------|--------|
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiModels.kt` | Modify - add list models response models |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiClient.kt` | Modify - add listModels() function |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/entity/AiPreferences.kt` | Modify - add geminiModel field |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/DataStoreAiPreferencesRepository.kt` | Modify - persist model preference |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsViewModel.kt` | Modify - add model list state and methods |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsScreen.kt` | Modify - add model dropdown UI |
| `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/service/FoodParsingService.kt` | Modify - pass model to API client |

---

## Task 1: Add List Models Response Models

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiModels.kt`

- [ ] **Step 1: Read current GeminiApiModels.kt**

```kotlin
// Current content should be a minimal file with Request/Response models
```

- [ ] **Step 2: Add list models response models after existing content**

```kotlin
@Serializable
data class ListModelsResponse(
    val models: List<ModelInfo> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class ModelInfo(
    val name: String,
    val version: String,
    val displayName: String? = null,
    val description: String? = null,
    val inputTokenLimit: Int? = null,
    val outputTokenLimit: Int? = null,
    val supportedGenerationMethods: List<String> = emptyList(),
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiModels.kt
git commit -m "feat(ai): add list models response models"
```

---

## Task 2: Add listModels() to GeminiApiClient

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiClient.kt:59`

- [ ] **Step 1: Read current GeminiApiClient.kt**

Focus on the existing structure and imports.

- [ ] **Step 2: Add import for GET request**

Add after line 7:
```kotlin
import io.ktor.client.request.get
```

- [ ] **Step 3: Add listModels function after generateContent (after line 96)**

```kotlin
/**
 * Lists available models from the Gemini API.
 *
 * @param apiKey The Google AI API key.
 * @return List of available model names (with "models/" prefix).
 * @throws GeminiApiException if the API request fails.
 */
suspend fun listModels(apiKey: String): List<ModelInfo> {
    logger.d(TAG) { "Listing available models" }

    return try {
        val response = httpClient.get(BASE_URL) {
            header(API_KEY_HEADER, apiKey)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            logger.e(TAG) { "Gemini API error listing models: $errorBody" }
            throw GeminiApiException(
                "Failed to list models: ${response.status.value}",
                response.status.value
            )
        }

        val listResponse = response.body<ListModelsResponse>()
        logger.d(TAG) { "Found ${listResponse.models.size} available models" }
        listResponse.models
    } catch (e: GeminiApiException) {
        throw e
    } catch (e: Exception) {
        logger.e(TAG, e) { "Failed to list models: ${e.message}" }
        throw GeminiApiException("Failed to list models: ${e.message}", null, e)
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/gemini/GeminiApiClient.kt
git commit -m "feat(ai): add listModels endpoint to GeminiApiClient"
```

---

## Task 3: Add geminiModel to AiPreferences

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/entity/AiPreferences.kt`

- [ ] **Step 1: Read current AiPreferences.kt**

- [ ] **Step 2: Update the data class**

Replace lines 10-12 with:
```kotlin
data class AiPreferences(
    val geminiApiKey: String?,
    val geminiModel: String? = null,
) : UserPreferences {
    val isGeminiEnabled: Boolean
        get() = !geminiApiKey.isNullOrBlank()
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/entity/AiPreferences.kt
git commit -m "feat(ai): add geminiModel field to AiPreferences"
```

---

## Task 4: Update DataStoreAiPreferencesRepository

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/DataStoreAiPreferencesRepository.kt`

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Update toUserPreferences to read model**

Replace line 14 with:
```kotlin
        AiPreferences(
            geminiApiKey = this[AiPreferencesKeys.GeminiApiKey],
            geminiModel = this[AiPreferencesKeys.GeminiModel],
        )
```

- [ ] **Step 3: Update applyUserPreferences to write model**

Replace lines 16-18 with:
```kotlin
    override fun MutablePreferences.applyUserPreferences(updated: AiPreferences) {
        this[AiPreferencesKeys.GeminiApiKey] = updated.geminiApiKey
        this[AiPreferencesKeys.GeminiModel] = updated.geminiModel
    }
```

- [ ] **Step 4: Add new preference key to AiPreferencesKeys**

Add to the end of AiPreferencesKeys object:
```kotlin
    val GeminiModel = stringPreferencesKey("ai:gemini_model")
```

- [ ] **Step 5: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/infrastructure/DataStoreAiPreferencesRepository.kt
git commit -m "feat(ai): persist gemini model preference in DataStore"
```

---

## Task 5: Update AiSettingsViewModel

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsViewModel.kt`

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Add imports**

Add after existing imports:
```kotlin
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.common.log.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

- [ ] **Step 3: Update class constructor and add state**

Replace lines 14-30 with:
```kotlin
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
```

- [ ] **Step 4: Update rememberAiSettingsViewModel**

Replace lines 33-39 with:
```kotlin
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
```

- [ ] **Step 5: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsViewModel.kt
git commit -m "feat(ai): add model loading and selection to AiSettingsViewModel"
```

---

## Task 6: Update AiSettingsScreen

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsScreen.kt`

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Add imports**

Add after existing imports:
```kotlin
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.ai.infrastructure.gemini.GeminiApiClient
import com.maksimowiczm.foodyou.common.log.Logger
import org.koin.compose.koinInject
```

- [ ] **Step 3: Update AiSettingsScreen function signature and content**

Replace the private `AiSettingsScreen` composable (lines 53-125) with:
```kotlin
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
                        onValueChange = {},
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
```

- [ ] **Step 4: Update public AiSettingsScreen composable (lines 37-51)**

Replace with:
```kotlin
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    aiPreferencesRepository: UserPreferencesRepository<AiPreferences> = koinInject(),
    geminiApiClient: GeminiApiClient = koinInject(),
    logger: Logger = koinInject(),
) {
    val viewModel = rememberAiSettingsViewModel(aiPreferencesRepository, geminiApiClient, logger)
    val preferences by viewModel.aiPreferences.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val modelsError by viewModel.modelsError.collectAsState()

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
```

- [ ] **Step 5: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/settings/ai/AiSettingsScreen.kt
git commit -m "feat(ai): add model selection dropdown to AiSettingsScreen"
```

---

## Task 7: Update FoodParsingService to Use Selected Model

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/service/FoodParsingService.kt`

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Update parseFoodDescription signature and call**

Replace lines 34-44 with:
```kotlin
    suspend fun parseFoodDescription(
        description: String,
        apiKey: String,
        model: String? = null,
    ): List<ParsedFoodEntry> {
        logger.d(TAG) { "Parsing food description: $description" }

        if (apiKey.isBlank()) {
            throw FoodParsingException("Google AI API key is empty")
        }

        val prompt = buildPrompt(description)

        return try {
            val response = geminiApiClient.generateContent(prompt, apiKey, model)
            parseResponse(response)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/domain/service/FoodParsingService.kt
git commit -m "feat(ai): pass model parameter to GeminiApiClient"
```

---

## Task 8: Update AiFoodInputViewModel to Pass Model

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/food/diary/ai/AiFoodInputViewModel.kt`

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Update parseFoodDescription call (line 46)**

Replace line 46 with:
```kotlin
                val parsedEntries = foodParsingService.parseFoodDescription(
                    description,
                    preferences.geminiApiKey!!,
                    preferences.geminiModel,
                )
```

- [ ] **Step 3: Commit**

```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/food/diary/ai/AiFoodInputViewModel.kt
git commit -m "feat(ai): pass selected model to food parsing service"
```

---

## Task 9: Add String Resources

**Files:**
- Modify: `shared/resources/src/commonMain/resources/Res.values/strings.xml` (or similar location)

- [ ] **Step 1: Find and read string resources file**

```bash
find . -name "strings.xml" | head -5
```

- [ ] **Step 2: Add new string resources**

Add these entries:
```xml
<string name="label_model_selection">Model Selection</string>
<string name="label_gemini_model">Gemini Model</string>
<string name="model_selection_requires_api_key">Enter an API key to see available models</string>
<string name="loading_models">Loading available models...</string>
<string name="retry">Retry</string>
<string name="model_default">Default</string>
```

- [ ] **Step 3: Commit**

```bash
git add shared/resources/src/commonMain/resources/Res.values/strings.xml
git commit -m "feat(ai): add string resources for model selection UI"
```

---

## Task 10: Wire Up GeminiApiClient in AiModule

**Files:**
- Modify: `app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/AiModule.kt`

- [ ] **Step 1: Read current AiModule.kt**

- [ ] **Step 2: Verify GeminiApiClient is already registered**

The factory for `GeminiApiClient` should already exist at lines 88-94. Verify it's there.

- [ ] **Step 3: Commit (if changes made)**

If no changes needed, skip this step. Otherwise:
```bash
git add app/src/commonMain/kotlin/com/maksimowiczm/foodyou/ai/AiModule.kt
git commit -m "chore(ai): verify GeminiApiClient factory registration"
```

---

## Self-Review Checklist

- [ ] All spec requirements covered by tasks
- [ ] No placeholder code (TBD, TODO)
- [ ] Type consistency across tasks (ModelInfo, listModels, etc.)
- [ ] Each task is self-contained with working code
- [ ] Commit messages follow conventional format
