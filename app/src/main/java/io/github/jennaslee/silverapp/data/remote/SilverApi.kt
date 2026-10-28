package io.github.jennaslee.silverapp.data.remote

import io.github.jennaslee.silverapp.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/** 백엔드 API. 명세: silver-app-backend/docs/api/v1.md */
interface SilverApi {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("api/v1/messages/classify")
    suspend fun classifyMessage(@Body request: ClassifyRequest): ClassifyResponse
}

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val time: String,
)

@Serializable
data class ClassifyRequest(
    val source: String,
    val sender: String,
    val senderInContacts: Boolean,
    val body: String,
    val ruleHits: List<String>,
)

@Serializable
data class ClassifyResponse(
    /** safe | suspicious | malicious */
    val verdict: String,
    val category: String,
    val reason: String,
)

object ApiClient {
    val json = Json { ignoreUnknownKeys = true }

    val api: SilverApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-Device-Token", BuildConfig.DEVICE_TOKEN)
                        .build(),
                )
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            // 키오스크 안내(이미지 + LLM)는 응답이 수 초 걸릴 수 있다
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SilverApi::class.java)
    }
}
