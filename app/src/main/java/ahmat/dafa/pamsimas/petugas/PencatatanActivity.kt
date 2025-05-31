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
import android.graphics.Matrix
import android.media.ExifInterface
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
import java.io.FileOutputStream
import java.io.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max
import kotlin.math.min

class PencatatanActivity : AppCompatActivity() {

    private lateinit var b: ActivityPencatatanBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val CAMERA_REQUEST_CODE = 100
//    private val GALLERY_REQUEST_CODE = 200
    private var selectedImageFile: File? = null // Variabel untuk menyimpan file gambar yang dipilih
    private var isProcessing = false // Flag untuk mencegah duplikasi request
    private lateinit var dialog: Dialog
    private var dataPelanggan: Pemakaian? = null // bisa diakses di mana saja dalam activity

//    private val CAMERA_PERMISSION_CODE = 101
//    private val GALLERY_PERMISSION_CODE = 201
    // Target ukuran file dalam KB (200-300 KB)
    private val TARGET_FILE_SIZE_KB = 250
    private val MAX_FILE_SIZE_BYTES = TARGET_FILE_SIZE_KB * 1024
    private val MIN_QUALITY = 30 // Kualitas minimum untuk menjaga keterbacaan text
    private var photoFile: File? = null
    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val GALLERY_REQUEST_CODE = 200
        private const val CAMERA_PERMISSION_CODE = 101
        private const val GALLERY_PERMISSION_CODE = 201
    }


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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Pastikan ada aplikasi kamera yang bisa menangani intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Buat file untuk menyimpan foto
            photoFile = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("PencatatanActivity", "Error creating image file", ex)
                null
            }

            // Lanjutkan hanya jika file berhasil dibuat
            photoFile?.also { file ->
                val photoURI: Uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider", // Pastikan ini sesuai dengan manifest
                    file
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(this, "Tidak ada aplikasi kamera yang tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Buat nama file dengan timestamp
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", // prefix
            ".jpg", // suffix
            storageDir // directory
        )
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


    // Fungsi untuk log ukuran file
    private fun logFileSize(file: File?) {
        file?.let {
            val sizeInKB = it.length() / 1024
            Log.d("PencatatanActivity", "Ukuran file: ${sizeInKB}KB")
        }
    }

    // Fungsi untuk merotasi gambar berdasarkan EXIF data
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        return try {
            val input = contentResolver.openInputStream(selectedImage)
            val ei = ExifInterface(input!!)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
                else -> img
            }
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error rotating image", e)
            img
        }
    }

    // Fungsi untuk merotasi bitmap
    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
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
    // Ganti fungsi kompres gambar yang sudah ada dengan versi yang lebih efektif ini



    // Fungsi untuk mendapatkan file terkompres dari URI (DIPERBAIKI)
    private fun getCompressedImageFileFromUri(uri: Uri): File? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            // Rotate bitmap jika diperlukan berdasarkan EXIF data
            val rotatedBitmap = rotateImageIfRequired(bitmap, uri)

            // Kompres dengan algoritma yang lebih baik
            getOptimizedCompressedFile(rotatedBitmap)
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error getting compressed file from URI", e)
            Toast.makeText(this, "Gagal mengompres gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Fungsi untuk mendapatkan file terkompres dari Bitmap (DIPERBAIKI)
    private fun getCompressedImageFileFromBitmap(bitmap: Bitmap): File? {
        return try {
            getOptimizedCompressedFile(bitmap)
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error getting compressed file from bitmap", e)
            Toast.makeText(this, "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Fungsi kompresi yang dioptimalkan (BARU)
    private fun getOptimizedCompressedFile(originalBitmap: Bitmap): File? {
        return try {
            val file = File(cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")

            // Step 1: Resize gambar berdasarkan ukuran optimal
            val resizedBitmap = getOptimalSizedBitmap(originalBitmap)

            // Step 2: Kompres dengan kualitas progresif
            val finalBitmap = compressToTargetSize(resizedBitmap, MAX_FILE_SIZE_BYTES)

            // Step 3: Simpan ke file
            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // Cleanup memory
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            if (finalBitmap != resizedBitmap && finalBitmap != originalBitmap) {
                finalBitmap.recycle()
            }

            // Log hasil kompresi
            val finalSizeKB = file.length() / 1024
            Log.d("PencatatanActivity", "Kompresi selesai. Ukuran akhir: ${finalSizeKB}KB")

            file
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error in optimized compression", e)
            null
        }
    }

    // Fungsi untuk mendapatkan ukuran optimal (BARU)
    private fun getOptimalSizedBitmap(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Hitung estimasi ukuran file original
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val originalSize = stream.size()

        // Jika sudah kecil, tidak perlu resize
        if (originalSize <= MAX_FILE_SIZE_BYTES) {
            return bitmap
        }

        // Tentukan ukuran maksimal berdasarkan aspek ratio
        val maxDimension = when {
            originalSize > 5 * 1024 * 1024 -> 1200 // > 5MB -> max 1200px
            originalSize > 2 * 1024 * 1024 -> 1400 // > 2MB -> max 1400px
            originalSize > 1 * 1024 * 1024 -> 1600 // > 1MB -> max 1600px
            else -> 1800 // <= 1MB -> max 1800px
        }

        val scaleFactor = if (originalWidth > originalHeight) {
            maxDimension.toFloat() / originalWidth
        } else {
            maxDimension.toFloat() / originalHeight
        }

        return if (scaleFactor < 1.0f) {
            val newWidth = (originalWidth * scaleFactor).toInt()
            val newHeight = (originalHeight * scaleFactor).toInt()

            Log.d("PencatatanActivity", "Resize dari ${originalWidth}x${originalHeight} ke ${newWidth}x${newHeight}")

            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    // Fungsi kompresi progresif yang lebih baik (BARU)
    private fun compressToTargetSize(bitmap: Bitmap, targetSizeBytes: Int): Bitmap {
        var currentBitmap = bitmap
        var quality = 90 // Mulai dengan kualitas tinggi

        // Loop untuk mencari kualitas optimal
        while (quality >= MIN_QUALITY) {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val currentSize = stream.size()

            Log.d("PencatatanActivity", "Quality: $quality%, Size: ${currentSize / 1024}KB")

            if (currentSize <= targetSizeBytes) {
                Log.d("PencatatanActivity", "Target tercapai pada quality: $quality%")
                break
            }

            // Turunkan kualitas secara bertahap
            quality -= if (currentSize > targetSizeBytes * 2) {
                15 // Turun drastis jika masih terlalu besar
            } else {
                8  // Turun perlahan jika sudah mendekati target
            }
        }

        // Jika masih terlalu besar di kualitas minimum, resize lagi
        if (quality < MIN_QUALITY) {
            Log.d("PencatatanActivity", "Perlu resize tambahan karena masih terlalu besar")

            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, MIN_QUALITY, stream)

            if (stream.size() > targetSizeBytes) {
                // Resize lebih kecil lagi
                val additionalScale = kotlin.math.sqrt(targetSizeBytes.toDouble() / stream.size().toDouble()) * 0.9
                val newWidth = (currentBitmap.width * additionalScale).toInt()
                val newHeight = (currentBitmap.height * additionalScale).toInt()

                Log.d("PencatatanActivity", "Resize tambahan ke ${newWidth}x${newHeight}")

                val smallerBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)

                if (currentBitmap != bitmap) {
                    currentBitmap.recycle()
                }

                currentBitmap = smallerBitmap
            }
        }

        return currentBitmap
    }

    // Fungsi untuk mengompres gambar ke ukuran target (GANTI YANG LAMA)
    private fun compressImageToTargetSize(originalBitmap: Bitmap, targetSizeBytes: Int): Bitmap {
        // Fungsi ini sudah diganti dengan getOptimizedCompressedFile
        // Tapi tetap diperlukan untuk kompatibilitas jika masih ada yang memanggil
        return compressToTargetSize(originalBitmap, targetSizeBytes)
    }
    // Fungsi kompresi khusus untuk foto kamera
    private fun getOptimizedCameraCompression(bitmap: Bitmap, originalPath: String): File? {
        return try {
            val file = File(cacheDir, "camera_compressed_${System.currentTimeMillis()}.jpg")

            // Untuk foto kamera, kita perlu pendekatan yang berbeda
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            Log.d("PencatatanActivity", "Original camera photo: ${originalWidth}x${originalHeight}")

            // Tentukan ukuran target berdasarkan dimensi foto kamera
            val targetDimension = when {
                originalWidth > 3000 || originalHeight > 3000 -> 2048
                originalWidth > 2000 || originalHeight > 2000 -> 1600
                originalWidth > 1500 || originalHeight > 1500 -> 1200
                else -> max(originalWidth, originalHeight) // Jangan resize jika sudah kecil
            }

            // Resize jika perlu
            val resizedBitmap = if (max(originalWidth, originalHeight) > targetDimension) {
                val scaleFactor = targetDimension.toFloat() / max(originalWidth, originalHeight)
                val newWidth = (originalWidth * scaleFactor).toInt()
                val newHeight = (originalHeight * scaleFactor).toInt()

                Log.d("PencatatanActivity", "Resizing camera photo to: ${newWidth}x${newHeight}")
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            // Kompres dengan kualitas yang memastikan ukuran 200-300KB
            var quality = 85 // Mulai dengan kualitas tinggi untuk foto kamera
            var targetAchieved = false

            while (quality >= 60 && !targetAchieved) { // Minimum quality 60 untuk menjaga ketajaman
                val stream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                val currentSize = stream.size()
                val currentSizeKB = currentSize / 1024

                Log.d("PencatatanActivity", "Camera compression - Quality: $quality%, Size: ${currentSizeKB}KB")

                // Target 200-300KB
                if (currentSizeKB in 200..300) {
                    targetAchieved = true
                    Log.d("PencatatanActivity", "Target achieved at quality: $quality%")
                } else if (currentSizeKB < 200) {
                    // Jika terlalu kecil, naikkan quality atau hentikan
                    if (quality < 85) {
                        quality += 5
                    } else {
                        targetAchieved = true // Terima apa adanya jika sudah max quality
                    }
                } else {
                    // Terlalu besar, turunkan quality
                    quality -= 5
                }

                if (targetAchieved || quality < 60) {
                    FileOutputStream(file).use { out ->
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, max(quality, 60), out)
                    }
                    break
                }
            }

            // Cleanup memory
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }

            val finalSizeKB = file.length() / 1024
            Log.d("PencatatanActivity", "Camera photo final size: ${finalSizeKB}KB")

            file
        } catch (e: Exception) {
            Log.e("PencatatanActivity", "Error in camera compression", e)
            null
        }
    }
    // Update juga bagian onActivityResult untuk memastikan kompresi berjalan optimal
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        // Tampilkan loading saat memproses gambar
                        Toast.makeText(this, "Memproses gambar dari kamera...", Toast.LENGTH_SHORT).show()

                        photoFile?.let { file ->
                            if (file.exists()) {
                                // Baca foto dari file yang sudah disimpan kamera
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                                if (bitmap != null) {
                                    // Tampilkan preview
                                    b.ivMeterPhoto.setImageBitmap(bitmap)
                                    b.ivMeterPhoto.visibility = View.VISIBLE
                                    b.photoPlaceholderContent.visibility = View.GONE

                                    // Kompres dengan ukuran yang tepat untuk foto kamera
                                    selectedImageFile = getOptimizedCameraCompression(bitmap, file.absolutePath)

                                    selectedImageFile?.let { compressedFile ->
                                        val sizeKB = compressedFile.length() / 1024
                                        Log.d("PencatatanActivity", "Foto kamera berhasil dikompres: ${compressedFile.absolutePath}, Ukuran: ${sizeKB}KB")
                                        Toast.makeText(this, "Foto berhasil diproses (${sizeKB}KB)", Toast.LENGTH_SHORT).show()

                                        // Hapus file temporary original
                                        if (file.absolutePath != compressedFile.absolutePath) {
                                            file.delete()
                                        }
                                    } ?: run {
                                        Toast.makeText(this, "Gagal memproses foto kamera", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Gagal membaca foto dari kamera", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "File foto tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(this, "Tidak ada file foto yang dihasilkan", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing camera image", e)
                        Toast.makeText(this, "Gagal memproses foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    try {
                        val selectedImageUri = data?.data
                        Toast.makeText(this, "Memproses gambar dari galeri...", Toast.LENGTH_SHORT).show()

                        selectedImageUri?.let { uri ->
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            val rotatedBitmap = rotateImageIfRequired(bitmap, uri)

                            b.ivMeterPhoto.setImageBitmap(rotatedBitmap)
                            b.ivMeterPhoto.visibility = View.VISIBLE
                            b.photoPlaceholderContent.visibility = View.GONE

                            selectedImageFile = getOptimizedCompressedFile(rotatedBitmap)

                            selectedImageFile?.let { file ->
                                val sizeKB = file.length() / 1024
                                Log.d("PencatatanActivity", "Gambar dari galeri berhasil dikompres: ${file.absolutePath}, Ukuran: ${sizeKB}KB")
                                Toast.makeText(this, "Foto berhasil diproses (${sizeKB}KB)", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Toast.makeText(this, "Gagal memproses foto", Toast.LENGTH_SHORT).show()
                            }

                            if (rotatedBitmap != bitmap) {
                                bitmap.recycle()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing gallery image", e)
                        Toast.makeText(this, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}