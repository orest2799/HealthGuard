package com.example.healthguard.data.network.pills

fun buildTimeKey(
    reminderId: String,
    hour: Int,
    minute: Int,

): String {
    return  "$reminderId-$hour-$minute"
}