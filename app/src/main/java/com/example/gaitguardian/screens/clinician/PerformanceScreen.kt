package com.example.gaitguardian.screens.clinician

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gaitguardian.ui.theme.bgColor
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
fun PerformanceScreen() {
    var selectedTask by remember { mutableStateOf("All Tasks") }

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
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(selectedTask) {
        modelProducer.runTransaction {
            lineSeries {
                when (selectedTask) {
                    "All Tasks" -> series(listOf(1, 2, 3, 4, 5), listOf(13, 8, 7, 12, 10))
                    "Sit-to-Stand" -> series(listOf(1, 2, 3), listOf(2, 2, 1))
                    "Walk from Chair" -> series(listOf(1, 2, 3), listOf(4, 5, 4))
                    "Turn First" -> series(listOf(1, 2, 3), listOf(3, 2, 4))
                    "Walk to Chair" -> series(listOf(1, 2, 3), listOf(4, 4, 5))
                    "Turn Second" -> series(listOf(1, 2, 3), listOf(2, 3, 3))
                    "Stand-to-Sit" -> series(listOf(1, 2, 3), listOf(1, 2, 2))
                    else -> series(emptyList(), emptyList())
                }
            }
        }
    }
    JetpackComposeBasicLineChart(modelProducer, selectedTask, modifier)


}

@Composable
private fun JetpackComposeBasicLineChart(
    modelProducer: CartesianChartModelProducer,
    selectedTask: String,
    modifier: Modifier = Modifier,
) {
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Color.Blue)),
        pointProvider = LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.point(
                shapeComponent(fill(Color.Blue), CorneredShape.Pill)
            )
        )
    )
    // Set colour text value for axis
    val axisValueColor = 0xFF000000.toInt() // Black (ARGB format)
    val valueComponent = TextComponent(
        color = axisValueColor, // since they only allow Int
    )

    val xAxis = HorizontalAxis.rememberBottom(
        title = selectedTask,
        titleComponent = TextComponent(),
        label = valueComponent
    )

    val yAxis = VerticalAxis.rememberStart(
        title = "Time Taken",
        titleComponent = TextComponent(),
        label = valueComponent
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(listOf(line))
            ),
            startAxis = yAxis,
            bottomAxis = xAxis,
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(300.dp), // Set a minimum height

    )
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
                    }
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

