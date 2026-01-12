package app.meds

import kotlinx.serialization.Serializable

@Serializable
data class MedicineInfo(
    val brand: String? = null,
    val activeSubstance: String? = null,
    val strength: String? = null,
    val form: String? = null,
    val source: String? = null
)

@Serializable
data class MedSearchResponse(
    val brandQuery: String? = null,
    val substanceQuery: String? = null,
    val results: List<MedicineInfo> = emptyList()
)
