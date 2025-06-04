package ahmat.dafa.pamsimas.payment
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface XenditApiService {
    @POST("v2/invoices")
    suspend fun createInvoice(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: XenditInvoiceRequest
    ): Response<XenditInvoiceResponse>

    @GET("v2/invoices/{invoice_id}")
    suspend fun getInvoice(
        @Header("Authorization") authorization: String,
        @Path("invoice_id") invoiceId: String
    ): Response<XenditInvoiceResponse>
}
