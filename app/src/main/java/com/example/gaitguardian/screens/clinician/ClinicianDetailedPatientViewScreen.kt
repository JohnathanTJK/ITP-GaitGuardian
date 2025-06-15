package com.example.gaitguardian.screens.clinician

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.buttonBackgroundColor
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
fun ClinicianDetailedPatientViewScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val patient = Patient(2,"Benny", 18)

    var clinicianComments by remember { mutableStateOf("") }
    var isReviewed by remember { mutableStateOf(false) }
    var statusUpdateMsg by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(scrollState),
//        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                text = "Currently reviewing",
                fontSize = 14.sp,
                color = Color(0xFF718096),
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = patient.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748)
                )
                Text(
                    text = "• Age ${patient.age}",
                    fontSize = 14.sp,
                    color = Color(0xFF718096)
                )
            }
            Text(
                "Video Details: 04/15/2023 1:30PM", fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            Text(
                "TUG TEST #5",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                ){
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ){
                        Text("Overall Performance Graph", fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    JetpackComposeBasicLineChart()
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { navController.navigate("performance_screen") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBackgroundColor
                            )
                        ) {
                            Text("View Detailed Graph")
                        }
                    }
                }
            Spacer(modifier = Modifier.height(16.dp))
            TUGsubTasksList()
            Spacer(modifier = Modifier.height(16.dp))
            Column{
                Text("Clinician Notes:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = clinicianComments,
                    onValueChange = { clinicianComments = it },
                    modifier = modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.DarkGray,
                        focusedContainerColor =  Color(0xFFF5F5F5),
                    ),
                    label = { Text("Enter observations here...", color = Color.Black) }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = isReviewed,
                        onCheckedChange = { isReviewed = it }
                    )
                    Text(text = "Mark as Reviewed")
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            //TODO: Update Database
                            statusUpdateMsg = "Status Updated Successfully."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBackgroundColor,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        ),
                        // If no comments/ not reviewed, don't allow update.
                        enabled = !clinicianComments.isEmpty() || isReviewed,
                    ) {
                        Text("Update Status", color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (statusUpdateMsg.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDFF0D8), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusUpdateMsg,
                            color = Color(0xFF3C763D),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

    }
}


// Taken from Vico's Sample Code https://github.com/patrykandpatrick/vico/blob/master/sample/compose/src/main/kotlin/com/patrykandpatrick/vico/sample/compose/BasicLineChart.kt
// Added axis labels + circular points on each datapoint
@Composable
private fun JetpackComposeBasicLineChart(
    modelProducer: CartesianChartModelProducer,
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

    val xAxis = HorizontalAxis.rememberBottom(
        title = "TUG Test Video Assessment",
        titleComponent = TextComponent()
    )

    val yAxis = VerticalAxis.rememberStart(
        title = "Time Taken",
        titleComponent = TextComponent()
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
        modifier = modifier,
    )
}

val tugSubTasks = listOf(
    "Sit to Stand",
    "Straight walking from Chair", "Turning", "Straight walking to chair", "Stand to Sit"
)

@Composable
fun TUGsubTasksList(modifier: Modifier = Modifier) {
    Column()
    {
        tugSubTasks.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    )
                    {
                        Text(task)
                        Card(
                            modifier = Modifier
                                .height(24.dp)
                                .width(40.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.DarkGray
                            ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ){
                                Text("15s", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF718096))
        }
    }
}

@Composable
fun JetpackComposeBasicLineChart(modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    listOf(1, 2, 3, 4, 5, 6),  // TUG Tests # / X values
                    listOf(13, 8, 7, 12, 0, 1)       // Time Taken / Y Values
                )
            }
        }
    }
    JetpackComposeBasicLineChart(modelProducer, modifier)
}