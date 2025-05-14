package ahmat.dafa.pamsimas.model

import DetailBiaya
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class TransaksiResponse(
    val success: Boolean,
    val message: String,
    val data: TransaksiPagination
) : Parcelable


data class TransaksiPagination(
    val current_page: Int,
    val data: List<Transaksi>,
    val last_page: Int,
    val next_page_url: String?,
    val prev_page_url: String?,
    val total: Int
) : Serializable

data class Transaksi(
    val id_transaksi: Int,
    val id_pemakaian: Int?,
    val id_pelanggan: Int?,
    val nama_pelanggan: String,
    val alamat_pelanggan: String,
    val tanggal_pencatatan: String?,
    val tanggal_pembayaran: String?,
    val meter_awal: Int?,
    val meter_akhir: Int?,
    val jumlah_pemakaian: Int?,
    val denda: Int?,
    val total_tagihan: Int?,
    val foto_meteran: String?,
    val status_pembayaran: String?,
    val detail_biaya: DetailBiaya,
) : Serializable
