package com.marjuk.eaptracker.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticType {
    LIGHT,
    MEDIUM,
    STRONG,
    SUCCESS,
    SELECTION
}

object HapticHelper {

    fun performHaptic(context: Context, type: HapticType = HapticType.MEDIUM) {
        if (!SettingsRepository.hapticEnabled.value) return

        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) return

            val intensityMultiplier = when (SettingsRepository.hapticIntensity.value) {
                "Light" -> 0.5f
                "Strong" -> 1.5f
                else -> 1.0f // "Medium"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    HapticType.LIGHT -> {
                        val amplitude = (40 * intensityMultiplier).toInt().coerceIn(1, 255)
                        vibrator.vibrate(VibrationEffect.createOneShot(15, amplitude))
                    }
                    HapticType.MEDIUM -> {
                        val amplitude = (90 * intensityMultiplier).toInt().coerceIn(1, 255)
                        vibrator.vibrate(VibrationEffect.createOneShot(25, amplitude))
                    }
                    HapticType.STRONG -> {
                        val amplitude = (180 * intensityMultiplier).toInt().coerceIn(1, 255)
                        vibrator.vibrate(VibrationEffect.createOneShot(45, amplitude))
                    }
                    HapticType.SELECTION -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                        } else {
                            val amplitude = (60 * intensityMultiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(VibrationEffect.createOneShot(18, amplitude))
                        }
                    }
                    HapticType.SUCCESS -> {
                        val timings = longArrayOf(0, 30, 60, 45)
                        val amp1 = (70 * intensityMultiplier).toInt().coerceIn(1, 255)
                        val amp2 = (140 * intensityMultiplier).toInt().coerceIn(1, 255)
                        val amplitudes = intArrayOf(0, amp1, 0, amp2)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}
