package ahmat.dafa.pamsimas.model

import android.R
import java.io.Serializable

data class PemakaianResponse(
    val success: Boolean,
    val message: String,
    val data: PemakaianPagination
)

data class PemakaianPagination(
    val current_page: Int,
    val data: List<Pemakaian>,
    val last_page: Int,
    val next_page_url: String?,
    val prev_page_url: String?,
    val total: Int
)

data class Pemakaian(
    val id_users: Int,
    val nama: String,
    val alamat: String,
    val rw: String,
    val rt: String,
    val no_hp: String,
    val jumlah_air: Int,
    val meter_akhir: Int,
    val waktu_catat: String?,
    val sudah_dicatat_bulan_ini: Boolean?
): Serializable
