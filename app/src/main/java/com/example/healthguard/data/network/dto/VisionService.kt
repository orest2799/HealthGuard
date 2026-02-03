import com.example.healthguard.data.models.MedicineOcrResult //
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface VisionService {
    @POST("ocr") // Matches backend route
    suspend fun scanMedicine(@Body image: RequestBody): MedicineOcrResult //
}