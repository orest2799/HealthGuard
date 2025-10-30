package app.vision

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.protobuf.ByteString
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.visionRoutes() {
    route("/vision") {

        // POST /vision/annotate  (multipart; field name: "file")
        post("/annotate") {
            val multipart = call.receiveMultipart()

            var imageBytes: ByteArray? = null
            try {
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            // Read the file bytes off the blocking InputStream on IO dispatcher
                            imageBytes = withContext(Dispatchers.IO) {
                                part.streamProvider().readBytes()
                            }
                        }
                        else -> Unit
                    }
                    part.dispose()
                }
            } catch (t: Throwable) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Failed to read multipart data: ${t.message}")
                )
                return@post
            }

            if (imageBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "No file received")
                )
                return@post
            }

            // ---- Google Cloud Vision OCR ----
            val img = Image.newBuilder()
                .setContent(ByteString.copyFrom(imageBytes))
                .build()

            // TEXT_DETECTION works well for labels; use DOCUMENT_TEXT_DETECTION for documents
            val feature = Feature.newBuilder()
                .setType(Feature.Type.TEXT_DETECTION)
                .build()

            val request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(img)
                .build()

            val fullText: String
            val words = mutableListOf<String>()

            try {
                withContext(Dispatchers.IO) {
                    ImageAnnotatorClient.create().use { client ->
                        val resp = client.batchAnnotateImages(listOf(request)).responsesList.first()

                        if (resp.hasError()) {
                            throw IllegalStateException(resp.error.message)
                        }

                        // Full page text
                        fullText = resp.fullTextAnnotation?.text ?: ""

                        // Individual tokens (skip the first which repeats full text)
                        if (resp.textAnnotationsCount > 1) {
                            resp.textAnnotationsList.drop(1).forEach { words += it.description }
                        }
                    }
                }
            } catch (t: Throwable) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Vision API error: ${t.message}")
                )
                return@post
            }

            // Response shape your Android client (VisionDto) expects
            call.respond(
                mapOf(
                    "text" to fullText,      // primary field your app shows
                    "fullText" to fullText,  // backup
                    "words" to words         // optional tokens
                )
            )
        }
    }
}


