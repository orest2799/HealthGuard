package com.example.healthguard.domain.model

data class Medication(
    val id: String = "",
    val name: String = "",
    val dose: String = "",
    val time: String = "" // store as string like "08:00 AM"
)