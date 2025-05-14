package ahmat.dafa.pamsimas.model

data class PelangganBerandaResponse(
    val success: Boolean,
    val message: String,
    val data: DashboardData
)

data class DashboardData(
    val id_pelanggan: Int,
    val nama_pelanggan: String,
    val transaksi_terbaru: Transaksi?,
    val keluhan_terbaru: Keluhan?
)
