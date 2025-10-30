package app.db

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

object DB {
    val firestore: Firestore by lazy {
        val projectId = System.getenv("GCP_PROJECT_ID") ?: "healthguard-b443f"
        FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId(projectId)
            .build()
            .service
    }
}
