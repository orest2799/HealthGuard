package com.example.healthguard

import android.app.Application
import com.google.firebase.FirebaseApp



class HealthAppAplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}