package app.vision

import com.google.cloud.vision.v1.ImageAnnotatorClient

object VisionClient {
    val client: ImageAnnotatorClient by lazy {
        try {
            ImageAnnotatorClient.create()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to create Vision client", e)
        }
    }
}
