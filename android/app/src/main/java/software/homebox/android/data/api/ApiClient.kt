package software.homebox.android.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.homebox.android.data.prefs.AppPreferences
import java.util.concurrent.TimeUnit

object ApiClient {

    private var currentBaseUrl: String = ""
    private var apiService: HomeboxApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        val token = AppPreferences.authToken
        if (token.isNotBlank()) {
            val headerVal = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            builder.header("Authorization", headerVal)
        }

        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Synchronized
    fun getService(overrideBaseUrl: String? = null): HomeboxApiService {
        val targetUrl = (overrideBaseUrl ?: AppPreferences.serverUrl).let {
            if (it.isBlank()) "http://127.0.0.1:7745/"
            else if (!it.endsWith("/")) "$it/"
            else it
        }

        if (apiService == null || currentBaseUrl != targetUrl) {
            currentBaseUrl = targetUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(targetUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(HomeboxApiService::class.java)
        }

        return apiService!!
    }

    fun getAttachmentUrl(attachmentId: String): String {
        val base = AppPreferences.serverUrl.trimEnd('/')
        return "$base/api/v1/attachments/$attachmentId"
    }
}
