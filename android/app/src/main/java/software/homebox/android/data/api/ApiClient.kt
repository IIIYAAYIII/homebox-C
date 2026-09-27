package software.homebox.android.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.homebox.android.data.prefs.AppPreferences
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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

    // Build OkHttpClient supporting self-signed certificates for self-hosted instances
    private val okHttpClient: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }

    /**
     * Clean and normalize raw user server input:
     * - Trims whitespaces and quotes
     * - Adds http:// if no protocol is given
     * - Strips accidental /api or /api/v1 suffixes
     * - Ensures a single trailing slash
     */
    fun cleanUrl(rawUrl: String): String {
        var url = rawUrl.trim().trim('"', '\'')
        if (url.isBlank()) return "http://127.0.0.1:7745/"

        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }

        url = url.trimEnd('/')

        if (url.endsWith("/api/v1", ignoreCase = true)) {
            url = url.substring(0, url.length - 7)
        } else if (url.endsWith("/api", ignoreCase = true)) {
            url = url.substring(0, url.length - 4)
        }

        return "$url/"
    }

    @Synchronized
    fun getService(overrideBaseUrl: String? = null): HomeboxApiService {
        val targetUrl = cleanUrl(overrideBaseUrl ?: AppPreferences.serverUrl)

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

    fun getAttachmentUrl(entityId: String, attachmentId: String): String {
        val base = cleanUrl(AppPreferences.serverUrl).trimEnd('/')
        val token = AppPreferences.authToken
        return if (token.isNotBlank()) {
            "$base/api/v1/entities/$entityId/attachments/$attachmentId?token=$token"
        } else {
            "$base/api/v1/entities/$entityId/attachments/$attachmentId"
        }
    }

    fun getAttachmentUrl(attachmentId: String): String {
        val base = cleanUrl(AppPreferences.serverUrl).trimEnd('/')
        val token = AppPreferences.authToken
        return if (token.isNotBlank()) {
            "$base/api/v1/attachments/$attachmentId?token=$token"
        } else {
            "$base/api/v1/attachments/$attachmentId"
        }
    }
}
