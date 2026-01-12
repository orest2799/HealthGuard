package app.vision

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageContext
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
import kotlinx.coroutines.withTimeout

// ✅ Create once per JVM (recommended for Google clients)
private val visionClient: ImageAnnotatorClient by lazy {
    ImageAnnotatorClient.create()
}

fun Route.visionRoutes() {
    route("/vision") {
        post("/annotate") {
            // Always return JSON with at least "text"
            try {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null

                // --- Read multipart file ---
                try {
                    multipart.forEachPart { part ->
                        try {
                            if (part is PartData.FileItem && (part.name == "file" || part.name == "image")) {
                                imageBytes = withContext(Dispatchers.IO) {
                                    part.streamProvider().readBytes()
                                }
                            }
                        } finally {
                            part.dispose()
                        }
                    }
                } catch (t: Throwable) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "text" to "",
                            "error" to "Failed to read multipart data",
                            "message" to (t.message ?: "no message")
                        )
                    )
                    return@post
                }

                if (imageBytes == null || imageBytes!!.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("text" to "", "error" to "No file received")
                    )
                    return@post
                }

                // --- Build Vision request ---
                val img = Image.newBuilder()
                    .setContent(ByteString.copyFrom(imageBytes))
                    .build()

                val request = AnnotateImageRequest.newBuilder()
                    .setImage(img)
                    .addFeatures(
                        Feature.newBuilder()
                            .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                            .build()
                    )
                    .setImageContext(
                        ImageContext.newBuilder()
                            .addLanguageHints("el")
                            .addLanguageHints("en")
                            .build()
                    )
                    .build()

                // --- Call Vision with timeout ---
                val fullText: String = withTimeout(20_000) {
                    withContext(Dispatchers.IO) {
                        val resp = visionClient.batchAnnotateImages(listOf(request)).responsesList.firstOrNull()
                            ?: throw IllegalStateException("Empty Vision response")

                        if (resp.hasError()) {
                            throw IllegalStateException("Vision API error: ${resp.error.message}")
                        }

                        resp.fullTextAnnotation?.text.orEmpty()
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "text" to fullText
                    )
                )
            } catch (t: Throwable) {
                t.printStackTrace()
                // ✅ Never let the socket close without a JSON response
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "text" to "",
                        "error" to "Vision route failed",
                        "message" to (t.message ?: "no message")
                    )
                )
            }
        }
    }
}
