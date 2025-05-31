//package ahmat.dafa.pamsimas.petugas
//
//import KategoriBiaya
//import TransaksiBayarResponse
//import ahmat.dafa.pamsimas.R
//import ahmat.dafa.pamsimas.databinding.ActivityCetakBinding
//import ahmat.dafa.pamsimas.model.Pemakaian
//import ahmat.dafa.pamsimas.petugas.adapter.KategoriAdapter
//import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
//import android.app.Activity
//import android.app.Dialog
//import android.content.Intent
//import android.content.SharedPreferences
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.FileProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import java.io.File
//import java.io.FileOutputStream
//
//class CetakActivity  : AppCompatActivity() {
//
//    private lateinit var b: ActivityCetakBinding
//    private lateinit var dialog: Dialog
//    private lateinit var kategoriAdapter: KategoriAdapter
//    private lateinit var sharedPreferences: SharedPreferences
//    private val kategoriList = mutableListOf<KategoriBiaya>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        b = ActivityCetakBinding.inflate(layoutInflater)
//        setContentView(b.root)
//
//        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
//
//        dialog = Dialog(this)
//        dialog.setContentView(R.layout.progress_dialog)
//        dialog.setCancelable(false)
//
//        val dataTransaksi = intent.getSerializableExtra("data_transaksi") as? TransaksiBayarResponse
//        val dataPelanggan = intent.getSerializableExtra("data_pelanggan") as? Pemakaian
//
////        dataPelanggan?.let { pemakaian ->
////            b.tvCustomerName.text = "${pemakaian.nama}"
////            b.tvCustomerAddress.text = "${pemakaian.alamat}, RT ${pemakaian.rt} RW ${pemakaian.rw}" ?: "${pemakaian.alamat}"
////        }
//
//        dataTransaksi?.let { pemakaian ->
//            b.tvCustomerName.text = "${pemakaian.data.nama_pelanggan}"
//            b.tvCustomerAddress.text = "${pemakaian.data.alamat_pelanggan}"
//            b.tvTransactionId.text = "${pemakaian.data.id_transaksi}"
//            b.tvOfficerName.text = "${pemakaian.data.nama_petugas}"
//            b.tvRecordDate.text = "${pemakaian.data.tanggal_pencatatan}"
//            b.tvPaymentDate.text = "${pemakaian.data.tanggal_pembayaran}"
//
//            b.tvReceiptDateTime.text = "${pemakaian.data.tanggal_pembayaran}"
//
//            b.tvInitialMeter.text = "${pemakaian.data.meter_awal} m³"
//            b.tvFinalMeter.text = "${pemakaian.data.meter_akhir} m³"
//            b.tvTotalUsage.text = "${pemakaian.data.jumlah_pemakaian} m³"
//
//            b.tvTotalBill.text = "Rp ${formatCurrency(pemakaian.data.total_tagihan)}"
//            b.tvBaseFee.text = "Rp ${formatCurrency(pemakaian.data.detail_biaya.beban.tarif)}"
//
//            kategoriList.addAll(pemakaian.data.detail_biaya.kategori)
//            kategoriAdapter = KategoriAdapter(kategoriList)
//            b.rvKategori.layoutManager = LinearLayoutManager(this)
//            b.rvKategori.adapter = kategoriAdapter
//
//            b.tvAmountPaid.text= "Rp ${formatCurrency(pemakaian.data.jumlah_bayar)}"
//            b.tvChange.text = "Rp ${formatCurrency(pemakaian.data.kembalian)}"
//        }
//
//        b.btnShare.setOnClickListener {
//            shareStrukLayout()
//        }
//
//        b.btnClose.setOnClickListener {
//            setResult(Activity.RESULT_OK)
//            finish()
//        }
//
//    }
//
//    private fun getBitmapFromView(view: View): Bitmap {
//        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
//        return bitmap
//    }
//
//    private fun saveBitmapToFile(bitmap: Bitmap): File {
//        val file = File(getExternalFilesDir(null), "struk_${System.currentTimeMillis()}.png")
//        val outputStream = FileOutputStream(file)
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//        outputStream.flush()
//        outputStream.close()
//        return file
//    }
//
//    private fun shareImageFile(file: File) {
//        val uri = FileProvider.getUriForFile(
//            this,
//            "${packageName}.fileprovider",
//            file
//        )
//
//        val shareIntent = Intent(Intent.ACTION_SEND).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_STREAM, uri)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        startActivity(Intent.createChooser(shareIntent, "Bagikan struk via..."))
//    }
//
//    private fun shareStrukLayout() {
//        val view = findViewById<View>(R.id.layoutStruk)
//        val bitmap = getBitmapFromView(view)
//        val file = saveBitmapToFile(bitmap)
//        shareImageFile(file)
//    }
//
//    // Override onBackPressed untuk menangani back button ketika proses sedang berjalan
//    override fun onBackPressed() {
//        setResult(Activity.RESULT_OK)
//        super.onBackPressed()
//    }
//
//}

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
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import java.io.FileOutputStream

