import java.io.Serializable

data class TransaksiBayarRequest(
    val id_transaksi: String,
    val uang_bayar: Int
)

data class TransaksiBayarResponse(
    val success: Boolean,
    val message: String,
    val data: TransaksiBayarData
): Serializable

data class TransaksiBayarData(
    val id_transaksi: Int,
    val nama_petugas: String,
    val nama_pelanggan: String,
    val alamat_pelanggan: String,
    val tanggal_pencatatan: String,
    val tanggal_pembayaran: String,

    val meter_awal: Int,
    val meter_akhir: Int,
    val jumlah_pemakaian: Int,

    val denda: Int?,
    val detail_biaya: DetailBiaya,
    val total_tagihan: Int,
    val jumlah_bayar: Int,
    val kembalian: Int
): Serializable

data class BebanBiaya(
    val id: Int,
    val tarif: Int
): Serializable
