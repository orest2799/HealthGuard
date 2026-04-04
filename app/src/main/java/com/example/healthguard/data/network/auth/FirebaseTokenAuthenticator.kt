package com.example.healthguard.data.network.auth

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class FirebaseTokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val user = FirebaseAuth.getInstance().currentUser ?: return null

        val freshToken = try {
            val task = user.getIdToken(true) // force refresh
            Tasks.await(task, 5, TimeUnit.SECONDS).token
        } catch (_: Exception) {
            null
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $freshToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var res: Response? = response
        var count = 1
        while (res?.priorResponse != null) {
            count++
            res = res.priorResponse
        }
        return count
    }
}
