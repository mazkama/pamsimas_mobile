package ahmat.dafa.pamsimas.model

data class ForgotPasswordResponse(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: Data?
) {
    data class Data(
        val username: String
    )
}

data class ForgotPasswordRequest(
    val no_hp: String
)
