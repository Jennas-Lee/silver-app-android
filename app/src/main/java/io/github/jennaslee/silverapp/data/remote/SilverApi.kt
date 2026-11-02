package io.github.jennaslee.silverapp.data.remote

import io.github.jennaslee.silverapp.BuildConfig
import io.github.jennaslee.silverapp.session.SessionStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 백엔드 API. 명세: silver-app-backend/docs/api/v1.md */
interface SilverApi {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("api/v1/messages/classify")
    suspend fun classifyMessage(@Body request: ClassifyRequest): ClassifyResponse

    @POST("api/v1/kiosk/guide")
    suspend fun kioskGuide(@Body request: KioskGuideRequest): KioskGuideResponse

    // 계정

    @POST("api/v1/auth/senior")
    suspend fun startSenior(@Body request: SeniorStartRequest): AuthResponse

    @POST("api/v1/auth/guardian/signup")
    suspend fun guardianSignup(@Body request: GuardianSignupRequest): AuthResponse

    @POST("api/v1/auth/guardian/login")
    suspend fun guardianLogin(@Body request: GuardianLoginRequest): AuthResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/me")
    suspend fun me(): MeResponse

    // 연결

    @POST("api/v1/links/code")
    suspend fun linkCode(): LinkCodeResponse

    @POST("api/v1/links")
    suspend fun link(@Body request: LinkRequest): LinkResponse

    @GET("api/v1/links")
    suspend fun links(): LinksResponse

    @DELETE("api/v1/links/{userId}")
    suspend fun unlink(@Path("userId") userId: String): Response<Unit>

    // 차단 기록

    @POST("api/v1/records")
    suspend fun uploadRecords(@Body request: UploadRecordsRequest): UploadRecordsResponse

    @DELETE("api/v1/records/{id}")
    suspend fun deleteRecord(@Path("id") id: String): Response<Unit>

    @GET("api/v1/seniors/{id}/records")
    suspend fun seniorRecords(
        @Path("id") seniorId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null,
    ): RecordsResponse
}

@Serializable
data class KioskGuideRequest(
    val sessionId: String? = null,
    val goal: String,
    /** base64 JPEG, 긴 변 1024px 이하 */
    val image: String,
    val mediaType: String = "image/jpeg",
    val history: List<KioskHistoryItem> = emptyList(),
    val userReply: String? = null,
)

@Serializable
data class KioskHistoryItem(val speech: String)

@Serializable
data class KioskGuideResponse(
    val sessionId: String,
    val speech: String,
    /** in_progress | done | need_input | unclear */
    val status: String,
    val target: KioskTarget? = null,
)

@Serializable
data class KioskTarget(
    val label: String,
    /** [x1, y1, x2, y2], 사진 크기 대비 0~1 */
    val box: List<Float>,
)

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

// 계정

@Serializable
data class UserDto(
    val id: String,
    /** senior | guardian */
    val role: String,
    val name: String,
    val email: String? = null,
)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class SeniorStartRequest(val name: String)

@Serializable
data class GuardianSignupRequest(val name: String, val email: String, val password: String)

@Serializable
data class GuardianLoginRequest(val email: String, val password: String)

@Serializable
data class MeResponse(val user: UserDto)

// 연결

@Serializable
data class LinkCodeResponse(val code: String, val expiresAt: String)

@Serializable
data class LinkRequest(val code: String)

@Serializable
data class LinkResponse(val senior: UserDto)

@Serializable
data class LinkedSenior(
    val id: String,
    val name: String,
    val linkedAt: String,
    /** 최근 7일 차단·경고 수 */
    val recentCount: Int = 0,
    val lastRecordAt: String? = null,
)

@Serializable
data class LinkedGuardian(
    val id: String,
    val name: String,
    val email: String? = null,
    val linkedAt: String,
)

/** 보호자는 seniors, 어르신은 guardians만 채워져 온다 */
@Serializable
data class LinksResponse(
    val seniors: List<LinkedSenior>? = null,
    val guardians: List<LinkedGuardian>? = null,
)

// 차단 기록

@Serializable
data class RecordDto(
    val id: String,
    /** ISO 시각 */
    val receivedAt: String,
    val source: String,
    val sender: String,
    /** 개인정보를 가린 본문 */
    val body: String,
    /** HIDDEN | WARNED */
    val action: String,
    val category: String,
    val reason: String,
)

@Serializable
data class UploadRecordsRequest(val records: List<RecordDto>)

@Serializable
data class UploadRecordsResponse(val received: Int, val saved: Int)

@Serializable
data class RecordsResponse(val records: List<RecordDto>)

object ApiClient {
    val json = Json { ignoreUnknownKeys = true }

    val api: SilverApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // 로그인했으면 계정 토큰, 아니면 예전 방식의 기기 토큰
                val token = SessionStore.current?.token
                val request = chain.request().newBuilder().apply {
                    if (token != null) header("Authorization", "Bearer $token") else header("X-Device-Token", BuildConfig.DEVICE_TOKEN)
                }.build()
                chain.proceed(request)
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

    /** 실패한 요청을 화면에 보여줄 쉬운 문장으로 바꾼다 (서버가 준 메시지를 우선 사용) */
    fun errorMessage(e: Throwable): String = when (e) {
        is HttpException -> runCatching {
            json.parseToJsonElement(e.response()?.errorBody()?.string().orEmpty())
                .jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
        }.getOrElse { "잠시 문제가 생겼어요. 다시 해 주세요." }
        is IOException -> "인터넷에 연결되어 있는지 확인해 주세요."
        else -> "잠시 문제가 생겼어요. 다시 해 주세요."
    }
}
