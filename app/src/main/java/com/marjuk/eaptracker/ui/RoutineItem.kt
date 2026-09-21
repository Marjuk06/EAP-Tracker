package com.marjuk.eaptracker.ui

data class RoutineItem(
    val date: String,
    val day: String,
    val classSubject: String?,
    val examDetails: Any?, // Can be String or List<String>
    val topics: List<String>? = null
)
