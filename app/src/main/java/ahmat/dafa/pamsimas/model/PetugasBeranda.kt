package ahmat.dafa.pamsimas.model

data class PetugasBerandaResponse(
    val success: Boolean,
    val message: String,
    val data: PetugasBerandaData
)

data class PetugasBerandaData(
    val id_petugas: Int,
    val jumlah_keluhan: Int,
    val total_harus_dicatat_bulan_ini: Int,
    val total_belum_dicatat_bulan_ini: Int,
    val total_uang_bulan_ini: Int,
    val transaksi_terbaru: Transaksi
)

