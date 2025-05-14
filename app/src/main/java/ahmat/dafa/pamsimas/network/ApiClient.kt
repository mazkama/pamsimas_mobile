package ahmat.dafa.pamsimas.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://dev.airtenggerlor.biz.id/api/"

    fun getInstance(context: Context, token: String?): ApiService {
        val clientBuilder = OkHttpClient.Builder()

        // Menambahkan waktu timeout
        clientBuilder.connectTimeout(30, TimeUnit.SECONDS)  // Timeout untuk koneksi
        clientBuilder.readTimeout(30, TimeUnit.SECONDS)     // Timeout untuk membaca data
        clientBuilder.writeTimeout(30, TimeUnit.SECONDS)    // Timeout untuk menulis data

        // Tambahkan interceptor jika ada token
        if (!token.isNullOrEmpty()) {
            clientBuilder.addInterceptor(AuthInterceptor(context, token))
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
