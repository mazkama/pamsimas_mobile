package ahmat.dafa.pamsimas.petugas

import KategoriBiaya
import PemakaianBayarResponse
import TransaksiBayarRequest
import TransaksiBayarResponse
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ActivityLanjutBayarBinding
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.model.TransaksiResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import ahmat.dafa.pamsimas.petugas.adapter.KategoriAdapter
import ahmat.dafa.pamsimas.utils.CurrencyHelper.cleanCurrencyString
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import ahmat.dafa.pamsimas.utils.QRCodeHelper
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call

class LanjutBayarActivity : AppCompatActivity() {

    private lateinit var b: ActivityLanjutBayarBinding
    private lateinit var dialog: Dialog
    private lateinit var kategoriAdapter: KategoriAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val kategoriList = mutableListOf<KategoriBiaya>()
    private var dataPelanggan: Pemakaian? = null // bisa diakses di mana saja dalam activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLanjutBayarBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        dialog = Dialog(this).apply {
            setContentView(R.layout.progress_dialog)
            setCancelable(false)
        }

        // Tangani data pemakaian (utama)
        val dataPemakaian = intent.getSerializableExtra("data_pemakaian") as? PemakaianBayarResponse

        // Tangani EXTRA_TRANSAKSI jika ada
        val extraTransaksi = intent.getSerializableExtra("data_transaksi") as? Transaksi

        if (dataPemakaian != null) {
            // Tangani data pelanggan
            dataPelanggan = intent.getSerializableExtra("data_pelanggan") as? Pemakaian
            dataPelanggan?.let { pemakaian ->
                val qrBitmap = QRCodeHelper.generateQRCode(pemakaian.id_users.toString())
                qrBitmap?.let { b.qrImageView.setImageBitmap(it) }

                b.tvCustomerId.text = "ID: ${pemakaian.id_users}"
                b.tvCustomerName.text = "Nama: ${pemakaian.nama}"
                b.tvCustomerAddress.text = "Alamat: ${pemakaian.alamat}, RT ${pemakaian.rt} RW ${pemakaian.rw}"
            }
            setupPemakaianUI(dataPemakaian)
        } else if (extraTransaksi != null) {
            // Jika ada EXTRA_TRANSAKSI tapi tidak ada data_pemakaian
            setupTransaksiUI(extraTransaksi)
        } else {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        b.btnBack.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun setupPemakaianUI(pemakaian: PemakaianBayarResponse) {
        with(b) {
            tvInitialMeter.text = "${pemakaian.data.meter_awal} m³"
            tvFinalMeter.text = "${pemakaian.data.meter_akhir} m³"
            tvTotalUsage.text = "${pemakaian.data.jumlah_pemakaian} m³"
            tvTotalBill.text = "Rp ${formatCurrency(pemakaian.data.total_tagihan)}"
            tvBaseFee.text = "Rp ${formatCurrency(pemakaian.data.detail_biaya.beban.tarif)}"

            kategoriList.addAll(pemakaian.data.detail_biaya.kategori)
            kategoriAdapter = KategoriAdapter(kategoriList)
            rvKategori.layoutManager = LinearLayoutManager(this@LanjutBayarActivity)
            rvKategori.adapter = kategoriAdapter

            val idTransaksi = pemakaian.data.id_transaksi.toString()
            val totalBill = cleanCurrencyString(tvTotalBill.text.toString())

            setupTextWatcher(totalBill)
            setupPayButton(idTransaksi, totalBill)
        }
    }

    private fun setupTransaksiUI(transaksi: Transaksi) {
        with(b) {
            Log.d("setupTRansaksiUI", transaksi.toString())

            val qrBitmap = QRCodeHelper.generateQRCode(transaksi.id_pelanggan.toString())
            qrBitmap?.let { b.qrImageView.setImageBitmap(it) }

            b.tvCustomerId.text = "ID: ${transaksi.id_pelanggan}"
            b.tvCustomerName.text = "Nama: ${transaksi.nama_pelanggan}"
            b.tvCustomerAddress.text = "Alamat: ${transaksi.alamat_pelanggan}"
            tvInitialMeter.text = "${transaksi.meter_awal} m³"
            tvFinalMeter.text = "${transaksi.meter_akhir} m³"
            tvTotalUsage.text = "${transaksi.jumlah_pemakaian} m³"

            tvTotalBill.text = "Rp ${formatCurrency(transaksi.total_tagihan ?: 0)}"
            val baseFee = transaksi.detail_biaya.beban.tarif ?: 0
            tvBaseFee.text = "Rp ${formatCurrency(baseFee)}"

            kategoriList.addAll(transaksi.detail_biaya.kategori)
            kategoriAdapter = KategoriAdapter(kategoriList)
            rvKategori.layoutManager = LinearLayoutManager(this@LanjutBayarActivity)
            rvKategori.adapter = kategoriAdapter

            tvDenda.text = "Rp ${formatCurrency(transaksi.denda)}"


            val idTransaksi = transaksi.id_transaksi.toString()
            val totalBill = cleanCurrencyString(tvTotalBill.text.toString())

            // Setup watcher dan tombol bayar
            setupTextWatcher(totalBill)
            setupPayButton(idTransaksi, totalBill)
        }
    }


    private fun setupTextWatcher(totalBill: Int) {
        b.etAmountPaid.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val uangBayarText = s.toString().replace(".", "").trim()
                val uangBayar = uangBayarText.toIntOrNull()

                if (uangBayar != null && uangBayar >= totalBill) {
                    val kembalian = uangBayar - totalBill
                    b.tvChange.text = "Rp. ${formatCurrency(kembalian)}"
                } else {
                    b.tvChange.text = "Rp. 0"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupPayButton(idTransaksi: String, totalBill: Int) {
        if (idTransaksi.isEmpty()) {
            Toast.makeText(this, "ID Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        b.btnPay.setOnClickListener {
            val uangBayarText = b.etAmountPaid.text.toString().replace(".", "").trim()

            if (uangBayarText.isEmpty()) {
                Toast.makeText(this, "Masukkan jumlah uang bayar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uangBayar = uangBayarText.toIntOrNull()
            if (uangBayar == null) {
                Toast.makeText(this, "Format uang bayar tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uangBayar < totalBill) {
                Toast.makeText(this, "Jumlah uang bayar harus minimal Rp. ${formatCurrency(totalBill)}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bayar(idTransaksi, uangBayar)
        }
    }


    private fun bayar(idTransaksi: String, uangBayar: Int) {
        dialog.show() // Tampilkan loading

        val request = TransaksiBayarRequest(id_transaksi = idTransaksi,uang_bayar = uangBayar)
        val token = sharedPreferences.getString("token", null)

        // Panggil API dengan Retrofit
        ApiClient.getInstance(this,token).lunasPemakaian(request).enqueue(object : retrofit2.Callback<TransaksiBayarResponse> {
            override fun onResponse(call: Call<TransaksiBayarResponse>, response: retrofit2.Response<TransaksiBayarResponse>) {
                dialog.dismiss() // Sembunyikan loading
                if (response.isSuccessful) {

                    val body = response.body()

                    if (body != null && body.success) {
                        Toast.makeText(this@LanjutBayarActivity, "${body.message}", Toast.LENGTH_LONG).show()
                        // Intent ke LanjutBayarActivity
                        val intent = Intent(this@LanjutBayarActivity, CetakActivity::class.java).apply {
                            putExtra("data_transaksi", response.body()) // pastikan model PemakaianBayarResponse implements Parcelable atau Serializable
//                            putExtra("data_pelanggan", dataPelanggan)
                        }
                        startActivity(intent)
                        finish() // Kembali setelah sukses
                    } else {
                        Toast.makeText(this@LanjutBayarActivity, "Gagal: ${body?.message ?: "Unknown Error"}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Tambahkan log kalau response code selain 2xx
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LanjutBayarActivity, "Gagal: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<TransaksiBayarResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@LanjutBayarActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("BayarTransaksi", "Network Failure", t)
            }
        })
    }

    // Override onBackPressed untuk menangani back button ketika proses sedang berjalan
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
}
