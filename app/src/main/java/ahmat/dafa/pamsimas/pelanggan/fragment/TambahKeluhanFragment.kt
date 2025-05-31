package ahmat.dafa.pamsimas.pelanggan.fragment

import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.UbahPasswordActivity
import ahmat.dafa.pamsimas.databinding.FragmentTambahPengaduanBinding
import ahmat.dafa.pamsimas.model.PemakaianStoreResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class TambahKeluhanFragment : Fragment() {

    private lateinit var b: FragmentTambahPengaduanBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 200
    private var selectedImageFile: File? = null // Variabel untuk menyimpan file gambar yang dipilih
    private var isProcessing = false // Flag untuk mencegah duplikasi request
    private lateinit var dialog: Dialog
    lateinit var fdashboardpelanggan: BerandaPelangganFragment
    private var photoFile: File? = null

    // Target ukuran file dalam KB (200-300 KB)
    private val TARGET_FILE_SIZE_KB = 250
    private val MAX_FILE_SIZE_BYTES = TARGET_FILE_SIZE_KB * 1024
    private val MIN_QUALITY = 30 // Kualitas minimum untuk menjaga keterbacaan text

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val CAMERA_PERMISSION_CODE = 101
        private const val GALLERY_PERMISSION_CODE = 201
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisParent = activity as MainActivity
        b = FragmentTambahPengaduanBinding.inflate(inflater, container, false)
        v = b.root

        fdashboardpelanggan = BerandaPelangganFragment()

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        b.btnTakePhoto.setOnClickListener {
            val options = arrayOf("Ambil Foto", "Pilih dari Galeri")
            val builder = android.app.AlertDialog.Builder(thisParent)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraWithPermission() // Pilih untuk mengambil foto dengan kamera
                    1 -> openGalleryWithPermission() // Pilih untuk memilih gambar dari galeri
                }
            }
            builder.show()
        }

        b.btnSubmit.setOnClickListener {
            sendPemakaianStore()
        }

        b.ivPhoto.setOnClickListener {
            selectedImageFile?.let { file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                b.fullscreenImageView.setImageBitmap(bitmap)
                b.previewOverlay.visibility = View.VISIBLE
            }
        }

        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
        }

        return v
    }

    // Fungsi untuk membuka kamera dengan permission
    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    // Fungsi untuk membuka kamera
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Pastikan ada aplikasi kamera yang bisa menangani intent
        if (takePictureIntent.resolveActivity(thisParent.packageManager) != null) {
            // Buat file untuk menyimpan foto
            photoFile = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("TambahKeluhanFragment", "Error creating image file", ex)
                null
            }

            // Lanjutkan hanya jika file berhasil dibuat
            photoFile?.also { file ->
                val photoURI: Uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${thisParent.packageName}.fileprovider", // Pastikan ini sesuai dengan manifest
                    file
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(thisParent, "Tidak ada aplikasi kamera yang tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Buat nama file dengan timestamp
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", // prefix
            ".jpg", // suffix
            storageDir // directory
        )
    }

    // Fungsi untuk membuka galeri dengan permission
    private fun openGalleryWithPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_CODE
            )
        } else {
            openGallery()
        }
    }

    // Fungsi untuk membuka galeri
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

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                    Toast.makeText(thisParent, "Izin kamera diberikan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(thisParent, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                    Toast.makeText(thisParent, "Izin galeri diberikan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(thisParent, "Izin galeri ditolak", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        // Tampilkan loading saat memproses gambar
                        Toast.makeText(thisParent, "Memproses gambar dari kamera...", Toast.LENGTH_SHORT).show()

                        photoFile?.let { file ->
                            if (file.exists()) {
                                // Baca foto dari file yang sudah disimpan kamera
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                                if (bitmap != null) {
                                    // Tampilkan preview
                                    b.ivPhoto.setImageBitmap(bitmap)
                                    b.ivPhoto.visibility = View.VISIBLE
                                    b.photoPlaceholderContent.visibility = View.GONE

                                    // Kompres dengan ukuran yang tepat untuk foto kamera
                                    selectedImageFile = getOptimizedCameraCompression(bitmap, file.absolutePath)

                                    selectedImageFile?.let { compressedFile ->
                                        val sizeKB = compressedFile.length() / 1024
                                        Log.d("TambahKeluhanFragment", "Foto kamera berhasil dikompres: ${compressedFile.absolutePath}, Ukuran: ${sizeKB}KB")
                                        Toast.makeText(thisParent, "Foto berhasil diproses (${sizeKB}KB)", Toast.LENGTH_SHORT).show()

                                        // Hapus file temporary original
                                        if (file.absolutePath != compressedFile.absolutePath) {
                                            file.delete()
                                        }
                                    } ?: run {
                                        Toast.makeText(thisParent, "Gagal memproses foto kamera", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(thisParent, "Gagal membaca foto dari kamera", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(thisParent, "File foto tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(thisParent, "Tidak ada file foto yang dihasilkan", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        Log.e("TambahKeluhanFragment", "Error processing camera image", e)
                        Toast.makeText(thisParent, "Gagal memproses foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    try {
                        val selectedImageUri = data?.data
                        Toast.makeText(thisParent, "Memproses gambar dari galeri...", Toast.LENGTH_SHORT).show()

                        selectedImageUri?.let { uri ->
                            val bitmap = MediaStore.Images.Media.getBitmap(thisParent.contentResolver, uri)
                            val rotatedBitmap = rotateImageIfRequired(bitmap, uri)

                            b.ivPhoto.setImageBitmap(rotatedBitmap)
                            b.ivPhoto.visibility = View.VISIBLE
                            b.photoPlaceholderContent.visibility = View.GONE

                            selectedImageFile = getOptimizedCompressedFile(rotatedBitmap)

                            selectedImageFile?.let { file ->
                                val sizeKB = file.length() / 1024
                                Log.d("TambahKeluhanFragment", "Gambar dari galeri berhasil dikompres: ${file.absolutePath}, Ukuran: ${sizeKB}KB")
                                Toast.makeText(thisParent, "Foto berhasil diproses (${sizeKB}KB)", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Toast.makeText(thisParent, "Gagal memproses foto", Toast.LENGTH_SHORT).show()
                            }

                            if (rotatedBitmap != bitmap) {
                                bitmap.recycle()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("TambahKeluhanFragment", "Error processing gallery image", e)
                        Toast.makeText(thisParent, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Fungsi untuk merotasi gambar berdasarkan EXIF data
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        return try {
            val input = thisParent.contentResolver.openInputStream(selectedImage)
            val ei = ExifInterface(input!!)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
                else -> img
            }
        } catch (e: Exception) {
            Log.e("TambahKeluhanFragment", "Error rotating image", e)
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

    // Fungsi kompresi yang dioptimalkan
    private fun getOptimizedCompressedFile(originalBitmap: Bitmap): File? {
        return try {
            val file = File(requireContext().cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")

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
            Log.d("TambahKeluhanFragment", "Kompresi selesai. Ukuran akhir: ${finalSizeKB}KB")

            file
        } catch (e: Exception) {
            Log.e("TambahKeluhanFragment", "Error in optimized compression", e)
            null
        }
    }

    // Fungsi untuk mendapatkan ukuran optimal
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

            Log.d("TambahKeluhanFragment", "Resize dari ${originalWidth}x${originalHeight} ke ${newWidth}x${newHeight}")

            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    // Fungsi kompresi progresif yang lebih baik
    private fun compressToTargetSize(bitmap: Bitmap, targetSizeBytes: Int): Bitmap {
        var currentBitmap = bitmap
        var quality = 90 // Mulai dengan kualitas tinggi

        // Loop untuk mencari kualitas optimal
        while (quality >= MIN_QUALITY) {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val currentSize = stream.size()

            Log.d("TambahKeluhanFragment", "Quality: $quality%, Size: ${currentSize / 1024}KB")

            if (currentSize <= targetSizeBytes) {
                Log.d("TambahKeluhanFragment", "Target tercapai pada quality: $quality%")
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
            Log.d("TambahKeluhanFragment", "Perlu resize tambahan karena masih terlalu besar")

            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, MIN_QUALITY, stream)

            if (stream.size() > targetSizeBytes) {
                // Resize lebih kecil lagi
                val additionalScale = kotlin.math.sqrt(targetSizeBytes.toDouble() / stream.size().toDouble()) * 0.9
                val newWidth = (currentBitmap.width * additionalScale).toInt()
                val newHeight = (currentBitmap.height * additionalScale).toInt()

                Log.d("TambahKeluhanFragment", "Resize tambahan ke ${newWidth}x${newHeight}")

                val smallerBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)

                if (currentBitmap != bitmap) {
                    currentBitmap.recycle()
                }

                currentBitmap = smallerBitmap
            }
        }

        return currentBitmap
    }

    // Fungsi kompresi khusus untuk foto kamera
    private fun getOptimizedCameraCompression(bitmap: Bitmap, originalPath: String): File? {
        return try {
            val file = File(requireContext().cacheDir, "camera_compressed_${System.currentTimeMillis()}.jpg")

            // Untuk foto kamera, kita perlu pendekatan yang berbeda
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            Log.d("TambahKeluhanFragment", "Original camera photo: ${originalWidth}x${originalHeight}")

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

                Log.d("TambahKeluhanFragment", "Resizing camera photo to: ${newWidth}x${newHeight}")
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

                Log.d("TambahKeluhanFragment", "Camera compression - Quality: $quality%, Size: ${currentSizeKB}KB")

                // Target 200-300KB
                if (currentSizeKB in 200..300) {
                    targetAchieved = true
                    Log.d("TambahKeluhanFragment", "Target achieved at quality: $quality%")
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
            Log.d("TambahKeluhanFragment", "Camera photo final size: ${finalSizeKB}KB")

            file
        } catch (e: Exception) {
            Log.e("TambahKeluhanFragment", "Error in camera compression", e)
            null
        }
    }

    // Fungsi untuk mengirim data ke API
    private fun sendPemakaianStore() {

        // Validasi input keterangan
        val keterangan = b.etComplaintDesc.text.toString()
        if (keterangan.isEmpty()) {
            Toast.makeText(thisParent, "Keterangan harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi jika foto belum dipilih
        if (selectedImageFile == null) {
            Toast.makeText(thisParent, "Pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Set proses berjalan dan tampilkan loading dialog
        isProcessing = true
        dialog.show()

        // Nonaktifkan tombol selama proses
        b.btnSubmit.isEnabled = false

        // Siapkan data request body
        val keteranganBody = RequestBody.create("text/plain".toMediaType(), keterangan)

        val fotoKeluhanPart = selectedImageFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("foto_keluhan", file.name, requestFile)
        }
        val token = sharedPreferences.getString("token", null)

        // Panggil API dengan Retrofit
        ApiClient.getInstance(thisParent, token).storeKeluhan(keteranganBody, fotoKeluhanPart)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    thisParent.runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSubmit.isEnabled = true

                        if (response.isSuccessful) {
                            val responseString = response.body()?.string()
                            try {
                                val json = JSONObject(responseString)
                                val success = json.optBoolean("success")
                                val message = json.optString("message")

                                Toast.makeText(thisParent, message, Toast.LENGTH_SHORT).show()
                                clearForm()

                                // Ganti fragment ke FragmentDashboardPelanggan (fdashboardpelanggan adalah instance-nya)
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.frameLayout, fdashboardpelanggan)
                                    .commit()

                                // Akses frameLayout dari MainActivity melalui thisParent
                                val frameLayout = thisParent.findViewById<View>(R.id.frameLayout)
                                frameLayout.setBackgroundColor(Color.argb(255, 255, 255, 255))
                                frameLayout.visibility = View.VISIBLE

                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(thisParent, "Gagal memproses response", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorString = response.errorBody()?.string()
                            try {
                                val json = JSONObject(errorString)
                                val message = json.optString("message")
                                Toast.makeText(thisParent, message, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(thisParent, "Gagal memproses error response", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    thisParent.runOnUiThread {
                        dialog.dismiss()
                        isProcessing = false
                        b.btnSubmit.isEnabled = true
                        Toast.makeText(thisParent, "Gagal: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun clearForm() {
        b.etComplaintDesc.text?.clear()
        b.ivPhoto.setImageDrawable(null)
        b.ivPhoto.visibility = View.GONE
        b.photoPlaceholderContent.visibility = View.VISIBLE
        selectedImageFile = null

        // Hapus file sementara kalau mau
        val tempFiles = requireContext().cacheDir.listFiles { file ->
            file.name.contains("compressed_image_") || file.name.contains("camera_compressed_")
        }
        tempFiles?.forEach { it.delete() }
    }
}