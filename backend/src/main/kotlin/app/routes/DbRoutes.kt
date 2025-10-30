package app.routes

import app.db.MedExtractionRepo
import app.db.OcrRepo
import app.meds.MedRecord
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** Simple DTOs for the API */
data class SaveOcrBody(val userId: String, val text: String)
data class SaveMedsBody(val userId: String, val ocrId: String, val meds: List<MedRecord>)

fun Route.dbRoutes() {

    // POST /api/ocr/save  { "userId": "...", "text": "OCR text..." }
    post("/api/ocr/save") {
        val body = call.receive<SaveOcrBody>()
        val id = OcrRepo.save(body.userId, body.text)
        call.respond(mapOf("id" to id))
    }

    // POST /api/meds/save  { "userId":"...", "ocrId":"...", "meds":[ MedRecord, ... ] }
    post("/api/meds/save") {
        val body = call.receive<SaveMedsBody>()
        val id = MedExtractionRepo.save(body.userId, body.ocrId, body.meds)
        call.respond(mapOf("id" to id))
    }
}