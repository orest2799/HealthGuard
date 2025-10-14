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

fun Route.visionRoutes() {
    route("/vision") {

        // POST /vision/annotate  (multipart; field name: "file")
        post("/annotate") {
            val multipart = call.receiveMultipart()

            var imageBytes: ByteArray? = null
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        // Simple + reliable: read InputStream into bytes
                        imageBytes = part.streamProvider().readBytes()
                    }
                    else -> Unit
                }
                part.dispose()
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

            val feature = Feature.newBuilder()
                .setType(Feature.Type.TEXT_DETECTION)
                .build()

            val request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(img)
                .build()

            var fullText = ""
            val words = mutableListOf<String>()

            ImageAnnotatorClient.create().use { client ->
                val resp = client.batchAnnotateImages(listOf(request)).responsesList.first()

                if (resp.hasError()) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to resp.error.message)
                    )
                    return@post
                }

                fullText = resp.fullTextAnnotation?.text ?: ""

                // Individual tokens (skip the first, which repeats full text)
                if (resp.textAnnotationsCount > 1) {
                    resp.textAnnotationsList.drop(1).forEach { words += it.description }
                }
            }

            // Keys expected by your Android client (VisionDto)
            call.respond(
                mapOf(
                    "text" to fullText,      // primary
                    "fullText" to fullText,  // backup
                    "words" to words         // tokens
                )
            )
        }
    }
}



