package app.routes

import app.auth.requireUid
import app.models.BestDay
import app.models.StepGoalRequest
import app.models.StepGoalResponse
import app.models.StepHistoryItem
import app.models.StepHistorySummaryResponse
import app.models.StepSyncRequest
import app.services.FirebaseService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.round

private val firebaseService by lazy { FirebaseService() }
private const val MAX_RANGE_DAYS = 366

fun requireIsoDate(value: String) {
    try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Invalid ISO date: $value. Expected YYYY-MM-DD")
    }
}

@Suppress("CheckResult")
fun Route.stepRoutes() {

    route("/steps") {

        // 🔹 SYNC STEPS
        post("/sync") {
            val uid = call.requireUid() ?: return@post
            val request = call.receive<StepSyncRequest>()

            require(request.steps >= 0) { "steps must be >= 0" }
            LocalDate.parse(request.date)

            firebaseService.syncStepsToCloud(
                userId = uid,
                date = request.date,
                steps = request.steps
            )

            call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
        }

        // 🔹 SET GOAL
        post("/goal") {
            val uid = call.requireUid() ?: return@post
            val request = call.receive<StepGoalRequest>()
            require(request.dailyTarget > 0)
            firebaseService.setDailyGoal(uid, request.dailyTarget)

            call.respond(
                HttpStatusCode.OK,
                StepGoalResponse(uid, request.dailyTarget)
            )
        }

        // 🔹 GET GOAL
        get("/goal") {
            val uid = call.requireUid() ?: return@get
            val goal = firebaseService.getDailyGoal(uid)
            call.respond(HttpStatusCode.OK, StepGoalResponse(uid, goal))
        }

        // 🔹 GET HISTORY
        get("/history") {
            val uid = call.requireUid() ?: return@get

            val start = call.request.queryParameters["start"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "error", "message" to "Missing start date")
                )

            val end = call.request.queryParameters["end"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "error", "message" to "Missing end date")
                )

            val startDate = try {
                requireIsoDate(start)
                LocalDate.parse(start)
            } catch (e: Exception) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "error", "message" to "Invalid start date (expected YYYY-MM-DD)")
                )
            }

            val endDate = try {
                requireIsoDate(end)
                LocalDate.parse(end)
            } catch (e: Exception) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "error", "message" to "Invalid end date (expected YYYY-MM-DD)")
                )
            }

            if (endDate.isBefore(startDate)) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "error", "message" to "end must be >= start")
                )
            }

            val daysRequested =
                java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            if (daysRequested > MAX_RANGE_DAYS) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "error",
                        "message" to "Date range too large (max $MAX_RANGE_DAYS days)"
                    )
                )
            }

            val rawDocs = firebaseService.getHistory(uid, start, end)

            // Build map date -> steps, keeping MAX if duplicates appear
            val rawHistory: Map<String, Int> = buildMap {
                for (doc in rawDocs) {
                    val date = doc["date"]?.toString() ?: continue
                    val steps = (doc["steps"] as? Number)?.toInt() ?: 0
                    val prev = this[date] ?: 0
                    this[date] = maxOf(prev, steps)
                }
            }

            // Fill missing dates with 0 steps
            val filledHistory = mutableListOf<StepHistoryItem>()
            var current = startDate
            while (!current.isAfter(endDate)) {
                val dateStr = current.toString()
                val steps = rawHistory[dateStr] ?: 0
                filledHistory.add(StepHistoryItem(dateStr, steps))
                current = current.plusDays(1)
            }

            val total = filledHistory.sumOf { it.steps }

            // Average over ALL days (including zero-step days)
            val average = if (filledHistory.isNotEmpty()) {
                round(total.toDouble() / filledHistory.size * 100) / 100
            } else 0.0

            val daysWithData = filledHistory.count { it.steps > 0 }

            // Average over active days only
            val averageActiveOnly = if (daysWithData > 0) {
                round(total.toDouble() / daysWithData * 100) / 100
            } else 0.0

            val dailyTarget = firebaseService.getDailyGoal(uid)

            val goalReachedDays =
                if (dailyTarget > 0) filledHistory.count { it.steps >= dailyTarget } else 0

            val maxPossible = dailyTarget * filledHistory.size
            val goalCompletionPercentage =
                if (dailyTarget > 0 && maxPossible > 0) {
                    round((total.toDouble() / maxPossible.toDouble()) * 100.0 * 10) / 10
                } else 0.0

            val best = filledHistory.maxByOrNull { it.steps }
            val bestDay = best?.let { BestDay(it.date, it.steps) }

            // filledHistory is sorted ascending by date (filled via while loop).
            // asReversed() gives us most-recent-first — required for streak counting.
            // Do NOT sort or reorder filledHistory before this block.
            val currentStreak =
                if (dailyTarget > 0) {
                    var streak = 0
                    for (day in filledHistory.asReversed()) {
                        if (day.steps >= dailyTarget) streak++ else break
                    }
                    streak
                } else 0

            call.respond(
                HttpStatusCode.OK,
                StepHistorySummaryResponse(
                    userId = uid,
                    startDate = start,
                    endDate = end,
                    dailyTarget = dailyTarget,
                    data = filledHistory,
                    total = total,
                    average = average,
                    averageActiveOnly = averageActiveOnly,
                    daysWithData = daysWithData,
                    goalCompletionPercentage = goalCompletionPercentage,
                    goalReachedDays = goalReachedDays,
                    bestDay = bestDay,
                    currentStreak = currentStreak
                )
            )
        }
    }
}