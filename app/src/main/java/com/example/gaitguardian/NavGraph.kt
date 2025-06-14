package com.example.gaitguardian

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.gaitguardian.screens.SettingsScreen
import com.example.gaitguardian.screens.StartScreen
import com.example.gaitguardian.screens.clinician.ClinicianDetailedPatientViewScreen
import com.example.gaitguardian.screens.clinician.ClinicianHomeScreen
import com.example.gaitguardian.screens.patient.AssessmentInfoScreen
import com.example.gaitguardian.screens.patient.FtfsAssessmentScreen
import com.example.gaitguardian.screens.patient.GaitAssessmentScreen
import com.example.gaitguardian.screens.patient.PatientHomeScreen
import com.example.gaitguardian.screens.patient.TugAssessmentScreen
import com.example.gaitguardian.screens.patient.VideoCaptureScreen
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel


@Composable
fun NavGraph(
    navController: NavHostController,
//    modifier: Modifier = Modifier,
    patientViewModel: PatientViewModel,
    clinicianViewModel: ClinicianViewModel
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route
    val currentUserView by clinicianViewModel.getCurrentUserView.collectAsState(initial = "")

    // Upon app start,
    // check what is the saved current view and load directly into the graph
    LaunchedEffect(currentUserView) {
        when (currentUserView) {
            "clinician" -> {
                navController.navigate("clinician_graph") {
                    popUpTo("start_screen") { inclusive = true }
                }
            }
            "patient" -> {
                navController.navigate("patient_graph") {
                    popUpTo("start_screen") { inclusive = true }
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
//            PatientTopBar()
            if (currentDestination != null) {
                navTopBar(navController, currentDestination)
            }
        },
        bottomBar = {
            NavigationBar(
//                containerColor = Color(0xFFFFC279),
                containerColor = Color.White,
//                containerColor =Color(0xFFFFD9A1),
            ) {
                // List of Bottom Nav Bar Icons
                val bottomNavItems = listOf(
                    Triple("Home", "patient_home_screen", Icons.Filled.Home to Icons.Outlined.Home),
                    Triple(
                        "Settings",
                        "settings_screen",
                        Icons.Filled.Settings to Icons.Outlined.Settings
                    )
                )
                bottomNavItems.forEach { (navLabel, route, icons) ->
                    val isSelected = currentDestination == route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navController.navigate(route) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) icons.first else icons.second,
                                contentDescription = navLabel,
                                tint = Color(0xFFE18F00)
                            )
                        },
                        label = {
                            Text(
                                navLabel,
                                color = Color(0xFFE18F00),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent // Removes active indicator
                        )
                    )
                }
            }
        }

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "start_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            // General Screens here
            composable("start_screen")
            {
                StartScreen(navController, patientViewModel, clinicianViewModel)
            }
            composable("settings_screen")
            {
                SettingsScreen(navController)
            }
            // Clinician-Specific Screens here
            navigation(
                startDestination = "clinician_home_screen", route = "clinician_graph"
            )
            {
                composable("clinician_home_screen") {
                    ClinicianHomeScreen(navController, clinicianViewModel,patientViewModel)
                }
                composable("clinician_detailed_patient_view_screen") {
                    ClinicianDetailedPatientViewScreen(navController)
                }
            }

            // Patient-Specific Screens here
            navigation(
                startDestination = "patient_home_screen", route = "patient_graph"
            )
            {
                composable("patient_home_screen") {
                    PatientHomeScreen(navController, patientViewModel)
                }
                composable(
                    route = "assessment_info_screen/{assessmentTitle}",
                    arguments = listOf(navArgument("assessmentTitle") { type = NavType.StringType })
                ) { backStackEntry ->
                    AssessmentInfoScreen(
                        navController = navController,
                        modifier = Modifier,
                        assessmentTitle = backStackEntry.arguments?.getString("assessmentTitle")
                            ?: "Assessment"
                    )
                }
                composable("gait_assessment_screen") {
                    GaitAssessmentScreen(navController)
                }
                composable("tug_assessment_screen") {
                    TugAssessmentScreen(navController)
                }
                composable("ftfs_assessment_screen") {
                    FtfsAssessmentScreen(navController)
                }
                composable("video_capture_screen") {
                    VideoCaptureScreen(navController)

                }
            }

        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun navTopBar(navController: NavHostController, currentDestination: String) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFFFFF6DD),
            titleContentColor = Color.Black,
        ),
        title = {
            Text(
                text = "GaitGuardian",
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (currentDestination != "start_screen") {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {
                navController.navigate("settings_screen")
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    )
}