class CetakActivity  : AppCompatActivity() {

    private lateinit var b: ActivityCetakBinding
    private lateinit var dialog: Dialog
    private lateinit var kategoriAdapter: KategoriAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val kategoriList = mutableListOf<KategoriBiaya>()
    private var currentTransaksi: TransaksiBayarResponse? = null

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

        currentTransaksi = dataTransaksi

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

        b.btnShare.setOnClickListener {
            shareStrukAsText()
        }

        b.btnClose.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun generateReceiptText(): String {
        val transaksi = currentTransaksi?.data ?: return ""
        val stringBuilder = StringBuilder()
        stringBuilder.append("         PAMSIMAS\n")
        stringBuilder.append("      Ds.Tenggerlor\n")
        stringBuilder.append("  Kec.Kunjang Kab.Kediri\n")
        stringBuilder.append("  Telp: +62 857-8599-5219\n")
        stringBuilder.append("=============================\n")
        stringBuilder.append("          LUNAS\n")
        stringBuilder.append("=============================\n")
        stringBuilder.append("ID Transaksi    : ${transaksi.id_transaksi}\n")
        stringBuilder.append("Nama            : ${transaksi.nama_pelanggan}\n")
        stringBuilder.append("Alamat          : ${transaksi.alamat_pelanggan}\n")
        stringBuilder.append("Nama Petugas    : ${transaksi.nama_petugas}\n")
        stringBuilder.append("Tgl catat: ${transaksi.tanggal_pencatatan}\n")
        stringBuilder.append("Tgl Bayar: ${transaksi.tanggal_pembayaran}\n")
        stringBuilder.append("-----------------------------\n")
        stringBuilder.append("DETAIL PENGGUNAAN AIR\n")
        stringBuilder.append("-----------------------------\n")
        stringBuilder.append("Meter Awal      : ${transaksi.meter_awal} m³\n")
        stringBuilder.append("Meter Akhir     : ${transaksi.meter_akhir} m³\n")
        stringBuilder.append("Total Pemakaian : ${transaksi.jumlah_pemakaian} m³\n")
        stringBuilder.append("-----------------------------\n")
        stringBuilder.append("RINCIAN BIAYA\n")
        stringBuilder.append("-----------------------------\n")
        stringBuilder.append("Beban           : Rp ${formatCurrency(transaksi.detail_biaya.beban.tarif)}\n")
        // Tambahkan kategori biaya
        for (kategori in transaksi.detail_biaya.kategori) {
            stringBuilder.append("${kategori.volume} m³ x Rp.${formatCurrency(kategori.tarif)} : Rp ${formatCurrency(kategori.subtotal)}\n")
        }

        stringBuilder.append("-----------------------------\n")
        stringBuilder.append("TOTAL TAGIHAN   : Rp ${formatCurrency(transaksi.total_tagihan)}\n")
        stringBuilder.append("-----------------------------\n")

        stringBuilder.append("JUMLAH BAYAR    : Rp ${formatCurrency(transaksi.jumlah_bayar)}\n")
        stringBuilder.append("KEMBALIAN       : Rp ${formatCurrency(transaksi.kembalian)}\n")
        stringBuilder.append("=============================\n")

        stringBuilder.append("Terima kasih\n")

        return stringBuilder.toString()
    }

    private fun shareStrukAsText() {
        val receiptText = generateReceiptText()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }

        startActivity(Intent.createChooser(shareIntent, "Bagikan struk via..."))
    }

    // Fungsi-fungsi untuk share gambar (tetap dipertahankan jika diperlukan)
    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(getExternalFilesDir(null), "struk_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private fun shareImageFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Bagikan struk via..."))
    }

    private fun shareStrukLayout() {
        val view = findViewById<View>(R.id.layoutStruk)
        val bitmap = getBitmapFromView(view)
        val file = saveBitmapToFile(bitmap)
        shareImageFile(file)
    }

    // Override onBackPressed untuk menangani back button ketika proses sedang berjalan
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
}