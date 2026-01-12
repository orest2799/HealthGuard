package app.meds

/**
 * Query for searching medications
 */
data class MedQuery(
    val q: String,           // The search query string
    val lang: String? = null // Optional language hint (e.g., "en", "el")
)

/**
 * Record representing a medication with all its details
 * This matches the structure used by OpenFdaProvider and EmaProvider
 */
data class MedRecord(
    val source: String,                      // Provider source (e.g., "ema", "openfda", "galinos")
    val id: String? = null,                  // Unique identifier
    val brand: String? = null,               // Brand/trade name
    val generic: String? = null,             // Generic/active ingredient name
    val substances: List<String> = emptyList(), // Active substances
    val strength: String? = null,            // Dosage strength (e.g., "500mg")
    val form: String? = null,                // Pharmaceutical form (tablet, capsule, etc.)
    val route: String? = null,               // Route of administration (oral, IV, etc.)
    val holder: String? = null,              // Marketing authorization holder
    val country: String? = null,             // Country/region (e.g., "US", "EU")
    val atc: String? = null,                 // ATC classification code
    val summary: String? = null,             // Indications/usage summary
    val url: String? = null,                 // URL to detailed information
    val language: String? = null,            // Content language
    val score: Double? = null,               // Relevance score for search results
    val metadata: Map<String, Any?> = emptyMap()  // Additional data
)

/**
 * Response from medication search endpoint
 */

/**
 * Provider interface - implement this for each medication data source
 */
interface MedProvider {
    val source: String
    suspend fun search(q: MedQuery): List<MedRecord>
}