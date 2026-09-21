package com.marjuk.eaptracker.ui
import androidx.compose.ui.res.stringResource

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marjuk.eaptracker.LocalHazeState
import com.marjuk.eaptracker.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    initialTab: String = "Profile",
    onBack: () -> Unit,
    onNavigateToFullRoutine: (String) -> Unit = {},
    onNavigateToFullExams: (String) -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToSetupWizard: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hazeState = LocalHazeState.current

    var showAdminPinDialog by remember { mutableStateOf(false) }

    // Reactive Profile States from ProfileRepository
    val userName by ProfileRepository.name
    val targetInstitution by ProfileRepository.targetInstitution
    val collegeName by ProfileRepository.college
    val hscBatch by ProfileRepository.hscBatch
    val rollNo by ProfileRepository.rollNo
    val avatarEmoji by ProfileRepository.avatar
    val quoteText by ProfileRepository.quote
    val targetExamName by ProfileRepository.targetExamName
    val targetExamEpoch by ProfileRepository.targetExamDateEpoch
    val studyGoalHours by ProfileRepository.dailyStudyHoursGoal
    val preferredTrack by ProfileRepository.preferredTrack

    // Metrics from Repositories
    val (completedChapters, totalChapters) = SyllabusRepository.getTotalChaptersStats()
    val syllabusPercent = if (totalChapters > 0) (completedChapters.toFloat() / totalChapters) * 100f else 0f

    val (totalExams, totalObtained, totalMax) = ProgressRepository.getOverallExamStats()
    val examAvg = if (totalMax > 0f) (totalObtained / totalMax) * 100f else 0f

    // Dialog control states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showProfilePhotoOptionsDialog by remember { mutableStateOf(false) }
    var showTargetExamDialog by remember { mutableStateOf(false) }
    var selectedTargetExamForEdit by remember { mutableStateOf<TargetExam?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTestNotificationDialog by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<String?>(null) } // "morning", "evening", "countdown"
    var resetDialogType by remember { mutableStateOf<String?>(null) } // "syllabus", "exams"

    var selectedImageForCropUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageForCropUri = uri
        }
    }

    val gso = remember(context) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googlePhotoSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    BackupRepository.updateAccountState(account)
                    val photoUrl = account.photoUrl?.toString()?.replace("/s96-c/", "/s400-c/")?.replace("=s96-c", "=s400-c")
                    if (!photoUrl.isNullOrBlank()) {
                        ProfileRepository.updateGooglePhotoUrl(photoUrl)
                        ProfileRepository.setProfilePhoto(photoUrl)
                        Toast.makeText(context, "Google profile photo synchronized!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No profile photo found on this Google account", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Google sign-in: ${e.localizedMessage ?: "Failed"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Live Countdown state
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val remainingMillis = maxOf(0L, targetExamEpoch - currentTimeMillis)
    val remainingSecs = (remainingMillis / 1000) % 60
    val remainingMins = (remainingMillis / (1000 * 60)) % 60
    val remainingHours = (remainingMillis / (1000 * 60 * 60)) % 24
    val remainingDays = remainingMillis / (1000 * 60 * 60 * 24)

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar with Back button, Title & Quick Edit action
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        onBack()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (selectedTab == "Profile") "MY PROFILE" else "APP SETTINGS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (selectedTab == "Profile") {
                    Button(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            showEditProfileDialog = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // 2. Connected Profile / Settings Toggle (Home Screen Style)
        item {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Profile", "Settings").forEach { tab ->
                    val isSelected = selectedTab == tab
                    val weight by animateFloatAsState(if (isSelected) 1.5f else 1f, label = "weight")
                    val cornerRadius by animateDpAsState(if (isSelected) 26.dp else 12.dp, label = "corners")
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                        label = "bg"
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else Color.White.copy(alpha = 0.7f), 
                        label = "content"
                    )

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                selectedTab = tab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (tab == "Profile") Icons.Default.Person else Icons.Default.Settings,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // ================= TAB 1: PROFILE CONTENT =================
        if (selectedTab == "Profile") {
            // Hero Profile Card
            item {
                HeroProfileCard(
                    hazeState = hazeState,
                    name = userName,
                    target = targetInstitution,
                    college = collegeName,
                    batch = hscBatch,
                    roll = rollNo,
                    avatar = avatarEmoji,
                    quote = quoteText,
                    onAvatarClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        showProfilePhotoOptionsDialog = true
                    },
                    onEditClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        showEditProfileDialog = true
                    }
                )
            }

            // Live Target Exam Countdown Card (Swipeable Multi-Target)
            item {
                AdmissionCountdownCard(
                    hazeState = hazeState,
                    targetExams = ProfileRepository.targetExamsList,
                    currentTimeMillis = currentTimeMillis,
                    onAddTarget = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        selectedTargetExamForEdit = null
                        showTargetExamDialog = true
                    },
                    onEditTarget = { exam ->
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        selectedTargetExamForEdit = exam
                        showTargetExamDialog = true
                    }
                )
            }

            // Academic Performance Overview
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PREPARATION OVERVIEW",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Syllabus Coverage
                        ProfileMetricCard(
                            modifier = Modifier.weight(1f),
                            hazeState = hazeState,
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentColor = MaterialTheme.colorScheme.primary,
                            title = "Syllabus Coverage",
                            mainValue = "${syllabusPercent.toInt()}%",
                            subValue = "$completedChapters of $totalChapters Chapters",
                            progress = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f
                        )

                        // Exam Average
                        ProfileMetricCard(
                            modifier = Modifier.weight(1f),
                            hazeState = hazeState,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = Color(0xFF4CAF50),
                            title = "Exam Average",
                            mainValue = if (totalExams > 0) "${examAvg.toInt()}%" else "N/A",
                            subValue = if (totalExams > 0) "$totalExams Exams Logged" else "No exams yet",
                            progress = if (totalMax > 0f) totalObtained / totalMax else null
                        )
                    }
                }
            }

            // Motivational Quotes Card
            item {
                MotivationalQuotesCard(
                    hazeState = hazeState,
                    onCopyQuote = { quote ->
                        clipboardManager.setText(AnnotatedString(quote))
                        HapticHelper.performHaptic(context, HapticType.SUCCESS)
                        Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Target Institutions Showcase
            item {
                TargetInstitutionsCard(
                    hazeState = hazeState,
                    selectedTarget = targetInstitution,
                    onSelectTarget = { newTarget ->
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        ProfileRepository.updateProfile(
                            userName, newTarget, collegeName, hscBatch, rollNo, avatarEmoji, quoteText
                        )
                    }
                )
            }

            // Quick Navigation Shortcuts
            item {
                QuickNavigationShortcuts(
                    hazeState = hazeState,
                    onNavigateToFullRoutine = onNavigateToFullRoutine,
                    onNavigateToFullExams = onNavigateToFullExams
                )
            }

            // Developer & App Info Card (Always visible on Profile)
            item {
                DeveloperInfoCard(hazeState = hazeState)
            }
        }

        // ================= TAB 2: SETTINGS CONTENT =================
        else {
            // 1. Google Drive Cloud Backup & Restore Card
            item {
                GoogleDriveBackupCard(hazeState = hazeState)
            }

            // 2. Comprehensive Notification System & Alerts Settings
            item {
                NotificationsSettingsCard(
                    hazeState = hazeState,
                    onOpenTimePicker = { target ->
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        timePickerTarget = target
                    },
                    onOpenTestDialog = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        showTestNotificationDialog = true
                    }
                )
            }

            // 3. Haptic & Tactile Feedback Settings
            item {
                HapticsSettingsCard(hazeState = hazeState)
            }

            // 4. Program & Routine Preferences Settings
            item {
                RoutinePreferencesCard(
                    hazeState = hazeState,
                    studyGoalHours = studyGoalHours,
                    preferredTrack = preferredTrack,
                    onGoalChange = { ProfileRepository.updateDailyGoal(it) },
                    onTrackChange = { ProfileRepository.updatePreferredTrack(it) }
                )
            }

            // 5. Display, Study Mode & Benchmark Target
            item {
                AppPreferencesCard(hazeState = hazeState)
            }

            // 6. Data Reset Options Card (Danger Zone)
            item {
                DataResetCard(
                    hazeState = hazeState,
                    onResetSyllabus = {
                        HapticHelper.performHaptic(context, HapticType.STRONG)
                        resetDialogType = "syllabus"
                    },
                    onResetExams = {
                        HapticHelper.performHaptic(context, HapticType.STRONG)
                        resetDialogType = "exams"
                    }
                )
            }

            // 7. Developer & App Info Card (Always visible on Settings)
            item {
                DeveloperInfoCard(
                    hazeState = hazeState,
                    onRerunSetup = onNavigateToSetupWizard
                )
            }
        }
    }

    // ================= POPUPS (FROSTED GLASS EFFECT) =================

    // -1. 1:1 Image Cropper Glass Dialog
    if (selectedImageForCropUri != null) {
        ImageCropperGlassDialog(
            hazeState = hazeState,
            imageUri = selectedImageForCropUri!!,
            onDismiss = { selectedImageForCropUri = null },
            onCropSuccess = { _ ->
                selectedImageForCropUri = null
            }
        )
    }

    // 0. Profile Photo Options Glass Dialog
    if (showProfilePhotoOptionsDialog) {
        ProfilePhotoOptionsGlassDialog(
            hazeState = hazeState,
            onPickGallery = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onGrabGooglePhoto = {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                val photoUrl = account?.photoUrl?.toString()?.replace("/s96-c/", "/s400-c/")?.replace("=s96-c", "=s400-c")
                if (!photoUrl.isNullOrBlank()) {
                    ProfileRepository.updateGooglePhotoUrl(photoUrl)
                    ProfileRepository.setProfilePhoto(photoUrl)
                    Toast.makeText(context, "Google profile photo synchronized!", Toast.LENGTH_SHORT).show()
                } else {
                    googlePhotoSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            onRemovePhoto = {
                ProfileRepository.setProfilePhoto(null)
                Toast.makeText(context, "Photo removed, using initials", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showProfilePhotoOptionsDialog = false }
        )
    }

    // 1. Edit Profile Glass Modal
    if (showEditProfileDialog) {
        EditProfileGlassDialog(
            initialName = userName,
            initialTarget = targetInstitution,
            initialCollege = collegeName,
            initialBatch = hscBatch,
            initialRoll = rollNo,
            initialAvatar = avatarEmoji,
            initialQuote = quoteText,
            onAvatarClick = {
                showProfilePhotoOptionsDialog = true
            },
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, target, college, batch, roll, avatar, quote ->
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                ProfileRepository.updateProfile(name, target, college, batch, roll, avatar, quote)
                showEditProfileDialog = false
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Target Exam & Date Glass Dialog
    if (showTargetExamDialog) {
        TargetExamGlassDialog(
            initialExam = selectedTargetExamForEdit,
            onDismiss = { showTargetExamDialog = false },
            onSave = { id, name, epoch, institution ->
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                if (id != null) {
                    ProfileRepository.updateTargetExam(id, name, epoch, institution)
                } else {
                    ProfileRepository.addTargetExam(name, epoch, institution)
                }
                showTargetExamDialog = false
                Toast.makeText(context, "Admission countdown saved", Toast.LENGTH_SHORT).show()
            },
            onDelete = { id ->
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                ProfileRepository.deleteTargetExam(id)
                showTargetExamDialog = false
                Toast.makeText(context, "Admission target removed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Custom Time Picker Glass Dialog
    if (timePickerTarget != null) {
        val target = timePickerTarget!!
        val title = when (target) {
            "morning" -> "Morning Routine Alert Time"
            "evening" -> "Evening Marks Reminder Time"
            "countdown" -> "Admission Countdown Alert Time"
            else -> "Reminder Time"
        }
        val currentTime = when (target) {
            "morning" -> SettingsRepository.dailyRoutineTime.value
            "evening" -> SettingsRepository.eveningProgressTime.value
            "countdown" -> SettingsRepository.countdownAlertTime.value
            else -> "08:00"
        }

        TimePickerGlassDialog(
            title = title,
            initialTime = currentTime,
            onDismiss = { timePickerTarget = null },
            onSave = { newTime ->
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                when (target) {
                    "morning" -> SettingsRepository.setDailyRoutineAlert(SettingsRepository.dailyRoutineAlert.value, newTime, context)
                    "evening" -> SettingsRepository.setEveningProgressAlert(SettingsRepository.eveningProgressAlert.value, newTime, context)
                    "countdown" -> SettingsRepository.setCountdownAlert(SettingsRepository.countdownAlertEnabled.value, newTime, context)
                }
                timePickerTarget = null
                Toast.makeText(context, "Reminder time updated to $newTime", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Test Notification Trigger Glass Dialog
    if (showTestNotificationDialog) {
        TestNotificationGlassDialog(
            onDismiss = { showTestNotificationDialog = false },
            onTriggerTest = { type ->
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                NotificationHelper.showTestNotification(context, type)
                Toast.makeText(context, "Live test notification sent. Check status bar", Toast.LENGTH_SHORT).show()
                showTestNotificationDialog = false
            }
        )
    }

    // 5. Import Data Backup Glass Dialog
    if (showImportDialog) {
        ImportBackupGlassDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonStr ->
                val success = ProfileRepository.importAllDataFromJson(context, jsonStr)
                if (success) {
                    HapticHelper.performHaptic(context, HapticType.SUCCESS)
                    showImportDialog = false
                    Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_LONG).show()
                } else {
                    HapticHelper.performHaptic(context, HapticType.STRONG)
                    Toast.makeText(context, "Invalid backup data. Please check formatting.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 6. Reset Confirm Glass Dialog
    if (resetDialogType != null) {
        val type = resetDialogType!!
        val title = if (type == "syllabus") "Reset Syllabus Progress?" else "Reset All Exam Scores?"
        val description = if (type == "syllabus") {
            "This will uncheck all completed sections (Book, Slide, QB, Concept) across Physics, Chemistry, Higher Math, and Biology."
        } else {
            "This will delete all recorded exam marks and scores."
        }

        ResetConfirmGlassDialog(
            title = title,
            message = description,
            onDismiss = { resetDialogType = null },
            onConfirm = {
                HapticHelper.performHaptic(context, HapticType.STRONG)
                if (type == "syllabus") {
                    ProfileRepository.resetSyllabus(context)
                    Toast.makeText(context, "Syllabus progress reset.", Toast.LENGTH_SHORT).show()
                } else if (type == "exams") {
                    ProfileRepository.resetExamMarks(context)
                    Toast.makeText(context, "Exam scores cleared.", Toast.LENGTH_SHORT).show()
                }
                resetDialogType = null
            }
        )
    }
}

// ================= HERO PROFILE CARD =================

@Composable
fun HeroProfileCard(
    hazeState: HazeState,
    name: String,
    target: String,
    college: String,
    batch: String,
    roll: String,
    avatar: String,
    quote: String,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                    )
                ),
                shape
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with Glowing Ring and Camera Edit Badge
            val photoUri = ProfileRepository.profilePhotoUri.value
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF00E5FF),
                                    Color(0xFF7C4DFF),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E2C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1E88E5),
                                            Color(0xFF7C4DFF)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ProfileRepository.getInitials(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.5.dp, Color(0xFF1E1E2C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Name & Verified Tick
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Candidate",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Target Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = target,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Academic 3-Item Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AcademicMetaItem(icon = Icons.Default.School, title = "Batch", value = batch)
                VerticalDivider(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
                AcademicMetaItem(icon = Icons.Default.AccountBalance, title = "College", value = college)
                VerticalDivider(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
                AcademicMetaItem(icon = Icons.Default.Badge, title = "ID", value = roll)
            }

            if (quote.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AcademicMetaItem(icon: ImageVector, title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 100.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ================= ADMISSION COUNTDOWN CARD =================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdmissionCountdownCard(
    hazeState: HazeState,
    targetExams: List<TargetExam>,
    currentTimeMillis: Long,
    onAddTarget: () -> Unit,
    onEditTarget: (TargetExam) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val examsList = if (targetExams.isNotEmpty()) targetExams else listOf(
        TargetExam("default", "BUET Admission Test", System.currentTimeMillis() + 90L * 86400000L, "BUET")
    )
    val pagerState = rememberPagerState(pageCount = { examsList.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent)
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val exam = examsList[page]
                val remainingMillis = maxOf(0L, exam.targetEpoch - currentTimeMillis)
                val remainingSecs = (remainingMillis / 1000) % 60
                val remainingMins = (remainingMillis / (1000 * 60)) % 60
                val remainingHours = (remainingMillis / (1000 * 60 * 60)) % 24
                val remainingDays = remainingMillis / (1000 * 60 * 60 * 24)

                val targetDateStr = try {
                    val ldt = Instant.ofEpochMilli(exam.targetEpoch).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    ldt.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
                } catch (_: Exception) {
                    "Target Exam Day"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ADMISSION COUNTDOWN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF00E5FF),
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    if (examsList.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${page + 1}/${examsList.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = exam.examName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable(onClick = onAddTarget),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Exam",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                    .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), CircleShape)
                                    .clickable { onEditTarget(exam) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Change Date",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountdownTimeBlock(modifier = Modifier.weight(1f), value = "$remainingDays", unit = "DAYS", highlight = true)
                        CountdownTimeBlock(modifier = Modifier.weight(1f), value = "%02d".format(remainingHours), unit = "HOURS")
                        CountdownTimeBlock(modifier = Modifier.weight(1f), value = "%02d".format(remainingMins), unit = "MINS")
                        CountdownTimeBlock(modifier = Modifier.weight(1f), value = "%02d".format(remainingSecs), unit = "SECS", isSecs = true)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target Date: $targetDateStr",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Pagination Dots Indicator
            if (examsList.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(examsList.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(4.dp)
                                .width(if (isSelected) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownTimeBlock(
    modifier: Modifier = Modifier,
    value: String,
    unit: String,
    highlight: Boolean = false,
    isSecs: Boolean = false
) {
    val bgColor = if (highlight) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)
    val textColor = if (highlight) Color(0xFF00E5FF) else if (isSecs) MaterialTheme.colorScheme.primary else Color.White

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            )
        )
    }
}

// ================= PROFILE METRIC CARD =================

@Composable
fun ProfileMetricCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    mainValue: String,
    subValue: String,
    progress: Float? = null
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
        }
    }
}

// ================= MOTIVATIONAL QUOTES CARD =================

@Composable
fun MotivationalQuotesCard(
    hazeState: HazeState,
    onCopyQuote: (String) -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)
    val quoteItem by QuotesRepository.currentQuote
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Discipline", "Focus", "Anti-Lazy", "Exam Ready", "Motivation")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFB74D).copy(alpha = 0.4f), Color.White.copy(alpha = 0.06f))
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB74D).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = quoteItem.emoji, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FOCUS & MOTIVATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFB74D),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "${quoteItem.category} Boost",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Clearly separated, non-overlapping action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Next Quote Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable {
                                HapticHelper.performHaptic(context, HapticType.SELECTION)
                                QuotesRepository.getRandomQuote(selectedCategory)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "New Quote",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy Quote Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable {
                                onCopyQuote(quoteItem.quote)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Quote",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D).copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${quoteItem.quote}”",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val bg = if (isSelected) Color(0xFFFFB74D).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
                    val tc = if (isSelected) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.65f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(
                                0.5.dp,
                                if (isSelected) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                selectedCategory = cat
                                QuotesRepository.getRandomQuote(cat)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = tc,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ================= NOTIFICATION SYSTEM & ALERTS SETTINGS =================

@Composable
fun NotificationsSettingsCard(
    hazeState: HazeState,
    onOpenTimePicker: (String) -> Unit,
    onOpenTestDialog: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    val masterNotif by SettingsRepository.notificationsMaster
    val quoteNotif by SettingsRepository.quoteNotificationsEnabled
    val quoteInterval by SettingsRepository.quoteIntervalHours
    val sleepStart by SettingsRepository.sleepStartHour
    val sleepEnd by SettingsRepository.sleepEndHour

    val dailyRoutineAlert by SettingsRepository.dailyRoutineAlert
    val dailyRoutineTime by SettingsRepository.dailyRoutineTime

    val eveningAlert by SettingsRepository.eveningProgressAlert
    val eveningProgressTime by SettingsRepository.eveningProgressTime

    val countdownAlert by SettingsRepository.countdownAlertEnabled
    val countdownAlertTime by SettingsRepository.countdownAlertTime

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Master Switch Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NOTIFICATIONS & REMINDERS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (masterNotif) "Active • Smart Daily Reminders" else "All Notifications Paused",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (masterNotif) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = masterNotif,
                    onCheckedChange = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setNotificationMaster(it, context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E5FF)
                    )
                )
            }

            if (masterNotif) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Periodic Quotes Booster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "3-Hour Motivation Boost",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Inspiring study reminders every $quoteInterval hours",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Switch(
                                checked = quoteNotif,
                                onCheckedChange = {
                                    HapticHelper.performHaptic(context, HapticType.SELECTION)
                                    SettingsRepository.setQuoteNotifications(it, context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (quoteNotif) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Interval Frequency:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 2, 3, 4, 6).forEach { hours ->
                                    val isSel = quoteInterval == hours
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                                            .border(
                                                1.dp,
                                                if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                                SettingsRepository.setQuoteInterval(hours, context)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${hours}h",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E2C).copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = Color(0xFF7C4DFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Quiet Sleep Window: ${sleepStart}:00 to 0${sleepEnd}:00 (Paused while sleeping)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Daily Exam & Routine Alert
                NotificationAlertRow(
                    icon = Icons.Default.WbSunny,
                    accentColor = Color(0xFFFFB74D),
                    title = "Daily Routine & Exam Alert",
                    description = "Morning summary of today's classes and scheduled exams",
                    time = dailyRoutineTime,
                    enabled = dailyRoutineAlert,
                    onToggle = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setDailyRoutineAlert(it, dailyRoutineTime, context)
                    },
                    onEditTime = { onOpenTimePicker("morning") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Evening Progress & Marks Reminder
                NotificationAlertRow(
                    icon = Icons.Default.Nightlight,
                    accentColor = Color(0xFF7C4DFF),
                    title = "Evening Marks & Progress Check",
                    description = "Evening prompt to input today's exam marks and check off chapters",
                    time = eveningProgressTime,
                    enabled = eveningAlert,
                    onToggle = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setEveningProgressAlert(it, eveningProgressTime, context)
                    },
                    onEditTime = { onOpenTimePicker("evening") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Target Exam Countdown Alert
                NotificationAlertRow(
                    icon = Icons.Default.HourglassTop,
                    accentColor = Color(0xFF00E5FF),
                    title = "Target Countdown Reminder",
                    description = "Daily countdown alert keeping your dream institution in focus",
                    time = countdownAlertTime,
                    enabled = countdownAlert,
                    onToggle = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setCountdownAlert(it, countdownAlertTime, context)
                    },
                    onEditTime = { onOpenTimePicker("countdown") }
                )


            }
        }
    }
}

@Composable
fun NotificationAlertRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    time: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Alert Time:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onEditTime() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = format12Hour(time),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Time",
                                tint = accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun format12Hour(time24: String): String {
    val parts = time24.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (hour >= 12) "PM" else "AM"
    val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return "%02d:%02d %s".format(hour12, min, period)
}

// ================= HAPTIC & TACTILE FEEDBACK SETTINGS =================

@Composable
fun HapticsSettingsCard(hazeState: HazeState) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    val hapticEnabled by SettingsRepository.hapticEnabled
    val hapticIntensity by SettingsRepository.hapticIntensity

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF7C4DFF).copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HAPTIC FEEDBACK",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (hapticEnabled) "Tactile vibration responses" else "Disabled",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (hapticEnabled) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        SettingsRepository.setHapticEnabled(it)
                        if (it) HapticHelper.performHaptic(context, HapticType.SUCCESS)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C4DFF)
                    )
                )
            }

            if (hapticEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Vibration Intensity Level:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light", "Medium", "Strong").forEach { level ->
                        val isSel = hapticIntensity == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) Color(0xFF7C4DFF).copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    SettingsRepository.setHapticIntensity(level)
                                    val testType = when (level) {
                                        "Light" -> HapticType.LIGHT
                                        "Strong" -> HapticType.STRONG
                                        else -> HapticType.MEDIUM
                                    }
                                    HapticHelper.performHaptic(context, testType)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSel) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= ROUTINE & TRACK PREFERENCES =================

@Composable
fun RoutinePreferencesCard(
    hazeState: HazeState,
    studyGoalHours: Int,
    preferredTrack: String,
    onGoalChange: (Int) -> Unit,
    onTrackChange: (String) -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    val defaultMode by SettingsRepository.defaultRoutineMode
    val autoScroll by SettingsRepository.autoScrollToToday

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ROUTINE & STUDY PREFERENCES",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Daily Target Study Hours",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Ideal for admission consistency",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (studyGoalHours > 2) {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                onGoalChange(studyGoalHours - 1)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "$studyGoalHours hrs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = {
                            if (studyGoalHours < 18) {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                onGoalChange(studyGoalHours + 1)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Default Routine Mode:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Offline", "Online").forEach { mode ->
                    val isSelected = defaultMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                SettingsRepository.setDefaultRoutineMode(mode)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$mode Routine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setAutoScrollToToday(!autoScroll)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Scroll to Today's Routine",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Jump to current day of week on launch",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }

                Checkbox(
                    checked = autoScroll,
                    onCheckedChange = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setAutoScrollToToday(it)
                    }
                )
            }
        }
    }
}

// ================= APP PREFERENCES & BENCHMARK =================

@Composable
fun AppPreferencesCard(hazeState: HazeState) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    val benchmarkPct by SettingsRepository.benchmarkPassPercentage
    val keepScreenOn by SettingsRepository.keepScreenOn

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TARGET BENCHMARK & DISPLAY",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exam Target Benchmark",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$benchmarkPct% Target",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = benchmarkPct.toFloat(),
                        onValueChange = {
                            SettingsRepository.setBenchmarkPassPercentage(it.toInt())
                        },
                        valueRange = 50f..95f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setKeepScreenOn(!keepScreenOn)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keep Screen On (Study Mode)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Prevents sleep while reading routine & Question Bank (QB)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }

                Checkbox(
                    checked = keepScreenOn,
                    onCheckedChange = {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        SettingsRepository.setKeepScreenOn(it)
                    }
                )
            }
        }
    }
}

// ================= TARGET INSTITUTIONS CARD =================

@Composable
fun TargetInstitutionsCard(
    hazeState: HazeState,
    selectedTarget: String,
    onSelectTarget: (String) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DREAM INSTITUTION & TARGET",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ProfileRepository.institutionPresets) { preset ->
                    val isSelected = selectedTarget.equals(preset, ignoreCase = true)
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.07f)
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectTarget(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

// ================= QUICK NAVIGATION SHORTCUTS =================

@Composable
fun QuickNavigationShortcuts(
    hazeState: HazeState,
    onNavigateToFullRoutine: (String) -> Unit,
    onNavigateToFullExams: (String) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "PROGRAM SCHEDULE SHORTCUTS",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigateToFullRoutine("offline") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Full Routine",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { onNavigateToFullExams("offline") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Exam Schedule",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ================= DATA MANAGEMENT & BACKUP CARD =================

// ================= DATA RESET & DANGER ZONE =================

@Composable
fun DataResetCard(
    hazeState: HazeState,
    onResetSyllabus: () -> Unit,
    onResetExams: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    var showDangerZone by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.2f)),
                    blurRadius = 20.dp
                )
            )
            .border(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.25f), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDangerZone = !showDangerZone },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "RESET DATA OPTIONS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Clear checklist progress or logged exam scores",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Icon(
                    imageVector = if (showDangerZone) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = showDangerZone) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetSyllabus,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Syllabus Checklist (QB & Theory)", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = onResetExams,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Exam Marks", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

// ================= DEVELOPER & APP INFO CARD =================

@Composable
fun DeveloperInfoCard(
    hazeState: HazeState,
    onRerunSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    tint = HazeTint(Color.Black.copy(alpha = 0.22f)),
                    blurRadius = 24.dp
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                    )
                ),
                shape
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DEVELOPER",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF7C4DFF),
                                    Color(0xFF00E5FF)
                                )
                            )
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dev_marjuk),
                        contentDescription = "Marjuk Amin",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Marjuk Amin",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Creator & Lead Developer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/marjuk06"))
                        context.startActivity(intent)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "EAP TRACKER • v1.0.0",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Engineering Admission Program Tracker",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// ================= PROFILE PHOTO OPTIONS GLASS DIALOG =================

