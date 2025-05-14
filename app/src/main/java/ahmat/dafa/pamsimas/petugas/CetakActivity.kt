package ahmat.dafa.pamsimas.petugas

import KategoriBiaya
import TransaksiBayarResponse
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ActivityCetakBinding
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.petugas.adapter.KategoriAdapter
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.app.Activity
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager

class CetakActivity  : AppCompatActivity() {

    private lateinit var b: ActivityCetakBinding
    private lateinit var dialog: Dialog
    private lateinit var kategoriAdapter: KategoriAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val kategoriList = mutableListOf<KategoriBiaya>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCetakBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        dialog = Dialog(this)
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        val dataTransaksi = intent.getSerializableExtra("data_transaksi") as? TransaksiBayarResponse
        val dataPelanggan = intent.getSerializableExtra("data_pelanggan") as? Pemakaian

//        dataPelanggan?.let { pemakaian ->
//            b.tvCustomerName.text = "${pemakaian.nama}"
//            b.tvCustomerAddress.text = "${pemakaian.alamat}, RT ${pemakaian.rt} RW ${pemakaian.rw}" ?: "${pemakaian.alamat}"
//        }

        dataTransaksi?.let { pemakaian ->
            b.tvCustomerName.text = "${pemakaian.data.nama_pelanggan}"
            b.tvCustomerAddress.text = "${pemakaian.data.alamat_pelanggan}"
            b.tvTransactionId.text = "${pemakaian.data.id_transaksi}"
            b.tvOfficerName.text = "${pemakaian.data.nama_petugas}"
            b.tvRecordDate.text = "${pemakaian.data.tanggal_pencatatan}"
            b.tvPaymentDate.text = "${pemakaian.data.tanggal_pembayaran}"

            b.tvReceiptDateTime.text = "${pemakaian.data.tanggal_pembayaran}"

            b.tvInitialMeter.text = "${pemakaian.data.meter_awal} m³"
            b.tvFinalMeter.text = "${pemakaian.data.meter_akhir} m³"
            b.tvTotalUsage.text = "${pemakaian.data.jumlah_pemakaian} m³"

            b.tvTotalBill.text = "Rp ${formatCurrency(pemakaian.data.total_tagihan)}"
            b.tvBaseFee.text = "Rp ${formatCurrency(pemakaian.data.detail_biaya.beban.tarif)}"

            kategoriList.addAll(pemakaian.data.detail_biaya.kategori)
            kategoriAdapter = KategoriAdapter(kategoriList)
            b.rvKategori.layoutManager = LinearLayoutManager(this)
            b.rvKategori.adapter = kategoriAdapter

            b.tvAmountPaid.text= "Rp ${formatCurrency(pemakaian.data.jumlah_bayar)}"
            b.tvChange.text = "Rp ${formatCurrency(pemakaian.data.kembalian)}"
        }

        b.btnClose.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

    }
    // Override onBackPressed untuk menangani back button ketika proses sedang berjalan
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }

}