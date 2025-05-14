import okhttp3.MultipartBody
import java.io.File
import java.io.Serializable

data class PemakaianBayarResponse(
    val success: Boolean,
    val message: String,
    val data: PemakaianBayarData
): Serializable

data class PemakaianBayarData(
    val id_pelanggan: Int,
    val id_transaksi: String,
    val nama_pelanggan: String,
    val meter_awal: Int,
    val meter_akhir: Int,
    val jumlah_pemakaian: Int,
    val detail_biaya: DetailBiaya,
    val total_tagihan: Int
) : Serializable

data class DetailBiaya(
    val beban: Beban,
    val kategori: List<KategoriBiaya>
) : Serializable

data class Beban(
    val id: Int,
    val tarif: Int
) : Serializable

data class KategoriBiaya(
    val id_kategori: Int,
    val batas_bawah: Int,
    val batas_atas: Int,
    val tarif: Int,
    val volume: Int,
    val subtotal: Int
) : Serializable

