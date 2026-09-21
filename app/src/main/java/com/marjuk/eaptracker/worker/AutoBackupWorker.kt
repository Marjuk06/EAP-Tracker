package com.marjuk.eaptracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marjuk.eaptracker.ui.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val email = BackupRepository.connectedAccountEmail.value
        val freq = BackupRepository.autoBackupFrequency.value
        
        if (email.isNullOrBlank() || freq == "Off") {
            return@withContext Result.success()
        }

        // চেক করবে এটি কি ডেইলি ব্যাকআপ নাকি ৫ মিনিটের চেঞ্জ ব্যাকআপ?
        val isPeriodic = inputData.getBoolean("is_periodic", false)

        try {
            val res = BackupRepository.performGoogleDriveBackup(
                context = applicationContext,
                email = email,
                showNotification = true,
                isManual = false,
                isPeriodic = isPeriodic // <-- নতুন প্যারামিটার
            ) {}

            if (res.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}