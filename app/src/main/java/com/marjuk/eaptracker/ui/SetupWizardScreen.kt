package com.marjuk.eaptracker.ui

import androidx.compose.ui.res.stringResource

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.marjuk.eaptracker.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.marjuk.eaptracker.R
import com.marjuk.eaptracker.ui.admin.RemoteSyncManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun SetupWizardScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    // Setup Wizard Form States
    var studentName by remember { mutableStateOf(ProfileRepository.name.value) }
    var collegeName by remember { mutableStateOf(ProfileRepository.college.value) }
    var hscBatch by remember { mutableStateOf(ProfileRepository.hscBatch.value) }
    var targetInstitution by remember { mutableStateOf(ProfileRepository.targetInstitution.value) }
    var studyGoalHours by remember { mutableIntStateOf(ProfileRepository.dailyStudyHoursGoal.intValue) }
    var preferredTrack by remember { mutableStateOf(ProfileRepository.preferredTrack.value) }
    var studentAvatarUri by remember { mutableStateOf(ProfileRepository.profilePhotoUri.value) }

    // Dialogs & Launchers
    var selectedImageForCropUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    // Google Sign-In & Backup Detect State
    var isCheckingDriveBackup by remember { mutableStateOf(false) }
    var detectedBackupInfo by remember { mutableStateOf<CloudBackupInfo?>(null) }
    var isRestoringBackup by remember { mutableStateOf(false) }
    var restoreProgressMsg by remember { mutableStateOf("") }
    var restoreSuccess by remember { mutableStateOf(false) }

    // Finalizing Screen State
    var isFinalizing by remember { mutableStateOf(false) }
    var finalizeStep by remember { mutableIntStateOf(0) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageForCropUri = uri
            showCropDialog = true
        }
    }

    // Google Sign In Client
    val gso = remember(context) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Check Drive Backup Helper
    fun checkExistingDriveBackup(email: String) {
        scope.launch(Dispatchers.IO) {
            isCheckingDriveBackup = true
            val info = BackupRepository.fetchLatestDriveBackupInfo(context, email)
            withContext(Dispatchers.Main) {
                isCheckingDriveBackup = false
                detectedBackupInfo = info
            }
        }
    }

    // Common Restore & Auto-Fill Handler
    fun performRestoreAndAutoFill(email: String) {
        scope.launch(Dispatchers.IO) {
            isRestoringBackup = true
            val res = BackupRepository.performGoogleDriveRestore(
                context = context,
                email = email,
                showNotification = true,
                onProgress = { msg -> restoreProgressMsg = msg }
            )
            withContext(Dispatchers.Main) {
                isRestoringBackup = false
                if (res.isSuccess) {
                    restoreSuccess = true
                    // Re-sync all wizard fields from restored repositories
                    ProfileRepository.init(context)
                    SyllabusRepository.init(context)
                    ExamMarksRepository.init(context)
                    SettingsRepository.init(context)
                    studentName = ProfileRepository.name.value
                    collegeName = ProfileRepository.college.value
                    hscBatch = ProfileRepository.hscBatch.value
                    targetInstitution = ProfileRepository.targetInstitution.value
                    preferredTrack = ProfileRepository.preferredTrack.value
                    studyGoalHours = ProfileRepository.dailyStudyHoursGoal.intValue
                    studentAvatarUri = ProfileRepository.profilePhotoUri.value
                    Toast.makeText(context, "🎉 Welcome back! Profile & progress restored from Google Drive!", Toast.LENGTH_LONG).show()
                    if (pagerState.currentPage == 0) {
                        pagerState.animateScrollToPage(1)
                    }
                } else {
                    Toast.makeText(context, "Restore: ${res.exceptionOrNull()?.message ?: "No backup found. Setup ready."}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launch Google Sign In
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    BackupRepository.updateAccountState(account)
                    if (studentName.isBlank()) {
                        studentName = account.displayName ?: studentName
                    }
                    val photo = account.photoUrl?.toString()?.replace("/s96-c/", "/s400-c/")?.replace("=s96-c", "=s400-c")
                    if (!photo.isNullOrBlank()) {
                        studentAvatarUri = photo
                        ProfileRepository.updateGooglePhotoUrl(photo)
                        ProfileRepository.setProfilePhoto(photo)
                    }
                    val email = account.email
                    if (!email.isNullOrBlank()) {
                        scope.launch(Dispatchers.IO) {
                            isCheckingDriveBackup = true
                            val info = BackupRepository.fetchLatestDriveBackupInfo(context, email)
                            withContext(Dispatchers.Main) {
                                isCheckingDriveBackup = false
                                detectedBackupInfo = info
                                if (info != null) {
                                    performRestoreAndAutoFill(email)
                                } else {
                                    Toast.makeText(context, "Google Account connected: ${account.email}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Google sign-in: ${e.localizedMessage ?: "Failed"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-check on load if already logged in
    LaunchedEffect(Unit) {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            BackupRepository.updateAccountState(lastAccount)
            val email = lastAccount.email
            if (!email.isNullOrBlank()) {
                checkExistingDriveBackup(email)
            }
        }
    }

    // Crop Image Dialog
    if (showCropDialog && selectedImageForCropUri != null) {
        ImageCropperGlassDialog(
            hazeState = hazeState,
            imageUri = selectedImageForCropUri!!,
            onDismiss = {
                showCropDialog = false
                selectedImageForCropUri = null
            },
            onCropSuccess = { savedPath ->
                studentAvatarUri = savedPath
                showCropDialog = false
                selectedImageForCropUri = null
            }
        )
    }

    // Finalizing Screen Overlay
    if (isFinalizing) {
        FinalizingSetupScreen(
            step = finalizeStep,
            onAnimationDone = {
                val permanentRoll = ProfileRepository.ensurePermanentStudentId(hscBatch.ifBlank { "HSC '25" })
                ProfileRepository.updateProfile(
                    newName = studentName.ifBlank { "Future Engineer" },
                    newTarget = targetInstitution.ifBlank { "BUET" },
                    newCollege = collegeName.ifBlank { "College" },
                    newBatch = hscBatch.ifBlank { "HSC '25" },
                    newRoll = permanentRoll,
                    newAvatar = ProfileRepository.avatar.value,
                    newQuote = ProfileRepository.quote.value
                )
                ProfileRepository.updateDailyGoal(studyGoalHours)
                ProfileRepository.updatePreferredTrack(preferredTrack)
                if (!studentAvatarUri.isNullOrBlank()) {
                    ProfileRepository.setProfilePhoto(studentAvatarUri)
                }
                com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud(context)
                SettingsRepository.setSetupCompleted(true)
                HapticHelper.performHaptic(context, HapticType.SUCCESS)
                onComplete()
            }
        )
        return
    }

    // Main Wizard Scaffold
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D14))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient decorative background mesh glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.75f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EAP TRACKER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Aspirant Onboarding",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }

                // Step Indicator Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Step ${pagerState.currentPage + 1} of 4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Horizontal Pager for Wizard Steps
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> Step0WelcomeView(
                        onGetStarted = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onConnectGoogle = { signInLauncher.launch(googleSignInClient.signInIntent) },
                        isCheckingBackup = isCheckingDriveBackup,
                        detectedBackup = detectedBackupInfo,
                        isRestoring = isRestoringBackup,
                        restoreProgress = restoreProgressMsg,
                        restoreSuccess = restoreSuccess,
                        onRestoreBackup = {
                            val email = BackupRepository.connectedAccountEmail.value
                            if (!email.isNullOrBlank()) {
                                performRestoreAndAutoFill(email)
                            }
                        }
                    )
                    1 -> Step1ProfileView(
                        name = studentName,
                        onNameChange = { studentName = it },
                        college = collegeName,
                        onCollegeChange = { collegeName = it },
                        batch = hscBatch,
                        onBatchChange = { hscBatch = it },
                        avatarUri = studentAvatarUri,
                        onPickGallery = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onGrabGooglePhoto = {
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            val photo = account?.photoUrl?.toString()?.replace("/s96-c/", "/s400-c/")?.replace("=s96-c", "=s400-c")
                            if (!photo.isNullOrBlank()) {
                                studentAvatarUri = photo
                                ProfileRepository.updateGooglePhotoUrl(photo)
                                ProfileRepository.setProfilePhoto(photo)
                                Toast.makeText(context, "Google profile photo synchronized!", Toast.LENGTH_SHORT).show()
                            } else {
                                signInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        connectedEmail = BackupRepository.connectedAccountEmail.value,
                        onConnectGoogle = { signInLauncher.launch(googleSignInClient.signInIntent) }
                    )
                    2 -> Step2TargetView(
                        target = targetInstitution,
                        onTargetChange = { targetInstitution = it },
                        studyHours = studyGoalHours,
                        onStudyHoursChange = { studyGoalHours = it }
                    )
                    3 -> Step3CloudAndTrackView(
                        track = preferredTrack,
                        onTrackChange = { preferredTrack = it },
                        onConnectGoogle = { signInLauncher.launch(googleSignInClient.signInIntent) },
                        isCheckingBackup = isCheckingDriveBackup,
                        detectedBackup = detectedBackupInfo,
                        isRestoring = isRestoringBackup,
                        restoreProgress = restoreProgressMsg,
                        restoreSuccess = restoreSuccess,
                        onRestoreBackup = {
                            val email = BackupRepository.connectedAccountEmail.value
                            if (!email.isNullOrBlank()) {
                                performRestoreAndAutoFill(email)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (hidden on page 0)
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isCurrent = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isCurrent) 22.dp else 7.dp, label = "dotWidth")
                        val color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next / Finish Button
                Button(
                    onClick = {
                        if (pagerState.currentPage == 1 && studentName.isBlank()) {
                            android.widget.Toast.makeText(context, "Please enter your full name to continue", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (pagerState.currentPage < 3) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            if (BackupRepository.connectedAccountEmail.value.isNullOrBlank()) {
                                android.widget.Toast.makeText(context, "Google Account connection is required to secure your candidate credentials", android.widget.Toast.LENGTH_LONG).show()
                                signInLauncher.launch(googleSignInClient.signInIntent)
                                return@Button
                            }
                            // Trigger Finalizing Screen with M3 Animation
                            isFinalizing = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == 3) "Finish Setup" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage == 3) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ================= STEP 0: WELCOME VIEW =================

@Composable
private fun Step0WelcomeView(
    onGetStarted: () -> Unit,
    onConnectGoogle: () -> Unit,
    isCheckingBackup: Boolean,
    detectedBackup: CloudBackupInfo?,
    isRestoring: Boolean,
    restoreProgress: String,
    restoreSuccess: Boolean,
    onRestoreBackup: () -> Unit
) {
    val connectedEmail = BackupRepository.connectedAccountEmail.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ===== RETURNING USER RESTORE CARD (shows when connected or backup found) =====
        if (!connectedEmail.isNullOrBlank() && (detectedBackup != null || isCheckingBackup || isRestoring || restoreSuccess)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (restoreSuccess) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (restoreSuccess) Color(0xFF81C784) else Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (restoreSuccess) "✅ Welcome Back!" else "☁️ Cloud Backup Detected!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (restoreSuccess) Color(0xFF81C784) else Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Text(
                                text = if (restoreSuccess)
                                    "Profile, progress & all data restored. Continue below!"
                                else if (isRestoring)
                                    restoreProgress.ifBlank { "Restoring your data..." }
                                else if (isCheckingBackup)
                                    "Checking Drive for your backup..."
                                else
                                    "Backup from ${detectedBackup?.let { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) } ?: "Google Drive"} detected for $connectedEmail",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    if (!restoreSuccess && !isRestoring && !isCheckingBackup && detectedBackup != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRestoreBackup,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF0D0D0D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore & Auto-Fill My Profile", fontWeight = FontWeight.Black, color = Color(0xFF0D0D0D), fontSize = 13.sp)
                        }
                    }
                    if (isRestoring || isCheckingBackup) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ===== RETURNING USER SIGN IN CARD (no account connected yet) =====
        if (connectedEmail.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF7C4DFF).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.40f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Returning Aspirant?",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color(0xFFCE93D8),
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Text(
                                text = "Sign in with Google to instantly restore your name, college, target & all study progress — no re-typing needed!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onConnectGoogle,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCE93D8))
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFFCE93D8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google & Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Glowing Hero Emblem with App Logo
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF00E5FF),
                            Color(0xFF7C4DFF),
                            Color(0xFF81C784),
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF101726)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "EAP Tracker Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome Aspirant!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your Ultimate Engineering Admission Companion for BUET, CKRUET, DU & Leading Engineering Programs.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Key Feature Glass Badges
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WizardFeatureGlassCard(
                icon = Icons.Default.CalendarMonth,
                title = "Smart Class & Exam Routines",
                subtitle = "Saturday to Friday weekly layout with instant schedule updates."
            )
            WizardFeatureGlassCard(
                icon = Icons.Default.Leaderboard,
                title = "Syllabus Mastery & Exam Scores",
                subtitle = "Track every chapter across QB, Concept & Slides with benchmark analytics."
            )
            WizardFeatureGlassCard(
                icon = Icons.Default.CloudSync,
                title = "Google Drive Cloud Backup",
                subtitle = "Restore all your scores & checkmarks anytime across devices."
            )
        }
    }
}

// ================= STEP 1: PROFILE & AVATAR VIEW =================

@Composable
private fun Step1ProfileView(
    name: String,
    onNameChange: (String) -> Unit,
    college: String,
    onCollegeChange: (String) -> Unit,
    batch: String,
    onBatchChange: (String) -> Unit,
    avatarUri: String?,
    onPickGallery: () -> Unit,
    onGrabGooglePhoto: () -> Unit,
    connectedEmail: String? = null,
    onConnectGoogle: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aspirant Profile",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        )
        Text(
            text = "Set up your student badge & credentials",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fast-track Google auto-fill banner (shown when not yet connected)
        if (connectedEmail.isNullOrBlank() && name.isBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fast-track with Google",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        )
                        Text(
                            text = "Connect Google to auto-fill your name, photo & restore previous data.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onConnectGoogle,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Connect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else if (!connectedEmail.isNullOrBlank()) {
            // Already connected — show compact badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF81C784).copy(alpha = 0.10f),
                border = BorderStroke(0.5.dp, Color(0xFF81C784).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google connected: $connectedEmail",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Avatar Preview with Glowing Border
        Box(
            modifier = Modifier
                .size(90.dp)
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
                .background(Color(0xFF181F2E)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else if (name.isNotBlank()) {
                val initials = name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
                Text(
                    text = initials.ifBlank { name.take(2).uppercase() },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Placeholder",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Avatar Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPickGallery,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onGrabGooglePhoto,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Google Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // College Field
        OutlinedTextField(
            value = college,
            onValueChange = onCollegeChange,
            label = { Text("College / Institution", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // HSC Batch Chips
        Text(
            text = "HSC Batch",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("HSC '25", "HSC '26", "Second Timer").forEach { item ->
                val selected = batch == item
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onBatchChange(item) }
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ================= STEP 2: DREAM TARGET & GOALS VIEW =================

@Composable
private fun Step2TargetView(
    target: String,
    onTargetChange: (String) -> Unit,
    studyHours: Int,
    onStudyHoursChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dream Target & Goals",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        )
        Text(
            text = "Define your engineering aspiration and study rhythm",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Target Goal Selection Chips
        Text(
            text = "Target Engineering Program",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        val popularTargets = listOf("BUET CSE", "BUET EEE", "BUET ME", "CKRUET Combined", "DU A-Unit", "IUT / MIST")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                popularTargets.take(3).forEach { prog ->
                    val selected = target == prog
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f).clickable { onTargetChange(prog) }
                    ) {
                        Text(
                            text = prog,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                popularTargets.drop(3).forEach { prog ->
                    val selected = target == prog
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f).clickable { onTargetChange(prog) }
                    ) {
                        Text(
                            text = prog,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = target,
            onValueChange = onTargetChange,
            label = { Text("Custom Goal (e.g. BUET NavArch, RUET CSE)", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Study Hours Goal Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Study Target",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "$studyHours Hours / Day",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = studyHours.toFloat(),
            onValueChange = { onStudyHoursChange(it.roundToInt()) },
            valueRange = 4f..16f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Benchmark advice card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF00E5FF).copy(alpha = 0.08f),
            border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "8-10 focused hours daily is the proven benchmark for engineering top rankers.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// ================= STEP 3: CLOUD & TRACK VIEW =================

@Composable
private fun Step3CloudAndTrackView(
    track: String,
    onTrackChange: (String) -> Unit,
    onConnectGoogle: () -> Unit,
    isCheckingBackup: Boolean,
    detectedBackup: CloudBackupInfo?,
    isRestoring: Boolean,
    restoreProgress: String,
    restoreSuccess: Boolean,
    onRestoreBackup: () -> Unit
) {
    val connectedEmail = BackupRepository.connectedAccountEmail.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Track & Cloud Sync",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        )
        Text(
            text = "Connect Google Account & select your routine track",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Routine Track Selection
        Text(
            text = "Program Track",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Offline + Online", "Offline Only", "Online Only").forEach { item ->
                val selected = track == item
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f).clickable { onTrackChange(item) }
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Drive Cloud Backup Section
        Text(
            text = "Google Account Connection (Required)",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (connectedEmail.isNullOrBlank()) {
                    // Not connected state
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link Google Account",
                                style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Auto-save marks & syllabus progress to your Google Drive",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            )
                        }
                    }

                    Button(
                        onClick = onConnectGoogle,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Google Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    // Connected State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF81C784).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Connected Account",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = connectedEmail,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Medium)
                                )
                            }
                        }

                        TextButton(onClick = onConnectGoogle) {
                            Text("Switch", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Cloud Backup Check & Restore
                    if (isCheckingBackup) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking for prior cloud backups...",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            )
                        }
                    } else if (restoreSuccess) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF81C784).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF81C784).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All syllabus checkmarks & test marks restored from cloud!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF81C784),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    } else if (detectedBackup != null) {
                        // Found Backup Card
                        val dateFormatted = try {
                            val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
                            sdf.format(Date(detectedBackup.timestamp))
                        } catch (_: Exception) { "Recent Backup" }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Previous Backup Detected",
                                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "$dateFormatted (${detectedBackup.formattedSize})",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        )
                                    }
                                }

                                if (isRestoring) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = restoreProgress.ifBlank { "Restoring data..." },
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = onRestoreBackup,
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore My Past Progress", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fresh student workspace initialized and connected to Google Drive.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= FINALIZING SETUP SCREEN (MATERIAL 3 SYNC ANIMATION) =================

@Composable
private fun FinalizingSetupScreen(
    step: Int,
    onAnimationDone: () -> Unit
) {
    val context = LocalContext.current
    var animPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(600)
        animPhase = 1 // Syllabus
        delay(700)
        animPhase = 2 // Routine
        delay(700)
        animPhase = 3 // Cloud
        delay(800)
        animPhase = 4 // Done

        // Trigger Welcome Notification on Setup Completion
        try {
            NotificationHelper.showScheduleUpdateNotification(
                context = context,
                title = "Welcome to EAP Tracker",
                message = "Your engineering admission workspace is ready. Syllabus, routines, and study targets are synchronized."
            )
            NotificationHistoryRepository.addNotification(
                title = "Welcome to EAP Tracker",
                message = "Your engineering admission workspace is ready. Syllabus, routines, and study targets are synchronized.",
                category = "Notice"
            )
            RemoteSyncManager.startRealtimeListener(context)
            com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.syncTelemetryToCloud(context)
            com.marjuk.eaptracker.ui.admin.StudentTelemetryManager.startIndividualStudentListener(context)
        } catch (_: Exception) {}

        delay(600)
        onAnimationDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Google Material 3 Expressive Loading Indicator (Polygon Morphing)
            com.marjuk.eaptracker.ui.components.M3ExpressiveLoadingIndicator(
                size = 72.dp,
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.5.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Setting Up Your Workspace",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preparing your engineering admission command center",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step Progress Checklist Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FinalizeStatusRow(
                        title = "Engineering Syllabus & Chapter Modules",
                        isCompleted = animPhase >= 1,
                        isActive = animPhase == 0
                    )
                    FinalizeStatusRow(
                        title = "Saturday-to-Friday Routine & Orientation",
                        isCompleted = animPhase >= 2,
                        isActive = animPhase == 1
                    )
                    FinalizeStatusRow(
                        title = "Google Drive Cloud Sync & Reminders",
                        isCompleted = animPhase >= 3,
                        isActive = animPhase == 2
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalizeStatusRow(
    title: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF81C784)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        } else if (isActive) {
            com.marjuk.eaptracker.ui.components.M3ExpressiveLoadingIndicator(
                color = MaterialTheme.colorScheme.primary,
                size = 22.dp,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isCompleted) Color.White else if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                fontWeight = if (isCompleted || isActive) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

/**
 * Official Material Design 3 Synchronized Multi-Arc Morphing Loading Indicator
 * Matches Google Material 3 Loading-Indicator-Overview-A-Sync spec.
 */
@Composable
fun Material3SyncLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_sync_indicator")

    // Rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Sweep angle expansion and contraction
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                30f at 0 using FastOutSlowInEasing
                270f at 1200 using FastOutSlowInEasing
                30f at 2400 using FastOutSlowInEasing
            }
        ),
        label = "sweep"
    )

    // Offset angle progression
    val startAngleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        ),
        label = "startAngle"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Inner subtle pulsing glow
        Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.Transparent),
                    radius = size.width / 2f
                )
            )
        }

        // Multi-arc morphing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Primary Morphing Arc (Cyan to Purple)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFF81C784),
                        Color(0xFF00E5FF)
                    )
                ),
                startAngle = rotation + startAngleOffset,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Secondary Opposing Accent Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF81C784),
                        Color(0xFF00E5FF),
                        Color(0xFF81C784)
                    )
                ),
                startAngle = rotation + startAngleOffset + 180f,
                sweepAngle = sweepAngle * 0.45f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth * 0.75f, cap = StrokeCap.Round)
            )
        }
    }
}

// ================= WIZARD HELPER COMPOSABLES =================

@Composable
private fun WizardFeatureGlassCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
