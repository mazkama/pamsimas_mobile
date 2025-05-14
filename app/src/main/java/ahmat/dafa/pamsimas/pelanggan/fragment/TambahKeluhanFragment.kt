package ahmat.dafa.pamsimas.pelanggan.fragment

import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.UbahPasswordActivity
import ahmat.dafa.pamsimas.databinding.FragmentTambahPengaduanBinding
import ahmat.dafa.pamsimas.model.PemakaianStoreResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class TambahKeluhanFragment  : Fragment() {

    private lateinit var b: FragmentTambahPengaduanBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 200
    private var selectedImageFile: File? = null // Variabel untuk menyimpan file gambar yang dipilih
    private var isProcessing = false // Flag untuk mencegah duplikasi request
    private lateinit var dialog: Dialog
    lateinit var fdashboardpelanggan : BerandaPelangganFragment

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
                    0 -> openCamera() // Pilih untuk mengambil foto dengan kamera
                    1 -> openGallery() // Pilih untuk memilih gambar dari galeri
                }
            }
            builder.show()
        }

        b.btnSubmit.setOnClickListener {
            sendPemakaianStore()
        }

        b.ivPhoto.setOnClickListener {
            val bitmap = BitmapFactory.decodeFile(selectedImageFile?.absolutePath)
            b.fullscreenImageView.setImageBitmap(bitmap)
            b.previewOverlay.visibility = View.VISIBLE
        }


        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
        }

        return v
    }

    // Fungsi untuk membuka kamera
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    // Fungsi untuk membuka galeri
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    try {
                        val photo = data.extras?.get("data") as Bitmap
                        // Set image to ImageView
                        b.ivPhoto.setImageBitmap(photo)
                        // Ubah visibility ImageView dan placeholder
                        b.ivPhoto.visibility = View.VISIBLE
                        b.photoPlaceholderContent.visibility = View.GONE
                        // Menyimpan file foto
                        selectedImageFile = getImageFileFromBitmap(photo)
                        Log.d("PencatatanActivity", "Gambar dari kamera berhasil disimpan: ${selectedImageFile?.absolutePath}")
                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing camera image", e)
                        Toast.makeText(thisParent, "Gagal memproses foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    try {
                        val selectedImageUri = data.data
                        // Set image to ImageView directly from URI
                        b.ivPhoto.setImageURI(selectedImageUri)
                        // Ubah visibility ImageView dan placeholder
                        b.ivPhoto.visibility = View.VISIBLE
                        b.photoPlaceholderContent.visibility = View.GONE
                        selectedImageUri?.let {
                            selectedImageFile = getImageFileFromUri(it)
                            Log.d("PencatatanActivity", "Gambar dari galeri berhasil disimpan: ${selectedImageFile?.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e("PencatatanActivity", "Error processing gallery image", e)
                        Toast.makeText(thisParent, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Mengambil path gambar dari URI
    private fun getImageFileFromUri(uri: Uri): File? {
        try {
            val file = File(requireContext().cacheDir, "temp_image.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            Log.e("PencatatanFragment", "Error getting file from URI", e)
            Toast.makeText(requireContext(), "Gagal mengambil file gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    // Mengambil file dari Bitmap
    private fun getImageFileFromBitmap(bitmap: Bitmap): File {
        try {
            val file = File(requireContext().cacheDir, "temp_image.jpg")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            file.writeBytes(stream.toByteArray())
            return file
        } catch (e: Exception) {
            Log.e("PencatatanFragment", "Error getting file from bitmap", e)
            Toast.makeText(requireContext(), "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
            throw e
        }
    }

    // Fungsi untuk mengirim data ke API
    private fun sendPemakaianStore() {

        // Validasi input keterangan
        val keterangan = b.etComplaintDesc.text.toString()
        if (keterangan == null) {
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
        val keteranganBody = RequestBody.create("text/plain".toMediaType(), keterangan.toString())

        val fotoKeluhanPart = selectedImageFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("foto_keluhan", file.name, requestFile)
        }
        val token = sharedPreferences.getString("token", null)


        // Panggil API dengan Retrofit
        ApiClient.getInstance(thisParent,token).storeKeluhan(keteranganBody,fotoKeluhanPart)
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
        b.ivPhoto.setImageDrawable(null) // atau setImageResource

        // Hapus file sementara kalau mau
        val tempFile = File(requireContext().cacheDir, "temp_image.jpg")
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }



}