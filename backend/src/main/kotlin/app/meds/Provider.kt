package app.meds


interface MedProvider {
    val source: String
    suspend fun search(q: MedQuery): List<MedRecord>
}
