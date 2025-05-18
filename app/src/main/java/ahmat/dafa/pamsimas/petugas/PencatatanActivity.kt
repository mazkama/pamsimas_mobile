package ahmat.dafa.pamsimas.petugas

import PemakaianBayarResponse
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ActivityPencatatanBinding
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.model.PemakaianResponse
import ahmat.dafa.pamsimas.model.PemakaianStoreResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.utils.QRCodeHelper
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class PencatatanActivity : AppCompatActivity() {

    private lateinit var b: ActivityPencatatanBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 200
    private var selectedImageFile: File? = null // Variabel untuk menyimpan file gambar yang dipilih
    private var isProcessing = false // Flag untuk mencegah duplikasi request
    private lateinit var dialog: Dialog
    private var dataPelanggan: Pemakaian? = null // bisa diakses di mana saja dalam activity

    private val CAMERA_PERMISSION_CODE = 101
    private val GALLERY_PERMISSION_CODE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPencatatanBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        dialog = Dialog(this)
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        dataPelanggan = intent.getSerializableExtra("data_pemakaian") as? Pemakaian
        dataPelanggan?.let { pemakaian ->

            val qrBitmap = QRCodeHelper.generateQRCode(pemakaian.id_users.toString())
            if (qrBitmap != null) {
                b.qrImageView.setImageBitmap(qrBitmap)
            }

            b.tvCustomerId.text = "ID#${pemakaian.id_users}"
            b.tvCustomerName.text = "${pemakaian.nama}"
            b.tvCustomerAddress.text = "${pemakaian.alamat}, RT ${pemakaian.rt} RW ${pemakaian.rw}"
            b.tvUsage.text = "Pemakaian: ${pemakaian.meter_akhir} m³"
            b.tvInitialMeter.text = "${pemakaian.meter_akhir} m³"
            b.tvPreviousDate.text = pemakaian.waktu_catat ?: "-"
        }

        // Tombol kembali di header
        b.btnBack.setOnClickListener {
            // Cek jika proses sedang berjalan
            if (isProcessing) {
                Toast.makeText(this, "Sedang memproses data, harap tunggu...", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            setResult(Activity.RESULT_OK)
            finish()
        }
        // Fungsi untuk mengambil foto dari kamera atau galeri
        b.btnTakePhoto.setOnClickListener {
            val options = arrayOf("Ambil Foto", "Pilih dari Galeri")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // Pilih untuk mengambil foto dengan kamera
                    1 -> openGallery() // Pilih untuk memilih gambar dari galeri
                }
            }
            builder.show()
        }

        // Fungsi untuk tombol simpan data
        b.btnSaveOnly.setOnClickListener {
            // Cek jika proses sedang berjalan
            if (isProcessing) {
                Toast.makeText(this, "Sedang memproses data, harap tunggu...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendPemakaianStore() // Panggil fungsi kirim data ke API
        }

        // Fungsi untuk tombol lanjut pembayaran (jika diperlukan)
        b.btnProceedPayment.setOnClickListener {
            // Implementasi untuk lanjut ke halaman pembayaran
//            Toast.makeText(this, "Fitur pembayaran belum tersedia", Toast.LENGTH_SHORT).show()
            sendPemakaianBayar()
        }

        b.ivMeterPhoto.setOnClickListener {
            val bitmap = BitmapFactory.decodeFile(selectedImageFile?.absolutePath)
            b.fullscreenImageView.setImageBitmap(bitmap)
            b.previewOverlay.visibility = View.VISIBLE
        }


        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
        }
    }

    // Fungsi untuk membuka kamera
    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }


    // Fungsi untuk membuka galeri
    private fun openGalleryWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_CODE
            )
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan
                Toast.makeText(this, "Izin kamera diberikan", Toast.LENGTH_SHORT).show()
            } else {
                // Izin ditolak
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Menangani hasil dari kamera atau galeri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    try {
                        val photo = data.extras?.get("data") as Bitmap
                        // Set image to ImageView
                        b.ivMeterPhoto.setImageBitmap(photo)
                        // Ubah visibility ImageView dan placeholder
                        b.ivMeterPhoto.visibility = View.VISIBLE
                        b.photoPlaceholderContent.visibility = View.GONE
                        // Menyimpan file foto
                        selectedImageFile = getImageFileFromBitmap(photo)
                        Log.d("PencatatanActivity", "Gambar dari kamera berhasil disimpan: ${selectedImageFile?.absolutePath}")
                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing camera image", e)
                        Toast.makeText(this, "Gagal memproses foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    try {
                        val selectedImageUri = data.data
                        // Set image to ImageView directly from URI
                        b.ivMeterPhoto.setImageURI(selectedImageUri)
                        // Ubah visibility ImageView dan placeholder
                        b.ivMeterPhoto.visibility = View.VISIBLE
                        b.photoPlaceholderContent.visibility = View.GONE
                        selectedImageUri?.let {
                            selectedImageFile = getImageFileFromUri(it)
                            Log.d("PencatatanActivity", "Gambar dari galeri berhasil disimpan: ${selectedImageFile?.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing gallery image", e)
                        Toast.makeText(this, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Mengambil path gambar dari URI
    private fun getImageFileFromUri(uri: Uri): File? {
        try {
            val file = File(cacheDir, "temp_image.jpg")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error getting file from URI", e)
            Toast.makeText(this, "Gagal mengambil file gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    // Mengambil file dari Bitmap
    private fun getImageFileFromBitmap(bitmap: Bitmap): File {
        try {
            val file = File(cacheDir, "temp_image.jpg")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            file.writeBytes(stream.toByteArray())
            return file
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error getting file from bitmap", e)
            Toast.makeText(this, "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
            throw e
        }
    }

    // Fungsi untuk mengirim data ke API
    private fun sendPemakaianStore() {
        val meterAkhirStr = b.etFinalMeter.text.toString()

        // Validasi input meter akhir
        if (meterAkhirStr.isEmpty()) {
            Toast.makeText(this, "Isi meter akhir terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi input harus angka
        val meterAkhir = meterAkhirStr.toIntOrNull()
        if (meterAkhir == null) {
            Toast.makeText(this, "Meter akhir harus berupa angka", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi jika foto belum dipilih
        if (selectedImageFile == null) {
            Toast.makeText(this, "Pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil data pelanggan
        val pemakaian = dataPelanggan ?: run {
            Toast.makeText(this, "Data pelanggan tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil nilai meter awal dari data pelanggan
        val meterAwal = pemakaian.meter_akhir ?: 0

        // Validasi meter akhir tidak boleh lebih kecil dari meter awal
        if (meterAkhir < meterAwal) {
            Toast.makeText(this, "Meter akhir tidak boleh lebih kecil dari meter awal ($meterAwal)", Toast.LENGTH_SHORT).show()
            return
        }

        // Set proses berjalan dan tampilkan loading dialog
        isProcessing = true
        dialog.show()

        // Nonaktifkan tombol selama proses
        b.btnSaveOnly.isEnabled = false
        b.btnProceedPayment.isEnabled = false

        // Siapkan data request body
        val idUsers = RequestBody.create("text/plain".toMediaType(), pemakaian.id_users.toString())
        val meterAwalBody = RequestBody.create("text/plain".toMediaType(), meterAwal.toString())
        val meterAkhirBody = RequestBody.create("text/plain".toMediaType(), meterAkhir.toString())

        val fotoMeteranPart = selectedImageFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("foto_meteran", file.name, requestFile)
        }
//        val fotoMeteranPart = null
        val token = sharedPreferences.getString("token", null)


        // Panggil API dengan Retrofit
        ApiClient.getInstance(this,token).storePemakaian(idUsers, meterAwalBody, meterAkhirBody,fotoMeteranPart)
            .enqueue(object : Callback<PemakaianStoreResponse> {
                override fun onResponse(call: Call<PemakaianStoreResponse>, response: Response<PemakaianStoreResponse>) {
                    runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSaveOnly.isEnabled = true
                        b.btnProceedPayment.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this@PencatatanActivity, "Data berhasil dikirim", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }else {
                            // Baca errorBody dari Laravel
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = StringBuilder()

                            if (!errorBody.isNullOrEmpty()) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    val message = jsonObject.optString("message", "Terjadi kesalahan.")
                                    errorMessage.append("$message\n")

                                    if (jsonObject.has("errors")) {
                                        val errorsObject = jsonObject.getJSONObject("errors")
                                        val keys = errorsObject.keys()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            val errorArray = errorsObject.getJSONArray(key)
                                            for (i in 0 until errorArray.length()) {
                                                errorMessage.append("${errorArray.getString(i)}\n")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage.append("Gagal parsing error: ${e.localizedMessage}")
                                }
                            } else {
                                errorMessage.append("Gagal: ${response.code()}")
                            }

                            Toast.makeText(this@PencatatanActivity, errorMessage.toString().trim(), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<PemakaianStoreResponse>, t: Throwable) {
                    runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSaveOnly.isEnabled = true
                        b.btnProceedPayment.isEnabled = true
                        if (t is java.net.SocketTimeoutException) {
                            // Timeout -> tampilkan dialog khusus
                            showTimeoutDialog()
                        } else {
                            Toast.makeText(this@PencatatanActivity, "Gagal: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })


    }

    // Fungsi untuk mengirim data ke API
    private fun sendPemakaianBayar() {
        val meterAkhirStr = b.etFinalMeter.text.toString()

        // Validasi input meter akhir
        if (meterAkhirStr.isEmpty()) {
            Toast.makeText(this, "Isi meter akhir terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi input harus angka
        val meterAkhir = meterAkhirStr.toIntOrNull()
        if (meterAkhir == null) {
            Toast.makeText(this, "Meter akhir harus berupa angka", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi jika foto belum dipilih
        if (selectedImageFile == null) {
            Toast.makeText(this, "Pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil data pelanggan
        val pemakaian = dataPelanggan ?: run {
            Toast.makeText(this, "Data pelanggan tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil nilai meter awal dari data pelanggan
        val meterAwal = pemakaian.meter_akhir ?: 0

        // Validasi meter akhir tidak boleh lebih kecil dari meter awal
        if (meterAkhir < meterAwal) {
            Toast.makeText(this, "Meter akhir tidak boleh lebih kecil dari meter awal ($meterAwal)", Toast.LENGTH_SHORT).show()
            return
        }

        // Set proses berjalan dan tampilkan loading dialog
        isProcessing = true
        dialog.show()

        // Nonaktifkan tombol selama proses
        b.btnSaveOnly.isEnabled = false
        b.btnProceedPayment.isEnabled = false

        // Siapkan data request body
        val idUsers = RequestBody.create("text/plain".toMediaType(), pemakaian.id_users.toString())
        val meterAwalBody = RequestBody.create("text/plain".toMediaType(), meterAwal.toString())
        val meterAkhirBody = RequestBody.create("text/plain".toMediaType(), meterAkhir.toString())

        val fotoMeteranPart = selectedImageFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("foto_meteran", file.name, requestFile)
        }
//        val fotoMeteranPart = null

        val token = sharedPreferences.getString("token", null)


        // Panggil API dengan Retrofit
        ApiClient.getInstance(this,token).bayarPemakaian(idUsers, meterAwalBody, meterAkhirBody,fotoMeteranPart)
            .enqueue(object : Callback<PemakaianBayarResponse> {
                override fun onResponse(call: Call<PemakaianBayarResponse>, response: Response<PemakaianBayarResponse>) {
                    runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSaveOnly.isEnabled = true
                        b.btnProceedPayment.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this@PencatatanActivity, "Data berhasil dikirim", Toast.LENGTH_SHORT).show()

                            // Intent ke LanjutBayarActivity
                            val intent = Intent(this@PencatatanActivity, LanjutBayarActivity::class.java).apply {
                                putExtra("data_pemakaian", response.body()) // pastikan model PemakaianBayarResponse implements Parcelable atau Serializable
                                putExtra("data_pelanggan", dataPelanggan)
                            }
                            startActivity(intent)
                            finish()
                        }else {
                            // Baca errorBody dari Laravel
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = StringBuilder()

                            if (!errorBody.isNullOrEmpty()) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    val message = jsonObject.optString("message", "Terjadi kesalahan.")
                                    errorMessage.append("$message\n")

                                    if (jsonObject.has("errors")) {
                                        val errorsObject = jsonObject.getJSONObject("errors")
                                        val keys = errorsObject.keys()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            val errorArray = errorsObject.getJSONArray(key)
                                            for (i in 0 until errorArray.length()) {
                                                errorMessage.append("${errorArray.getString(i)}\n")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage.append("Gagal parsing error: ${e.localizedMessage}")
                                }
                            } else {
                                errorMessage.append("Gagal: ${response.code()}")
                            }

                            Toast.makeText(this@PencatatanActivity, errorMessage.toString().trim(), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<PemakaianBayarResponse>, t: Throwable) {
                    runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSaveOnly.isEnabled = true
                        b.btnProceedPayment.isEnabled = true

                        if (t is java.net.SocketTimeoutException) {
                            // Timeout -> tampilkan dialog khusus
                            showTimeoutDialog()
                        } else {
                            Toast.makeText(this@PencatatanActivity, "Gagal: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })

    }

    // Menampilkan dialog ketika terjadi timeout
    private fun showTimeoutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Koneksi Timeout")
        builder.setMessage("Koneksi ke server memakan waktu terlalu lama, namun data mungkin telah tersimpan. Apakah Anda ingin kembali ke halaman sebelumnya?")
        builder.setPositiveButton("Ya") { _, _ ->
            finish()  // Kembali ke halaman sebelumnya
        }
        builder.setNegativeButton("Coba Lagi") { _, _ ->
            // Opsional: bisa menambahkan fungsi untuk cek status data jika API mendukung
            Toast.makeText(this, "Silakan coba beberapa saat lagi", Toast.LENGTH_SHORT).show()
        }
        builder.setCancelable(false)
        builder.show()
    }

    // Override onBackPressed untuk menangani back button ketika proses sedang berjalan
    override fun onBackPressed() {
        if (isProcessing) {
            Toast.makeText(this, "Sedang memproses data, harap tunggu...", Toast.LENGTH_SHORT).show()
            return
        }
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
}