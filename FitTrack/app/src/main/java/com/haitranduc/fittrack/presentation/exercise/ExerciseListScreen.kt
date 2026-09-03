package com.haitranduc.fittrack.presentation.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme

data class ExerciseUiItem(
    val id: String,
    val name: String,
    val bodyPartKey: BodyPartFilterKey,
    val bodyPart: String,
    val target: String,
    val equipmentKey: EquipmentFilterKey,
    val equipment: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBodyPart by remember { mutableStateOf(BodyPartFilterKey.ALL) }
    var selectedEquipment by remember { mutableStateOf(EquipmentFilterKey.ALL) }

    val exerciseUiItems = ExerciseMockData.exercises.map { exercise ->
        ExerciseUiItem(
            id = exercise.id,
            name = stringResource(exercise.nameRes),
            bodyPartKey = exercise.bodyPartKey,
            bodyPart = stringResource(exercise.bodyPartRes),
            target = stringResource(exercise.targetRes),
            equipmentKey = exercise.equipmentKey,
            equipment = stringResource(exercise.equipmentRes)
        )
    }

    val filteredExercises = remember(searchQuery, selectedBodyPart, selectedEquipment, exerciseUiItems) {
        exerciseUiItems.filter { exercise ->
            val matchesQuery = searchQuery.isBlank() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.target.contains(searchQuery, ignoreCase = true) ||
                    exercise.equipment.contains(searchQuery, ignoreCase = true)
            val matchesBodyPart = selectedBodyPart == BodyPartFilterKey.ALL || exercise.bodyPartKey == selectedBodyPart
            val matchesEquipment = selectedEquipment == EquipmentFilterKey.ALL || exercise.equipmentKey == selectedEquipment
            matchesQuery && matchesBodyPart && matchesEquipment
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_exercises),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search input with continuous label for accessibility
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(text = stringResource(R.string.label_search_exercises)) },
                placeholder = { Text(text = stringResource(R.string.hint_search_exercises)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body Part Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_body_part_filter),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BodyPartFilterKey.values().forEach { key ->
                    FilterChip(
                        selected = selectedBodyPart == key,
                        onClick = { selectedBodyPart = key },
                        label = { Text(text = stringResource(key.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Equipment Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_equipment_filter),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EquipmentFilterKey.values().forEach { key ->
                    FilterChip(
                        selected = selectedEquipment == key,
                        onClick = { selectedEquipment = key },
                        label = { Text(text = stringResource(key.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredExercises.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.empty_exercises),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExercises, key = { it.id }) { exercise ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseClick(exercise.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = exercise.bodyPart,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = exercise.equipment,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = exercise.target,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Exercise List Screen")
@Composable
private fun ExerciseListScreenPreview() {
    FitTrackTheme {
        ExerciseListScreen(onExerciseClick = {})
    }
}
