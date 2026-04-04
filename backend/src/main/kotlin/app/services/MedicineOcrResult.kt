package app.services


data class MedicineOcrResult(
    val brand: String? = null,
    val activeSubstance: String? = null,
    val strength: String? = null,
    val form: String? = null
)
