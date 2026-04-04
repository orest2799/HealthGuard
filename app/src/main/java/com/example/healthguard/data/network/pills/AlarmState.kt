package com.example.healthguard.data.network.pills

data class AlarmState(
    val pillId: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val medName: String = "",
    val dosage: String = "",
    val dayOfWeek: Int = 0
)