package ahmat.dafa.pamsimas.model

import java.io.File

data class PemakaianStoreRequest(
    val id_users: String,
    val meter_awal: String,
    val meter_akhir: String,
    val foto_meteran: File?
)

data class PemakaianStoreResponse(
    val success: Boolean,
    val message: String
)