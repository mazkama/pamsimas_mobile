package ahmat.dafa.pamsimas.payment

import android.util.Base64
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*

class XenditHelper {

    companion object {
        // Ganti dengan Secret Key Anda dari Xendit Dashboard
        private const val SECRET_KEY = "xnd_development_YOUR_SECRET_KEY_HERE"
        private const val BASE_URL = "https://api.xendit.co/"

        fun getAuthorizationHeader(): String {
            val credentials = "$SECRET_KEY:"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            return "Basic $encoded"
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(XenditApiService::class.java)

    suspend fun createInvoice(
        externalId: String,
        amount: Int,
        payerEmail: String,
        description: String,
        customerName: String,
        customerPhone: String? = null,
        items: List<XenditItem>? = null,
        successUrl: String? = null,
        failureUrl: String? = null
    ): Result<XenditInvoiceResponse> {
        return try {
            val nameParts = customerName.split(" ", limit = 2)
            val firstName = nameParts.firstOrNull() ?: customerName
            val lastName = nameParts.getOrNull(1)

            val request = XenditInvoiceRequest(
                external_id = externalId,
                amount = amount,
                payer_email = payerEmail,
                description = description,
                customer = XenditCustomer(
                    given_names = firstName,
                    surname = lastName,
                    email = payerEmail,
                    mobile_number = customerPhone
                ),
                items = items,
                success_redirect_url = successUrl,
                failure_redirect_url = failureUrl,
                customer_notification_preference = XenditNotificationPreference()
            )

            val response = apiService.createInvoice(
                authorization = getAuthorizationHeader(),
                request = request
            )

            if (response.isSuccessful) {
                val invoice = response.body()
                if (invoice != null) {
                    Result.success(invoice)
                } else {
                    Result.failure(Exception("Invoice response tidak ditemukan"))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvoiceStatus(invoiceId: String): Result<XenditInvoiceResponse> {
        return try {
            val response = apiService.getInvoice(
                authorization = getAuthorizationHeader(),
                invoiceId = invoiceId
            )

            if (response.isSuccessful) {
                val invoice = response.body()
                if (invoice != null) {
                    Result.success(invoice)
                } else {
                    Result.failure(Exception("Invoice tidak ditemukan"))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}