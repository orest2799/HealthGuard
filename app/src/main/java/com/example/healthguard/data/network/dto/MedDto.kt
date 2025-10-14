package com.example.healthguard.data.network.dto

import com.squareup.moshi.Json


/* --- Records returned by /meds/search --- */
data class MedRecord(
    @Json(name = "source")   val source: String? = null,
    @Json(name = "id")       val id: String? = null,

    @Json(name = "generic")  val generic: String? = null,
    @Json(name = "brand")    val brand: String? = null,

    @Json(name = "strength") val strength: String? = null,
    @Json(name = "form")     val form: String? = null,
    @Json(name = "route")    val route: String? = null,

    @Json(name = "holder")   val holder: String? = null,
    @Json(name = "country")  val country: String? = null,
    @Json(name = "atc")      val atc: String? = null,

    @Json(name = "summary")  val summary: String? = null,
    @Json(name = "url")      val url: String? = null,

    @Json(name = "score")    val score: Double? = null
)

data class MedSearchResponse(
    @Json(name = "query")   val query: String,
    @Json(name = "lang")    val lang: String? = null,
    @Json(name = "results") val results: List<MedRecord> = emptyList()
)
