package com.marjuk.eaptracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector

data class StudySection(
    val name: String,
    val icon: ImageVector,
    val isCompleted: Boolean = false
)

data class Chapter(
    val name: String,
    val sections: List<StudySection> = listOf(
        StudySection("Book", Icons.AutoMirrored.Filled.MenuBook),
        StudySection("Slide", Icons.Default.CastForEducation),
        StudySection("QB", Icons.Default.AccountBalance),
        StudySection("Concept", Icons.Default.Lightbulb)
    )
) {
    val isCompleted: Boolean get() = sections.all { it.isCompleted }
}

data class Paper(
    val name: String,
    val chapters: List<Chapter>
) {
    val completedChapters: Int get() = chapters.count { it.isCompleted }
    val totalChapters: Int get() = chapters.size
}

data class Subject(
    val name: String,
    val icon: ImageVector,
    val papers: List<Paper>
) {
    val totalChapters: Int get() = papers.sumOf { it.totalChapters }
    val completedChapters: Int get() = papers.sumOf { it.completedChapters }
    val progress: Float get() = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f
}
