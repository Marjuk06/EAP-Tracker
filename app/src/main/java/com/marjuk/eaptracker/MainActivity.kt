package com.marjuk.eaptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marjuk.eaptracker.ui.*
import com.marjuk.eaptracker.ui.theme.EAPTrackerTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }

class MainActivity : ComponentActivity() {

    companion object {
        val pendingNavigation = androidx.compose.runtime.mutableStateOf<String?>(null)
        var hasShownSplashThisSession = false
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        SyllabusRepository.init(this)
        ExamMarksRepository.init(this)
        ProgressRepository.init(this)
        ProfileRepository.init(this)
        SettingsRepository.init(this)
        StudyTrackerRepository.init(this)
        BackupRepository.init(this)
        NotificationHelper.init(this)
        NotificationHistoryRepository.init(this)
        com.marjuk.eaptracker.ui.admin.DynamicRoutineRepository.init(this)
        com.marjuk.eaptracker.ui.admin.DynamicExamRepository.init(this)
        com.marjuk.eaptracker.ui.admin.RemoteSyncManager.init(this)
        com.marjuk.eaptracker.ui.admin.AppUpdateManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Subscribe to FCM Topics and Sync Device FCM Token for Instant Push Notifications
        try {
            com.marjuk.eaptracker.service.EAPFirebaseMessagingService.subscribeToTopics()
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    com.marjuk.eaptracker.service.EAPFirebaseMessagingService.uploadFcmToken(this, token)
                }
            }
            NotificationHelper.schedulePeriodicWork(this)
            NotificationHelper.rescheduleAll(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Silent background sync with Universal REST API endpoint & Student Telemetry
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.marjuk.eaptracker.ui.admin.RemoteSyncManager.fetchFromCloud(this@MainActivity) { _, _ -> }
                if (SettingsRepository.isSetupCompleted.value) {
                    com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.startIndividualStudentListener(this@MainActivity)
                    com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud(this@MainActivity)
                    NotificationHelper.checkAndDeliverTodaysScheduleOnFirstOpen(this@MainActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()
        val hasDirectNav = intent?.getStringExtra("navigate_to")?.isNotBlank() == true
        val startWithSplash = !hasShownSplashThisSession && !hasDirectNav

        setContent {
            val keepScreenOn by SettingsRepository.keepScreenOn
            androidx.compose.runtime.LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            EAPTrackerTheme {
                var showSplash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(startWithSplash) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    label = "splashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        AnimatedSplashScreen(
                            onSplashFinished = { 
                                showSplash = false
                                hasShownSplashThisSession = true
                            }
                        )
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val dest = intent?.getStringExtra("navigate_to")
        if (!dest.isNullOrBlank()) {
            pendingNavigation.value = dest
        }

        // Handle In-App Update Intent from Push Notification
        val apkUrl = intent?.getStringExtra("apk_url") ?: intent?.getStringExtra("apkUrl")
        val isUpdate = intent?.getStringExtra("type") == "app_update" || !apkUrl.isNullOrBlank()
        if (isUpdate && !apkUrl.isNullOrBlank()) {
            val versionName = intent?.getStringExtra("version_name") ?: intent?.getStringExtra("versionName") ?: ""
            val versionCode = intent?.getStringExtra("version_code")?.toIntOrNull() ?: 0
            val title = intent?.getStringExtra("title") ?: "🚀 New Update Available"
            val changelog = intent?.getStringExtra("changelog") ?: intent?.getStringExtra("message") ?: ""
            val isForce = intent?.getBooleanExtra("is_force", false) ?: false
            com.marjuk.eaptracker.ui.admin.AppUpdateManager.handleRemoteUpdatePayload(
                this, apkUrl, versionName, versionCode, title, changelog, isForce
            )
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hazeState = androidx.compose.runtime.remember { HazeState() }
    val defaultHazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tint = HazeTint(Color.Black.copy(alpha = 0.15f)),
        blurRadius = 20.dp
    )

    val pendingNav by MainActivity.pendingNavigation
    androidx.compose.runtime.LaunchedEffect(pendingNav) {
        pendingNav?.let { target ->
            when (target) {
                "home", "routine" -> navController.navigate(NavigationItem.Home.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                "syllabus" -> navController.navigate(NavigationItem.Syllabus.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                "exams" -> navController.navigate(NavigationItem.Exams.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                "study_stats" -> navController.navigate("study_statistics") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                "progress" -> navController.navigate(NavigationItem.Progress.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                "profile" -> navController.navigate(NavigationItem.Profile.route) {
                    launchSingleTop = true
                }
                "settings" -> navController.navigate("settings") {
                    launchSingleTop = true
                }
            }
            MainActivity.pendingNavigation.value = null
        }
    }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalHazeStyle provides defaultHazeStyle
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Subtle Material 3 Background Treatment
            Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
                M3Background()
            }

            // In-App Auto Update Dialog
            com.marjuk.eaptracker.ui.admin.AppUpdateDialog()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { innerPadding ->
            val isSetupDone by SettingsRepository.isSetupCompleted
            val startDestination = if (isSetupDone) NavigationItem.Home.route else "setup_wizard"

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                )
            ) {
                composable("setup_wizard") {
                    SetupWizardScreen(
                        onComplete = {
                            navController.navigate(NavigationItem.Home.route) {
                                popUpTo("setup_wizard") { inclusive = true }
                            }
                        }
                    )
                }
                composable(NavigationItem.Home.route) { 
                    HomeScreen(
                        onNavigateToFullRoutine = { type ->
                            navController.navigate("full_routine/$type")
                        },
                        onNavigateToExams = {
                            navController.navigate(NavigationItem.Exams.route)
                        },
                        onNavigateToStudyStats = {
                            navController.navigate("study_statistics")
                        },
                        onOpenFullScreenTimer = {
                            navController.navigate("fullscreen_focus")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        }
                    ) 
                }
                composable(NavigationItem.Syllabus.route) { SyllabusScreen() }
                composable(NavigationItem.Exams.route) { 
                    ExamsScreen(onNavigateToFullExams = { type ->
                        navController.navigate("full_exams/$type")
                    }) 
                }
                composable(NavigationItem.Progress.route) { 
                    ProgressScreen(
                        onNavigateToStudyStats = {
                            navController.navigate("study_statistics")
                        }
                    ) 
                }
                composable("study_statistics") {
                    StudyStatisticsScreen(onBack = { navController.popBackStack() })
                }
                composable("fullscreen_focus") {
                    FullScreenFocusTimerScreen(onBack = { navController.popBackStack() })
                }
                composable("fullscreen_focus_timer") {
                    FullScreenFocusTimerScreen(onBack = { navController.popBackStack() })
                }
                composable(NavigationItem.Profile.route) { 
                    ProfileScreen(
                        initialTab = "Profile",
                        onBack = { navController.popBackStack() },
                        onNavigateToFullRoutine = { type ->
                            navController.navigate("full_routine/$type")
                        },
                        onNavigateToFullExams = { type ->
                            navController.navigate("full_exams/$type")
                        },
                        onNavigateToAdmin = {
                            navController.navigate("admin_panel")
                        },
                        onNavigateToSetupWizard = {
                            navController.navigate("setup_wizard")
                        }
                    ) 
                }
                composable("settings") { 
                    ProfileScreen(
                        initialTab = "Settings",
                        onBack = { navController.popBackStack() },
                        onNavigateToFullRoutine = { type ->
                            navController.navigate("full_routine/$type")
                        },
                        onNavigateToFullExams = { type ->
                            navController.navigate("full_exams/$type")
                        },
                        onNavigateToAdmin = {
                            navController.navigate("admin_panel")
                        },
                        onNavigateToSetupWizard = {
                            navController.navigate("setup_wizard")
                        }
                    ) 
                }
                composable(
                    route = NavigationItem.FullRoutine.route,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "offline"
                    FullRoutineScreen(type = type, onBack = { navController.popBackStack() })
                }
                composable(
                    route = "full_exams/{type}",
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "offline"
                    FullExamScheduleScreen(type = type, onBack = { navController.popBackStack() })
                }
                composable("admin_panel") {
                    com.marjuk.eaptracker.ui.admin.AdminDashboardScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        var showNotificationInboxDialog by remember { mutableStateOf(false) }

        if (showNotificationInboxDialog) {
            NotificationInboxDialog(
                hazeState = hazeState,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onDismiss = { showNotificationInboxDialog = false }
            )
        }

        // Unified Top-Right Action Hub (Notification Bell + Settings Gear) across ALL screens
        AnimatedVisibility(
            visible = currentRoute != NavigationItem.Profile.route && 
                      currentRoute != "settings" &&
                      currentRoute != "study_statistics" && 
                      currentRoute != "fullscreen_focus" &&
                      currentRoute != "admin_panel" &&
                      currentRoute != "setup_wizard" &&
                      currentRoute?.startsWith("full_") != true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Notification (Bell) Icon
                val unreadCount = NotificationHistoryRepository.itemsState.count { !it.isRead }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            showNotificationInboxDialog = true
                            NotificationHistoryRepository.markAllAsRead()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else "$unreadCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // 2. Profile & Settings (Top Right Icon) -> Opens ProfileScreen first
                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        navController.navigate(NavigationItem.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Profile & Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Overlay the FloatingNavbar at the bottom
        AnimatedVisibility(
            visible = currentRoute?.startsWith("full_") != true && 
                      currentRoute != "study_statistics" && 
                      currentRoute != "fullscreen_focus" &&
                      currentRoute != "admin_panel" &&
                      currentRoute != "setup_wizard",
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingNavbar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        // User tapped active tab again while inside child subscreen -> Pop back to root
                        navController.popBackStack(route, inclusive = false)
                    }
                }
            )
        }
    }
    }
}

@Composable
fun M3Background() {
    val shapeColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    val accentShapeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Right-side upper geometric wedge (behind Online toggle & top-right of routine)
            val rightTopShape = Path().apply {
                moveTo(w, h * 0.24f)
                cubicTo(w * 0.90f, h * 0.28f, w * 0.84f, h * 0.35f, w * 0.88f, h * 0.42f)
                lineTo(w, h * 0.46f)
                close()
            }
            drawPath(path = rightTopShape, color = shapeColor)

            // 2. Left-side middle organic curve (behind routine days 17, 18, 19)
            val leftMidShape = Path().apply {
                moveTo(0f, h * 0.50f)
                cubicTo(w * 0.14f, h * 0.55f, w * 0.15f, h * 0.63f, 0f, h * 0.68f)
                close()
            }
            drawPath(path = leftMidShape, color = shapeColor)

            // 3. Right-side lower curve (behind routine bottom / full routine button)
            val rightBottomShape = Path().apply {
                moveTo(w, h * 0.70f)
                cubicTo(w * 0.86f, h * 0.75f, w * 0.85f, h * 0.82f, w, h * 0.88f)
                close()
            }
            drawPath(path = rightBottomShape, color = accentShapeColor)

            // 4. Subtle ambient lighting highlights
            drawCircle(
                color = glowColor,
                radius = size.maxDimension * 0.30f,
                center = Offset(w * 0.85f, h * 0.35f)
            )
            drawCircle(
                color = glowColor,
                radius = size.maxDimension * 0.28f,
                center = Offset(w * 0.10f, h * 0.60f)
            )
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.headlineLarge.copy(
                color = Color.White,
                letterSpacing = 2.sp
            )
        )
    }
}
