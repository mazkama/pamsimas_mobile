package ahmat.dafa.pamsimas.payment

data class XenditInvoiceRequest(
    val external_id: String,
    val amount: Int,
    val payer_email: String,
    val description: String,
    val invoice_duration: Int = 86400, // 24 jam dalam detik
    val customer: XenditCustomer? = null,
    val customer_notification_preference: XenditNotificationPreference? = null,
    val success_redirect_url: String? = null,
    val failure_redirect_url: String? = null,
    val currency: String = "IDR",
    val items: List<XenditItem>? = null,
    val fees: List<XenditFee>? = null
)

data class XenditCustomer(
    val given_names: String,
    val surname: String? = null,
    val email: String,
    val mobile_number: String? = null,
    val addresses: List<XenditAddress>? = null
)

data class XenditAddress(
    val city: String,
    val country: String,
    val postal_code: String,
    val state: String,
    val street_line1: String,
    val street_line2: String? = null
)

data class XenditNotificationPreference(
    val invoice_created: List<String> = listOf("email"),
    val invoice_reminder: List<String> = listOf("email"),
    val invoice_paid: List<String> = listOf("email"),
    val invoice_expired: List<String> = listOf("email")
)

data class XenditItem(
    val name: String,
    val quantity: Int,
    val price: Int,
    val category: String? = null,
    val url: String? = null
)

data class XenditFee(
    val type: String,
    val value: Int
)

data class XenditInvoiceResponse(
    val id: String,
    val user_id: String,
    val external_id: String,
    val status: String,
    val merchant_name: String,
    val amount: Int,
    val payer_email: String,
    val description: String,
    val invoice_url: String,
    val expiry_date: String,
    val created: String,
    val updated: String,
    val currency: String
)