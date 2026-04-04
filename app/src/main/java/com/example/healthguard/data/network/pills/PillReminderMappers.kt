package com.example.healthguard.data.network.pills


fun PillReminder.toDto(): PillReminderDto {
    return PillReminderDto(
        id = id,
        medicineName = medicineName,
        dosage = dosage,
        enabled = enabled,
        reminderTimes = reminderTimes.map {
            ReminderTimeDto(
                hour = it.hour,
                minute = it.minute,
                daysOfWeek = it.daysOfWeek
            )
        }
    )
}

fun PillReminderDto.toModel(): PillReminder {
    return PillReminder(
        id = id,
        medicineName = medicineName,
        dosage = dosage,
        enabled = enabled,
        reminderTimes = reminderTimes.map {
            ReminderTime(
                hour = it.hour,
                minute = it.minute,
                daysOfWeek = it.daysOfWeek
            )
        }
    )
}