@Composable
fun ProfilePhotoOptionsGlassDialog(
    hazeState: HazeState,
    onPickGallery: () -> Unit,
    onGrabGooglePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentPhoto = ProfileRepository.profilePhotoUri.value

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(28.dp),
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROFILE AVATAR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Current Avatar Preview
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF00E5FF),
                                    Color(0xFF7C4DFF),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E2C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = currentPhoto,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = ProfileRepository.getInitials(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Pick from Gallery & Crop 1:1 Option
                Button(
                    onClick = {
                        onDismiss()
                        onPickGallery()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Choose from Gallery & Crop",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // 2. Grab / Sync Google Profile Photo (Always available)
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onGrabGooglePhoto()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grab Google Profile Photo",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // 3. Remove / Reset to Monogram
                if (!currentPhoto.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRemovePhoto()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove Photo (Use Initials)",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= EDIT PROFILE GLASS DIALOG =================

@Composable
fun EditProfileGlassDialog(
    initialName: String,
    initialTarget: String,
    initialCollege: String,
    initialBatch: String,
    initialRoll: String,
    initialAvatar: String,
    initialQuote: String,
    onAvatarClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var target by remember { mutableStateOf(initialTarget) }
    var college by remember { mutableStateOf(initialCollege) }
    var batch by remember { mutableStateOf(initialBatch) }
    var roll by remember { mutableStateOf(initialRoll) }
    var avatar by remember { mutableStateOf(initialAvatar) }
    var quote by remember { mutableStateOf(initialQuote) }

    val hazeState = LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "EDIT PROFILE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Avatar Preview & Quick Change
                val photoUri = ProfileRepository.profilePhotoUri.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = ProfileRepository.getInitials(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile Photo",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (!photoUri.isNullOrBlank()) "Custom / Google Photo" else "Student Initials",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Button(
                        onClick = onAvatarClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Change",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = { Text("Target Goal (e.g. BUET CSE)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = batch,
                                onValueChange = { batch = it },
                                label = { Text("Batch") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                                )
                            )

                            OutlinedTextField(
                                value = roll,
                                onValueChange = { /* Read only - permanent student ID */ },
                                label = { Text("Candidate ID") },
                                singleLine = true,
                                readOnly = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                                )
                            )
                        }

                        OutlinedTextField(
                            value = college,
                            onValueChange = { college = it },
                            label = { Text("College / Institution") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = quote,
                            onValueChange = { quote = it },
                            label = { Text("Aspirant Motto / Bio") },
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, target, college, batch, roll, avatar, quote)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Profile",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ================= CALENDAR DATE PICKER GLASS DIALOG =================

@Composable
fun CalendarDatePickerGlassDialog(
    initialEpoch: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current

    val initialCalendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = if (initialEpoch > System.currentTimeMillis() - 86400000L) initialEpoch else System.currentTimeMillis()
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCalendar.get(Calendar.MONTH)) } // 0..11
    var selectedDay by remember { mutableIntStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }

    val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val daysOfWeek = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOffset = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SELECT TARGET DATE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month / Year Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                            selectedDay = minOf(selectedDay, 28)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = Color.White)
                    }

                    Text(
                        text = "${monthNames[selectedMonth]} $selectedYear",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    IconButton(
                        onClick = {
                            HapticHelper.performHaptic(context, HapticType.LIGHT)
                            if (selectedMonth == 11) {
                                selectedMonth = 0
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                            selectedDay = minOf(selectedDay, 28)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week header (Sat to Fri)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (dayName == "Fri") Color(0xFFEF5350) else Color(0xFF00E5FF).copy(alpha = 0.8f),
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Matrix
                val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7
                val todayCal = Calendar.getInstance()
                val isCurrentMonthAndYear = todayCal.get(Calendar.YEAR) == selectedYear && todayCal.get(Calendar.MONTH) == selectedMonth
                val todayDate = todayCal.get(Calendar.DAY_OF_MONTH)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOffset + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = selectedDay == dayNumber
                                    val isToday = isCurrentMonthAndYear && todayDate == dayNumber

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF00E5FF)
                                                    isToday -> Color(0xFF00E5FF).copy(alpha = 0.25f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                color = if (isToday && !isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                HapticHelper.performHaptic(context, HapticType.LIGHT)
                                                selectedDay = dayNumber
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.Black
                                                    isToday -> Color(0xFF00E5FF)
                                                    col == 6 -> Color(0xFFEF5350).copy(alpha = 0.85f)
                                                    else -> Color.White
                                                },
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val chosenCal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, 10, 0, 0)
                }
                val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                val diffDays = maxOf(0L, (chosenCal.timeInMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFmt.format(chosenCal.time),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "$diffDays Days Left",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.SUCCESS)
                        onDateSelected(chosenCal.timeInMillis)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm Date",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ================= TARGET EXAM GLASS DIALOG =================

@Composable
fun TargetExamGlassDialog(
    initialExam: TargetExam? = null,
    onDismiss: () -> Unit,
    onSave: (id: String?, examName: String, targetEpoch: Long, institution: String) -> Unit,
    onDelete: ((id: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var examName by remember { mutableStateOf(initialExam?.examName ?: "BUET Admission Test") }
    var targetEpoch by remember { mutableLongStateOf(initialExam?.targetEpoch ?: (System.currentTimeMillis() + 90L * 86400000L)) }
    var institution by remember { mutableStateOf(initialExam?.institution ?: "") }
    var showCalendarPicker by remember { mutableStateOf(false) }

    val examPresets = listOf(
        "BUET Preliminary Admission Test" to "BUET",
        "BUET Final Written Test" to "BUET",
        "CKRUET Combined Admission Test" to "CKRUET",
        "DU A-Unit Admission Exam" to "Dhaka University",
        "IUT Admission Test" to "IUT",
        "MIST Admission Test" to "MIST",
        "Medical Admission Test" to "DGHS",
        "IBA BBA Admission Test" to "IBA DU"
    )

    val hazeState = LocalHazeState.current

    if (showCalendarPicker) {
        CalendarDatePickerGlassDialog(
            initialEpoch = targetEpoch,
            onDismiss = { showCalendarPicker = false },
            onDateSelected = { selected ->
                targetEpoch = selected
                showCalendarPicker = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (initialExam != null) "EDIT TARGET EXAM" else "ADD TARGET EXAM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "QUICK PRESETS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(examPresets) { (presetName, presetInst) ->
                                val isSel = examName == presetName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            if (isSel) Color(0xFF00E5FF) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            examName = presetName
                                            institution = presetInst
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f),
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = examName,
                            onValueChange = { examName = it },
                            label = { Text("Exam Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = institution,
                            onValueChange = { institution = it },
                            label = { Text("Target Institution / Authority") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Themed Date Picker Trigger
                        val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                        val targetDateStr = dateFmt.format(java.util.Date(targetEpoch))
                        val diffDays = maxOf(0L, (targetEpoch - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))

                        Column {
                            Text(
                                text = "EXAM DATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                                        showCalendarPicker = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = targetDateStr,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "$diffDays days from now",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White.copy(alpha = 0.55f)
                                                )
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Pick Date",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF00E5FF),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialExam != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(initialExam.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF5350)
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Delete", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (examName.isNotBlank()) {
                                onSave(initialExam?.id, examName, targetEpoch, institution)
                            }
                        },
                        modifier = Modifier
                            .weight(if (initialExam != null) 1.5f else 1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialExam != null) "Update Target" else "Save Target",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

// ================= TIME PICKER GLASS DIALOG =================

// ================= TIME PICKER GLASS DIALOG (AUTHENTIC MIUI / HYPEROS STYLE) =================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteXiaomiWheelPicker(
    count: Int,
    formatItem: (Int) -> String,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeightDp: androidx.compose.ui.unit.Dp = 48.dp,
    visibleItemsCount: Int = 5,
    onRawIndexChanged: ((oldRaw: Int, newRaw: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val repeats = 200
    val totalCount = count * repeats
    val startIndex = (repeats / 2) * count + (initialIndex % count)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex - (visibleItemsCount / 2))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerRawIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) startIndex
            else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visible.minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - centerOffset)
                }?.index ?: startIndex
            }
        }
    }

    var previousRawIndex by remember { mutableIntStateOf(startIndex) }
    val selectedValue = (centerRawIndex % count + count) % count

    LaunchedEffect(centerRawIndex) {
        if (centerRawIndex != previousRawIndex) {
            onRawIndexChanged?.invoke(previousRawIndex, centerRawIndex)
            previousRawIndex = centerRawIndex
            onItemSelected(selectedValue)
            HapticHelper.performHaptic(context, HapticType.SELECTION)
        }
    }

    val viewportHeight = itemHeightDp * visibleItemsCount

    Box(
        modifier = modifier.height(viewportHeight),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalCount) { rawIndex ->
                val actualValue = (rawIndex % count + count) % count
                val distance = kotlin.math.abs(rawIndex - centerRawIndex)
                val (alpha, fontSize, fontWeight) = when (distance) {
                    0 -> Triple(1f, 38.sp, FontWeight.Normal)
                    1 -> Triple(0.35f, 26.sp, FontWeight.Light)
                    else -> Triple(0.12f, 18.sp, FontWeight.ExtraLight)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatItem(actualValue),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun XiaomiAmPmPicker(
    selectedAmPm: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemHeightDp: androidx.compose.ui.unit.Dp = 48.dp
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.height(itemHeightDp * 5),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AM
        Box(
            modifier = Modifier
                .height(itemHeightDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (selectedAmPm != "AM") {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        onSelect("AM")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AM",
                fontSize = if (selectedAmPm == "AM") 22.sp else 18.sp,
                fontWeight = if (selectedAmPm == "AM") FontWeight.Normal else FontWeight.Light,
                color = if (selectedAmPm == "AM") Color.White else Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // PM
        Box(
            modifier = Modifier
                .height(itemHeightDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (selectedAmPm != "PM") {
                        HapticHelper.performHaptic(context, HapticType.SELECTION)
                        onSelect("PM")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PM",
                fontSize = if (selectedAmPm == "PM") 22.sp else 18.sp,
                fontWeight = if (selectedAmPm == "PM") FontWeight.Normal else FontWeight.Light,
                color = if (selectedAmPm == "PM") Color.White else Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TimePickerGlassDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val rawHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val rawMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val initialAmPm = if (rawHour < 12) "AM" else "PM"
    val initial12Hour = if (rawHour == 0) 12 else if (rawHour > 12) rawHour - 12 else rawHour

    var selectedAmPm by remember { mutableStateOf(initialAmPm) }
    var selected12Hour by remember { mutableIntStateOf(initial12Hour) }
    var selectedMinute by remember { mutableIntStateOf(rawMinute) }

    val hazeState = LocalHazeState.current

    // Dynamic relative countdown text (e.g. "Alert in 13 hours 58 minutes")
    val relativeTimeText = remember(selectedAmPm, selected12Hour, selectedMinute) {
        val hour24 = if (selectedAmPm == "AM") {
            if (selected12Hour == 12) 0 else selected12Hour
        } else {
            if (selected12Hour == 12) 12 else selected12Hour + 12
        }
        val now = java.time.LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        val targetMinutes = hour24 * 60 + selectedMinute
        var diff = targetMinutes - nowMinutes
        if (diff <= 0) diff += 24 * 60
        val hours = diff / 60
        val minutes = diff % 60
        when {
            hours > 0 && minutes > 0 -> "Alert in $hours hours $minutes minutes"
            hours > 0 -> "Alert in $hours hours"
            minutes > 0 -> "Alert in $minutes minutes"
            else -> "Alert in 24 hours"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row (Close 'X' - Title & Relative Subtitle - Save '✓')
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.5.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = relativeTimeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    IconButton(
                        onClick = {
                            val hour24 = if (selectedAmPm == "AM") {
                                if (selected12Hour == 12) 0 else selected12Hour
                            } else {
                                if (selected12Hour == 12) 12 else selected12Hour + 12
                            }
                            val formatted = "%02d:%02d".format(hour24, selectedMinute)
                            onSave(formatted)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // MIUI Style Floating Wheels (No inner boxes)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Column Labels ('H' and 'M')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1.1f))
                        Text(
                            text = "H",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "M",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // 3 Vertical Wheels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. AM / PM Column
                        XiaomiAmPmPicker(
                            selectedAmPm = selectedAmPm,
                            onSelect = { selectedAmPm = it },
                            modifier = Modifier.weight(1.1f),
                            itemHeightDp = 48.dp
                        )

                        // 2. Hour Wheel (01 - 12) with Auto AM/PM Toggle across 12-hour boundary
                        InfiniteXiaomiWheelPicker(
                            count = 12,
                            formatItem = { "%02d".format(it + 1) },
                            initialIndex = (selected12Hour - 1).coerceIn(0, 11),
                            onItemSelected = { selected12Hour = it + 1 },
                            onRawIndexChanged = { oldRaw, newRaw ->
                                val oldCycle = Math.floorDiv(oldRaw + 1, 12)
                                val newCycle = Math.floorDiv(newRaw + 1, 12)
                                if (oldCycle != newCycle) {
                                    val delta = kotlin.math.abs(newCycle - oldCycle)
                                    if (delta % 2 != 0) {
                                        selectedAmPm = if (selectedAmPm == "AM") "PM" else "AM"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            itemHeightDp = 48.dp,
                            visibleItemsCount = 5
                        )

                        // 3. Minute Wheel (00 - 59)
                        InfiniteXiaomiWheelPicker(
                            count = 60,
                            formatItem = { "%02d".format(it) },
                            initialIndex = selectedMinute.coerceIn(0, 59),
                            onItemSelected = { selectedMinute = it },
                            modifier = Modifier.weight(1f),
                            itemHeightDp = 48.dp,
                            visibleItemsCount = 5
                        )
                    }
                }
            }
        }
    }
}

// ================= TEST NOTIFICATION GLASS DIALOG =================

@Composable
fun TestNotificationGlassDialog(
    onDismiss: () -> Unit,
    onTriggerTest: (String) -> Unit
) {
    val hazeState = LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "LIVE TEST ALERTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select a notification type to trigger immediately on your phone status bar:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f)),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestAlertOptionButton(
                        icon = Icons.Default.FormatQuote,
                        accentColor = Color(0xFFFFB74D),
                        title = "Motivational Quote Boost",
                        subtitle = "Random quote from your curated 38 list",
                        onClick = { onTriggerTest("quote") }
                    )

                    TestAlertOptionButton(
                        icon = Icons.Default.WbSunny,
                        accentColor = Color(0xFFFF9800),
                        title = "Morning Routine Alert",
                        subtitle = "Today's classes, topics and scheduled exams",
                        onClick = { onTriggerTest("routine") }
                    )

                    TestAlertOptionButton(
                        icon = Icons.Default.Nightlight,
                        accentColor = Color(0xFF7C4DFF),
                        title = "Evening Marks Reminder",
                        subtitle = "Prompt to log exam marks & QB chapters",
                        onClick = { onTriggerTest("evening") }
                    )

                    TestAlertOptionButton(
                        icon = Icons.Default.HourglassTop,
                        accentColor = Color(0xFF00E5FF),
                        title = "Admission Countdown Alert",
                        subtitle = "Days left until target exam alert",
                        onClick = { onTriggerTest("countdown") }
                    )

                    TestAlertOptionButton(
                        icon = Icons.Default.RocketLaunch,
                        accentColor = MaterialTheme.colorScheme.primary,
                        title = "Trigger All Test Alerts",
                        subtitle = "Test all notification channels at once",
                        onClick = { onTriggerTest("all") }
                    )
                }
            }
        }
    }
}

@Composable
fun TestAlertOptionButton(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 10.5.sp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

// ================= IMPORT BACKUP GLASS DIALOG =================

@Composable
fun ImportBackupGlassDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    val hazeState = LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "RESTORE BACKUP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Paste your exported JSON backup data below to restore your syllabus checkmarks (Book, Slide, QB, Concept), exam scores, and profile details.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = { jsonText = it },
                            label = { Text("Backup JSON") },
                            placeholder = { Text("{\n  \"profile\": {...},\n  \"examScores\": {...}\n}") },
                            minLines = 5,
                            maxLines = 8,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            onImport(jsonText.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restore All Data",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ================= RESET CONFIRM GLASS DIALOG =================

@Composable
fun ResetConfirmGlassDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val hazeState = LocalHazeState.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        tint = HazeTint(Color.Black.copy(alpha = 0.35f)),
                        blurRadius = 24.dp
                    )
                )
                .border(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF5252).copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DANGER ZONE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yes, Reset", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


