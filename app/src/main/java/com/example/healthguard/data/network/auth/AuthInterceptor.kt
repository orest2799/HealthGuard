package com.example.healthguard.data.network.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class FirebaseAuthHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()


        val path = original.url.encodedPath
        if (path == "/health" || path == "/version") {
            return chain.proceed(original)
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) return chain.proceed(original)

        val token = try {
            val task = user.getIdToken(false)
            Tasks.await(task, 5, TimeUnit.SECONDS).token
        } catch (_: Exception) {
            null
        }

        val req = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(req)
    }
}
