package ahmat.dafa.pamsimas.petugas

import KategoriBiaya
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ahmat.dafa.pamsimas.databinding.ActivityPelunasanBinding
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.petugas.adapter.KategoriAdapter
import ahmat.dafa.pamsimas.petugas.adapter.PaymentGatewayAdapter
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.content.Intent

class PelunasanAdapter : AppCompatActivity() {

    private lateinit var binding: ActivityPelunasanBinding
    private var transaksiData: Transaksi? = null
    private lateinit var kategoriAdapter: KategoriAdapter
    private val kategoriList = mutableListOf<KategoriBiaya>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPelunasanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Terima data transaksi dari intent
        transaksiData = intent.getSerializableExtra("data_transaksi") as? Transaksi

        if (transaksiData != null) {
            setupViews()
            setupClickListeners()
        } else {
            Toast.makeText(this, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViews() {
        transaksiData?.let { transaksi ->
            // Set data pencatatan
            binding.idPencatatan.text = transaksi.id_pemakaian.toString()
            // Asumsi tanggal pencatatan ada di transaksi, sesuaikan dengan field yang tersedia
            binding.tvPreviousDate.text = transaksi.tanggal_pencatatan ?: "Tidak tersedia"

            // Set data penggunaan air
            binding.tvInitialMeter.text = "${transaksi.meter_awal} m³"
            binding.tvFinalMeter.text = "${transaksi.meter_akhir} m³"
            binding.tvTotalUsage.text = "${transaksi.jumlah_pemakaian} m³"

            // Set total tagihan
            val totalTagihan = transaksi.total_tagihan ?: 0
            binding.tvTotalBill.text = "Rp ${formatCurrency(totalTagihan)}"

            // Set biaya beban
            val baseFee = transaksi.detail_biaya?.beban?.tarif ?: 0
            binding.tvBaseFee.text = "Rp ${formatCurrency(baseFee)}"

            // Setup RecyclerView untuk kategori - menggunakan pattern yang sama seperti LanjutBayarActivity
            transaksi.detail_biaya?.kategori?.let { kategoriFromTransaksi ->
                kategoriList.clear()
                kategoriList.addAll(kategoriFromTransaksi)
                kategoriAdapter = KategoriAdapter(kategoriList)
                binding.rvKategori.layoutManager = LinearLayoutManager(this@PelunasanAdapter)
                binding.rvKategori.adapter = kategoriAdapter
            }

            // Set biaya air dan admin
            val biayaAdmin = 4000 // Biaya admin tetap
            val totalDenganAdmin = totalTagihan + biayaAdmin

            binding.tvWaterFee.text = "Rp ${formatCurrency(totalTagihan)}"
            binding.tvAdminFee.text = "Rp ${formatCurrency(biayaAdmin)}"
            binding.tvTotalBill2.text = "Rp ${formatCurrency(totalDenganAdmin)}"
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Payment method selection (placeholder)
        // 1. Perbaikan kode onClick listener
        binding.btnSelectPaymentMethod.setOnClickListener {
            val intent = Intent(this, PaymentGatewayAdapter::class.java)
            startActivity(intent)
        }

        // Payment button
        binding.btnPay.setOnClickListener {
            processPayment()
        }
    }

    private fun processPayment() {
        transaksiData?.let { transaksi ->
            // Implementasi proses pembayaran
            Toast.makeText(this, "Belum", Toast.LENGTH_SHORT).show()

//            val idTransaksi = transaksi.id_transaksi.toString()
//            val totalBillText = binding.tvTotalBill2.text.toString()
//            val totalBill = cleanCurrencyString(totalBillText).toIntOrNull() ?: 0
//
//            // TODO: Implementasi logika pembayaran sesuai gateway yang digunakan
//            // Contoh: integrasi dengan Midtrans, Xendit, dll
//            // Gunakan idTransaksi dan totalBill untuk proses pembayaran
//
//            // Log untuk debugging
//            android.util.Log.d("PaymentGateway", "ID Transaksi: $idTransaksi, Total: $totalBill")
        }
    }

    // Override onBackPressed untuk konsistensi dengan LanjutBayarActivity
    override fun onBackPressed() {
        super.onBackPressed()
    }
}