package com.maksimowiczm.foodyou.app.ui.food.diary.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.ai.domain.entity.AiPreferences
import com.maksimowiczm.foodyou.ai.domain.entity.ParsedFoodEntry
import com.maksimowiczm.foodyou.ai.domain.service.FoodParsingService
import com.maksimowiczm.foodyou.common.log.Logger
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun AiFoodInputDialog(
    onDismiss: () -> Unit,
    onFoodSelected: (ParsedFoodEntry) -> Unit,
    modifier: Modifier = Modifier,
    foodParsingService: FoodParsingService = koinInject(),
    aiPreferencesRepository: UserPreferencesRepository<AiPreferences> = koinInject(),
    logger: Logger = koinInject(),
) {
    val viewModel = remember {
        AiFoodInputViewModel(foodParsingService, aiPreferencesRepository, logger)
    }
    val uiState by viewModel.uiState.collectAsState()

    AiFoodInputDialog(
        uiState = uiState,
        onDismiss = onDismiss,
        onParse = viewModel::parseFoodDescription,
        onFoodSelected = onFoodSelected,
        onRetry = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
private fun AiFoodInputDialog(
    uiState: AiFoodInputUiState,
    onDismiss: () -> Unit,
    onParse: (String) -> Unit,
    onFoodSelected: (ParsedFoodEntry) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        title = { Text(stringResource(Res.string.headline_ai_assistant)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (uiState) {
                    is AiFoodInputUiState.Input -> {
                        Text(
                            text = stringResource(Res.string.ai_food_input_description),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text(stringResource(Res.string.label_food_description)) },
                            placeholder = {
                                Text(stringResource(Res.string.placeholder_food_description))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                        )
                    }
                    is AiFoodInputUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(Res.string.ai_parsing_food),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    is AiFoodInputUiState.Success -> {
                        Text(
                            text = stringResource(Res.string.ai_parsed_results),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uiState.parsedEntries) { entry ->
                                ParsedFoodCard(
                                    entry = entry,
                                    onClick = { onFoodSelected(entry) },
                                )
                            }
                        }
                    }
                    is AiFoodInputUiState.Error -> {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (uiState) {
                is AiFoodInputUiState.Input -> {
                    TextButton(
                        onClick = { onParse(inputText) },
                        enabled = inputText.isNotBlank(),
                    ) {
                        Text(stringResource(Res.string.action_parse))
                    }
                }
                is AiFoodInputUiState.Error -> {
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.action_retry)) }
                }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
        modifier = modifier,
    )
}

@Composable
private fun ParsedFoodCard(entry: ParsedFoodEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${entry.quantity} (${entry.weightGrams}g)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NutrientText("${entry.nutritionFacts.energy.value?.toInt() ?: 0} kcal")
                NutrientText("P: ${entry.nutritionFacts.proteins.value?.toInt() ?: 0}g")
                NutrientText("C: ${entry.nutritionFacts.carbohydrates.value?.toInt() ?: 0}g")
                NutrientText("F: ${entry.nutritionFacts.fats.value?.toInt() ?: 0}g")
            }
        }
    }
}

@Composable
private fun NutrientText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
