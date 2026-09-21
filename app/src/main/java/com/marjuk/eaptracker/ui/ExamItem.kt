package com.marjuk.eaptracker.ui

data class ExamItem(
    val date: String,
    val day: String,
    val exams: List<String>,
    val syllabus: List<String>? = null
)
