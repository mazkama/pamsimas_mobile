package ahmat.dafa.pamsimas.model

import java.io.Serializable

data class KeluhanResponse(
    val success: Boolean,
    val message: String,
    val data: KeluhanPagination
)

data class KeluhanPagination(
    val current_page: Int,
    val data: List<Keluhan>,
    val last_page: Int,
    val next_page_url: String?,
    val prev_page_url: String?,
    val total: Int
)

data class Keluhan(
    val id_keluhan: Int,
    val id_users: Int,
    val nama_pelapor: String,
    val no_hp: String,
    val keterangan: String,
    val status: String,
    val foto_keluhan: String?,
    val tanggal: String,
    val tanggapan: String?
) : Serializable
