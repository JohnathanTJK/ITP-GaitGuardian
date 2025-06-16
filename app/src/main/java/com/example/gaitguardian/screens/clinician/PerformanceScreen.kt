package com.example.gaitguardian.screens.clinician

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gaitguardian.ui.theme.bgColor

@Composable
fun PerformanceScreen() {
    var selectedTask by remember { mutableStateOf("All Tasks") }

    val subtasks = listOf(
        "All Tasks",
        "Sit-to-Stand",
        "Walk from Chair",
        "Turn First",
        "Walk to Chair",
        "Turn Second",
        "Stand-to-Sit"
    )

    Column(
        modifier = Modifier
            .background(bgColor)
            .fillMaxSize()
            .padding(16.dp)

    ) {

        TaskDropdownFilter(
            selectedTask = selectedTask,
            onTaskSelected = { selectedTask = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        PerformanceChart(
            selectedTask = selectedTask,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PerformanceChart(
    selectedTask: String,
    modifier: Modifier = Modifier
) {
    //TODO: Update the graph to display the corresponding values
    Box(
        modifier = modifier.background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TEMPORARY PLACEHOLDER")
            Text("Chart for: $selectedTask")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDropdownFilter(
    selectedTask: String,
    onTaskSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val allTasks = listOf(
        "All Tasks",
        "Sit-to-Stand",
        "Walk from Chair",
        "Turn First",
        "Walk to Chair",
        "Turn Second",
        "Stand-to-Sit"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTask,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filter by Task", color = Color.DarkGray) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.DarkGray,
                unfocusedLabelColor = Color.DarkGray,
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allTasks.forEach { task ->
                DropdownMenuItem(
                    text = { Text(task) },
                    onClick = {
                        onTaskSelected(task)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun PerformanceScreenPreview() {
    PerformanceScreen()
}