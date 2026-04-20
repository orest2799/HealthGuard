package com.example.healthguard.data.network.pills

data class PillReminder(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val reminderTimes: List<ReminderTime>,
    val enabled: Boolean = true
)

data class ReminderTime(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int>
)


data class PillReminderPayload(
    val pillId: String,
    val medicineName: String,
    val dosage: String,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: Int
)

data class AlarmState(
    val pillId: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val medName: String = "",
    val dosage: String = "",
    val dayOfWeek: Int = 0
)



data class PillReminderDto(
    val id: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val enabled: Boolean = true,
    val reminderTimes: List<ReminderTimeDto> = emptyList()
)

data class ReminderTimeDto(
    val hour: Int = 0,
    val minute: Int = 0,
    val daysOfWeek: List<Int> = emptyList()
)
fun buildTimeKey(
    reminderId: String,
    hour: Int,
    minute: Int,

    ): String {
    return  "$reminderId-$hour-$minute"
}