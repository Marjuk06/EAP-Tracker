package com.marjuk.eaptracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : NavigationItem("home", "Home", Icons.Default.Dashboard)
    object Syllabus : NavigationItem("syllabus", "Syllabus", Icons.AutoMirrored.Filled.ListAlt)
    object Exams : NavigationItem("exams", "Exams", Icons.AutoMirrored.Filled.Assignment)
    object Progress : NavigationItem("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp)
    object Profile : NavigationItem("profile", "Profile", Icons.Default.Person)
    object FullRoutine : NavigationItem("full_routine/{type}", "Full Routine", Icons.AutoMirrored.Filled.Assignment)
}

val bottomNavItems = listOf(
    NavigationItem.Home,
    NavigationItem.Syllabus,
    NavigationItem.Exams,
    NavigationItem.Progress
)
