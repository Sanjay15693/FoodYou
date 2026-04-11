# Gemini Model Selection Feature - Design Spec

**Date:** 2026-04-11  
**Status:** Approved

## Overview

Add dynamic model selection to the AI food parsing feature. Users will be able to choose which Gemini model to use from a dropdown populated by fetching available models from the Gemini API.

## Current State

- `GeminiApiClient.kt:28` hardcodes model as `gemini-2.5-flash-preview-09-2025`
- `AiPreferences` only stores `geminiApiKey`
- `DataStoreAiPreferencesRepository` only persists API key
- `AiSettingsScreen` only has API key input field

## Changes Required

### 1. Update `GeminiApiModels.kt`

Add response model for listing models:

```kotlin
@Serializable
data class ListModelsResponse(
    val models: List<ModelInfo>
)

@Serializable  
data class ModelInfo(
    val name: String,  // Format: "models/gemini-2.5-flash"
    val version: String,
    val displayName: String? = null,
    val description: String? = null,
)
```

### 2. Update `GeminiApiClient.kt`

Add `listModels` function:

```kotlin
suspend fun listModels(apiKey: String): List<ModelInfo> {
    val response = httpClient.get("$BASE_URL") {
        header(API_KEY_HEADER, apiKey)
    }
    // Parse and return models
}
```

### 3. Update `AiPreferences.kt`

Add model field:

```kotlin
data class AiPreferences(
    val geminiApiKey: String?,
    val geminiModel: String? = null  // null = use server default
) : UserPreferences {
    val isGeminiEnabled: Boolean
        get() = !geminiApiKey.isNullOrBlank()
}
```

### 4. Update `DataStoreAiPreferencesRepository.kt`

Add model preference key and handle in repository.

### 5. Update `AiSettingsViewModel.kt`

Add model list state and methods:

```kotlin
class AiSettingsViewModel(...) {
    val availableModels = MutableStateFlow<List<String>>(emptyList())
    val isLoadingModels = MutableStateFlow(false)
    
    fun loadModels() // Fetch from API
    fun updateModel(model: String)
    fun updateApiKey(apiKey: String)
}
```

### 6. Update `AiSettingsScreen.kt`

Add model selector dropdown below API key field:

- Show loading indicator while fetching models
- Dropdown with available models
- Display selected model
- Handle errors with retry button

### 7. Update `FoodParsingService.kt`

Pass model to `GeminiApiClient.generateContent()`:

```kotlin
val response = geminiApiClient.generateContent(
    prompt,
    apiKey,
    model ?: DEFAULT_MODEL
)
```

## API Integration

**Endpoint:** `GET https://generativelanguage.googleapis.com/v1beta/models`

**Headers:** `x-goog-api-key: {apiKey}`

**Response parsing:** Extract `name` field from each model, strip "models/" prefix for display value.

## UI Design

### Settings Screen Layout
1. Description text (existing)
2. API Key field (existing)
3. Model dropdown (new)
   - Shows "Loading..." while fetching
   - Shows "Select model" placeholder when empty
   - Lists all available models

### Error Handling
- **No API key:** Show "Enter API key to see available models"
- **Fetch failed:** Show error message with "Retry" button
- **Empty list:** Show "No models available"

## Testing Considerations

1. Mock `listModels` response in unit tests
2. Test UI with various API key states
3. Test error handling for network failures
4. Test default value behavior when model preference is null
