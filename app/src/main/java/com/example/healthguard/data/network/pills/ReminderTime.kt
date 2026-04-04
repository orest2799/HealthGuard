package com.example.healthguard.data.network.pills

data class ReminderTime(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int>
)