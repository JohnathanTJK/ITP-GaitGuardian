package com.example.gaitguardian

import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.gaitguardian.screens.SplashScreen
import com.example.gaitguardian.screens.StartScreen
import com.example.gaitguardian.screens.camera.ARCoreDistanceCheckScreen
//import com.example.gaitguardian.screens.camera.CameraScreenWithDistanceDetection
//import com.example.gaitguardian.screens.camera.IntegratedCameraScreen
import com.example.gaitguardian.screens.camera.NewCameraScreen
//import com.example.gaitguardian.screens.camera.NewCameraScreen
import com.example.gaitguardian.screens.camera.mergedsiti.CameraScreen
import com.example.gaitguardian.screens.cameraTEST.LateralDistanceCameraScreen
//import com.example.gaitguardian.screens.cameraTEST.WalkableDistanceScreen
import com.example.gaitguardian.screens.clinician.ClinicianDetailedPatientViewScreen
import com.example.gaitguardian.screens.clinician.ClinicianHomeScreen
import com.example.gaitguardian.screens.clinician.PerformanceScreen
import com.example.gaitguardian.screens.clinician.PinEntryExample
import com.example.gaitguardian.screens.patient.AssessmentInfoScreen
import com.example.gaitguardian.screens.patient.FtfsAssessmentScreen
import com.example.gaitguardian.screens.patient.GaitAssessmentScreen
import com.example.gaitguardian.screens.patient.LoadingScreen
import com.example.gaitguardian.screens.patient.LateralCoverageScreen
import com.example.gaitguardian.screens.patient.ManageVideoPrivacyScreen
import com.example.gaitguardian.screens.patient.PatientHomeScreen
import com.example.gaitguardian.screens.patient.ResultScreen
import com.example.gaitguardian.screens.patient.TugAssessmentScreen
import com.example.gaitguardian.screens.patient.VideoCaptureScreen
import com.example.gaitguardian.screens.patient.VideoInstructionScreen
import com.example.gaitguardian.screens.patient.ViewVideosScreen
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NavTopBar(navController: NavHostController, currentDestination: String) {
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
            if (currentDestination != "patient_home_screen" &&
                currentDestination != "clinician_home_screen"
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.Black,
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
                    tint = Color.Black
                )
            }
        }
    )
}



