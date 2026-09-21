package com.marjuk.eaptracker.ui

import androidx.compose.ui.res.stringResource
import com.marjuk.eaptracker.R

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.AccountPicker
import com.google.android.gms.common.api.ApiException
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun GoogleDriveBackupCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePrimary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(26.dp)

    // Reactive backup states
    val accountEmail by BackupRepository.connectedAccountEmail
    val lastBackupTime = BackupRepository.getFormattedLastBackupTime()
    val lastBackupSize = BackupRepository.getFormattedLastBackupSize()
    val isBackupRunning by BackupRepository.isBackupInProgress
    val isRestoreRunning by BackupRepository.isRestoreInProgress
    val progressText by BackupRepository.backupProgressText
    val autoFreq by BackupRepository.autoBackupFrequency
    val wifiOnly by BackupRepository.backupOverWifiOnly

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreInfo by remember { mutableStateOf<CloudBackupInfo?>(null) }
    var showFreqDialog by remember { mutableStateOf(false) }

    // Google Sign-In Client (Requests profile, email & drive scopes so real user avatars and names show in popup)
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

    // Consent Launcher (In case Google Drive scope needs explicit one-time user approval)
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val currentEmail = accountEmail
            if (!currentEmail.isNullOrBlank()) {
                scope.launch {
                    val res = BackupRepository.performGoogleDriveBackup(
                        context = context,
                        email = currentEmail,
                        onConsentRequired = { intent -> /* Already handling */ }
                    ) {}
                    if (res.isSuccess) {
                        Toast.makeText(context, "Backup uploaded to Google Drive", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Google Sign-In Launcher (Displays official Google branded popup with photos/avatars)
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            var email: String? = null
            var displayName: String? = null

            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    BackupRepository.updateAccountState(account)
                    email = account.email
                    displayName = account.displayName
                }
            } catch (_: Exception) {}

            if (email.isNullOrBlank()) {
                email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            }

            if (!email.isNullOrBlank()) {
                BackupRepository.setAccountEmail(email, displayName)
                Toast.makeText(context, "Connected as $email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showApiConsoleGuideDialog by remember { mutableStateOf(false) }

    val launchAccountPicker: () -> Unit = {
        try {
            googleSignInClient.signOut().addOnCompleteListener {
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            try {
                signInLauncher.launch(googleSignInClient.signInIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open Google Sign-In", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Local SAF Create Document Launcher (Export)
    val exportLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = BackupRepository.exportBackupToLocalUri(context, uri)
                if (res.isSuccess) {
                    Toast.makeText(context, "Exported backup successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Local SAF Open Document Launcher (Import)
    val importLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = BackupRepository.importBackupFromLocalUri(context, uri)
                if (res.isSuccess) {
                    Toast.makeText(context, "All data restored successfully from backup", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && pendingRestoreInfo != null) {
        val info = pendingRestoreInfo!!
        Dialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
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
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(themePrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = themePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Restore Cloud Backup?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Found cloud backup:\n• Size: ${info.formattedSize}\n• Account: ${accountEmail ?: "Connected Account"}\n\nRestoring will update your syllabus, marks, and timer progress with this backup.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Cancel", color = Color.White)
                        }

                        Button(
                            onClick = {
                                showRestoreConfirmDialog = false
                                val currentEmail = accountEmail
                                if (!currentEmail.isNullOrBlank()) {
                                    scope.launch {
                                        val res = BackupRepository.performGoogleDriveRestore(context, currentEmail) {}
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Restored all syllabus and tracker data", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Restore failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                        ) {
                            Text("Restore", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Google Cloud Console Setup Guidance Dialog
    if (showApiConsoleGuideDialog) {
        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
        val sha1 = "A9:E3:51:E2:C4:30:8F:79:09:09:96:64:95:04:2E:96:A6:BF:47:37"
        val pkg = "com.marjuk.eaptracker"

        Dialog(
            onDismissRequest = { showApiConsoleGuideDialog = false },
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = themePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Drive Sync",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "To enable direct background sync, register this SHA-1 in your Google Cloud Console:\n\n• Package: $pkg\n• SHA-1: $sha1",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(sha1))
                            Toast.makeText(context, "SHA-1 copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy SHA-1 Fingerprint", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            showApiConsoleGuideDialog = false
                            shareBackupToDrive(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Google Drive App (1-Tap)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { showApiConsoleGuideDialog = false },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Close", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Auto-Backup Frequency Dialog (Weekly / Daily Schedule)
    if (showFreqDialog) {
        Dialog(
            onDismissRequest = { showFreqDialog = false },
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Backup Schedule",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        IconButton(
                            onClick = { showFreqDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    listOf("Daily", "Weekly", "Monthly", "Off").forEach { freq ->
                        val isSelected = autoFreq == freq
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) themePrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    BorderStroke(
                                        0.5.dp,
                                        if (isSelected) themePrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    HapticHelper.performHaptic(context, HapticType.LIGHT)
                                    BackupRepository.saveAutoBackupSettings(context, freq, wifiOnly)
                                    showFreqDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) themePrimary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = themePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Main Card (Frosted Glass with App Theme Accent)
    Box(
        modifier = modifier
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
                        listOf(themePrimary.copy(alpha = 0.4f), Color.White.copy(alpha = 0.08f))
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(themePrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Backup",
                        tint = themePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "GOOGLE DRIVE BACKUP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = themePrimary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Cloud Backup & Sync",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Securely back up your syllabus progress, exam scores, and study timer history to Google Drive. You can restore your data anytime.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last Backup Stats Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Backup:",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                        Text(
                            text = lastBackupTime,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }

                    if (lastBackupSize.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Size:",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                            )
                            Text(
                                text = lastBackupSize,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Account:",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                        Text(
                            text = accountEmail ?: "Not Connected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (accountEmail != null) themePrimary else Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Live Backup / Restore Progress text
            AnimatedVisibility(visible = isBackupRunning || isRestoreRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = themePrimary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = themePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Back Up & Restore Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. BACK UP NOW BUTTON
                Button(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.SUCCESS)
                        val currentEmail = accountEmail
                        if (currentEmail.isNullOrBlank()) {
                            launchAccountPicker()
                        } else {
                            scope.launch {
                                val res = BackupRepository.performGoogleDriveBackup(
                                    context = context,
                                    email = currentEmail,
                                    onConsentRequired = { intent ->
                                        consentLauncher.launch(intent)
                                    }
                                ) {}
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Backup uploaded to Google Drive", Toast.LENGTH_SHORT).show()
                                } else {
                                    val err = res.exceptionOrNull()?.localizedMessage ?: ""
                                    if (err.contains("UnregisteredOnApiConsole", ignoreCase = true) ||
                                        err.contains("authorization", ignoreCase = true) ||
                                        err.contains("ApiConsole", ignoreCase = true)) {
                                        showApiConsoleGuideDialog = true
                                    } else {
                                        Toast.makeText(context, "Drive sync: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isBackupRunning && !isRestoreRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Icon(
                        imageVector = if (accountEmail == null) Icons.Default.Login else Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (accountEmail == null) "Connect & Back Up" else "Back Up Now",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp
                    )
                }

                // 2. RESTORE BUTTON
                OutlinedButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        val currentEmail = accountEmail
                        if (currentEmail.isNullOrBlank()) {
                            launchAccountPicker()
                        } else {
                            scope.launch {
                                val info = BackupRepository.fetchLatestDriveBackupInfo(
                                    context = context,
                                    email = currentEmail,
                                    onConsentRequired = { intent ->
                                        consentLauncher.launch(intent)
                                    }
                                )
                                if (info != null) {
                                    pendingRestoreInfo = info
                                    showRestoreConfirmDialog = true
                                } else {
                                    Toast.makeText(context, "No cloud backup found for $currentEmail", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(0.9f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isBackupRunning && !isRestoreRunning,
                    border = BorderStroke(1.dp, themePrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themePrimary)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(14.dp))

            // Settings Rows
            // 1. Google Account Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { launchAccountPicker() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Google Account", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                    Text(text = accountEmail ?: "Tap to connect Google Account", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp))
                }
                Text(
                    text = if (accountEmail == null) "Connect" else "Switch",
                    style = MaterialTheme.typography.labelMedium.copy(color = themePrimary, fontWeight = FontWeight.Bold)
                )
            }

            // 2. Auto-Backup Frequency Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showFreqDialog = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Auto-Backup Schedule", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                    Text(text = "Frequency: $autoFreq", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp))
                }
                Text(
                    text = autoFreq,
                    style = MaterialTheme.typography.labelMedium.copy(color = themePrimary, fontWeight = FontWeight.Bold)
                )
            }

            // 3. Wi-Fi Only Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Back up over Wi-Fi only", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                    Text(text = "Save mobile data during backups", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp))
                }
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { BackupRepository.saveAutoBackupSettings(context, autoFreq, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = themePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Local File Backup Fallback (Offline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                        exportLocalLauncher.launch("EAPTracker_Backup_$timestamp.json")
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export", modifier = Modifier.size(15.dp), tint = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export File", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }

                OutlinedButton(
                    onClick = {
                        HapticHelper.performHaptic(context, HapticType.LIGHT)
                        importLocalLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import", modifier = Modifier.size(15.dp), tint = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import File", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/**
 * Direct 1-Tap Share to Google Drive or other apps via Android System Intent.
 */
fun shareBackupToDrive(context: Context) {
    try {
        val json = BackupRepository.createFullBackupJson(context)
        val file = java.io.File(context.cacheDir, "EAPTracker_Backup.json")
        file.writeText(json, java.nio.charset.StandardCharsets.UTF_8)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Save to Google Drive"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving backup: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
