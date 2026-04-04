package com.example.healthguard.data.network.appointments

data class Appointment(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val timestamp: Long = 0L,
    val reminderOffsets: List<Long> = listOf(
        24 * 60 * 60 * 1000L, // 24h
        2 * 60 * 60 * 1000L   // 2h
    )
)