@Composable
fun NavGraph(
    navController: NavHostController,
//    modifier: Modifier = Modifier,
    patientViewModel: PatientViewModel,
    clinicianViewModel: ClinicianViewModel,
    tugDataViewModel: TugDataViewModel
) {
    val saveVideos by patientViewModel.saveVideos.collectAsState()
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val currentUserView by clinicianViewModel.getCurrentUserView.collectAsState()
    // to track if the initial navigation already happened
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

//    // Upon app start,
//    // check what is the saved current view and load directly into the graph
    LaunchedEffect(currentUserView) {
        if (!hasNavigated) {
            when (currentUserView) {
                null -> {
                    navController.navigate("start_screen") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
                "clinician" -> {
                    navController.navigate("clinician_graph") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
                "patient" -> {
                    navController.navigate("patient_graph") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
            }
            hasNavigated = true
        }
    }
    if (currentDestination == "splash_screen") {
        // No Scaffold — just directly display SplashScreen
        SplashScreen(navController, clinicianViewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
//            PatientTopBar()
                if (currentDestination != null && currentDestination != "camera_screen/{assessmentTitle}" && currentDestination != "3m_screen" && currentDestination != "gpt_screen" && currentDestination != "start_screen"
                    && currentDestination != "lateral_screen") {
                    NavTopBar(navController, currentDestination)
                }
            },
            bottomBar = {
                if (currentDestination != "camera_screen/{assessmentTitle}" && currentDestination != "3m_screen" && currentDestination != "gpt_screen" && currentDestination != "start_screen" && currentDestination != "lateral_screen") {
                    NavigationBar(
                        containerColor = Color.White,
                    ) {
                        // Dynamically assign home route based on user view
                        val homeRoute = if (currentUserView == "clinician") "clinician_home_screen" else "patient_home_screen"

                        val bottomNavItems = listOf(
                            Triple(
                                "Home",
                                homeRoute,
                                Icons.Filled.Home to Icons.Outlined.Home
                            ),
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
                                onClick = {
                                    // Avoid adding duplicate copies in back stack
                                    if (navController.currentDestination?.route != route) {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
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
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "splash_screen",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("splash_screen") {
                    SplashScreen(navController, clinicianViewModel)
                }
                composable("start_screen") {
                    StartScreen(navController, patientViewModel, clinicianViewModel)
                }
                composable("settings_screen") {
                    SettingsScreen(
                        navController = navController,
                        patientViewModel = patientViewModel,
                        isClinician = currentUserView == "clinician",
                        clinicianViewModel = clinicianViewModel
                    )
                }
                // Clinician-Specific Screens here
                navigation(
//                    startDestination = "clinician_home_screen", route = "clinician_graph"
                    startDestination = "clinician_pin_verification_screen", route = "clinician_graph"

                )
                {
                    composable("clinician_pin_verification_screen") {
                        PinEntryExample(navController)
                    }
                    composable("clinician_home_screen") {
                        ClinicianHomeScreen(navController, clinicianViewModel, patientViewModel, tugDataViewModel)
                    }
//                    composable("clinician_detailed_patient_view_screen") {
//                        ClinicianDetailedPatientViewScreen(navController)
//                    }
                    composable("clinician_detailed_patient_view_screen/{testId}") { backStackEntry ->
                        val testId = backStackEntry.arguments?.getString("testId")?.toIntOrNull()
                        if (testId != null) {
                            ClinicianDetailedPatientViewScreen(navController, clinicianViewModel, tugDataViewModel, testId)
                        }
                    }
//                    composable("clinician_detailed_patient_view_screen") {
//                        ClinicianDetailedPatientViewScreen(navController)
//                    }

                    composable("performance_screen")
                    {
                        PerformanceScreen()
                    }
                    composable("3m_screen")
                    {
                        LateralCoverageScreen()
                    }
//                    composable("camera_screen")
//                    {
////                    CameraScreen()
//                    NewCameraScreen(navController, patientViewModel)
////                    CameraScreenWithDistanceDetection()
////                    IntegratedCameraScreen(navController,patientViewModel)
//                }
                    composable("camera_screen/{assessmentTitle}") { backStackEntry ->
                        val assessmentTitle = backStackEntry.arguments?.getString("assessmentTitle")
                        if (assessmentTitle != null)
                        {
                            NewCameraScreen(navController, patientViewModel,tugDataViewModel, assessmentTitle)
                        }
//                        ManageVideoPrivacyScreen(navController, patientViewModel)
                    }
                composable("ar_test")
                {
                    ARCoreDistanceCheckScreen(
                        onDistanceMet = {navController.navigate("camera_screen")}
                    )
                }
                    composable("lateral_screen")
                    {
                        LateralDistanceCameraScreen()
                    }
//                composable("gpt_screen")
//                {
//                    WalkableDistanceScreen()
//                }
            }

                // Patient-Specific Screens here
                navigation(
                    startDestination = "patient_home_screen", route = "patient_graph"
                )
                {
                    composable("patient_home_screen") {
                        PatientHomeScreen(navController, patientViewModel, tugDataViewModel)
                    }
                    composable(
                        route = "assessment_info_screen/{assessmentTitle}",
                        arguments = listOf(navArgument("assessmentTitle") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        AssessmentInfoScreen(
                            navController = navController,
                            modifier = Modifier,
                            patientViewModel = patientViewModel,
                            tugViewModel = tugDataViewModel,
                            assessmentTitle = backStackEntry.arguments?.getString("assessmentTitle")
                                ?: "Assessment"
                        )
                    }
                    composable("gait_assessment_screen") {
                        GaitAssessmentScreen(navController)
                    }
                    composable(
                        route = "assessment_instruction_screen/{assessmentType}",
                        arguments = listOf(navArgument("assessmentType") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("assessmentType") ?: ""
                        VideoInstructionScreen(navController, type)
                    }

                    composable ("tug_assessment_screen") {
                    TugAssessmentScreen (
                        navController = navController,
                        patientId = "test_patient_123" // Temporary test value
                    ) }

                    composable("ftfs_assessment_screen") {
                        FtfsAssessmentScreen(navController)
                    }
                    composable("video_capture_screen") {
                        VideoCaptureScreen(navController, patientViewModel)
                    }
                    composable("video_privacy_screen/{assessmentTitle}") { backStackEntry ->
                        val assessmentTitle = backStackEntry.arguments?.getString("assessmentTitle")
                        if (assessmentTitle != null)
                        {
                            ManageVideoPrivacyScreen(navController, patientViewModel, assessmentTitle)

                        }
//                        ManageVideoPrivacyScreen(navController, patientViewModel)
                    }
                    composable("view_videos_screen") {
                        ViewVideosScreen(navController)
                    }
//                    composable("loading_screen/{time}") { backStackEntry ->
//                        val time = backStackEntry.arguments?.getString("time")?.toIntOrNull() ?: 0
//                        LoadingScreen(navController, time)
//                    }
                    composable("loading_screen/{assessmentTitle}") { backStackEntry ->
                        val time = backStackEntry.arguments?.getString("assessmentTitle")
                        if (time != null) {
                            LoadingScreen(navController, time)
                        }
                    }
                    composable("result_screen/{assessmentTitle}") { backStackEntry ->
                        val time = backStackEntry.arguments?.getString("assessmentTitle")
                        if (time != null) {
                            ResultScreen(navController, time, patientViewModel, tugDataViewModel)
                        }
                    }
//                    composable(
//                        route = "result_screen/{time}",
//                        arguments = listOf(navArgument("time") { type = NavType.IntType })
//                    ) { backStackEntry ->
//                        val time = backStackEntry.arguments?.getInt("time") ?: 0
//                        ResultScreen(
//                            navController = navController,
//                            recordingTime = time,
//                            patientViewModel = patientViewModel
//                        )
//                    }
                }
            }
        }
    }
}


