package ahmat.dafa.pamsimas.pelanggan.fragment

import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.UbahPasswordActivity
import ahmat.dafa.pamsimas.databinding.FragmentBerandaPelangganBinding
import ahmat.dafa.pamsimas.model.Keluhan
import ahmat.dafa.pamsimas.model.PelangganBerandaResponse
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.model.UserResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.DataKeluhanActivity
import ahmat.dafa.pamsimas.petugas.DataRiwayatActivity
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BerandaPelangganFragment : Fragment() {

    private lateinit var b: FragmentBerandaPelangganBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private var isFirstLoad = true

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            val currentTime = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID"))
            b.tvCurrentDateTime.text = dateFormat.format(currentTime)

            handler.postDelayed(this, 1000) // Update setiap 1 detik
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisParent = activity as MainActivity
        b = FragmentBerandaPelangganBinding.inflate(inflater, container, false)
        v = b.root

        sharedPreferences =
            requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        val id_pelanggan = sharedPreferences.getString("id", null)
        val nama = sharedPreferences.getString("nama", null)
        b.tvCustomerName.text = "Halo, ${nama}" ?: "Tidak Ditemukan"
        b.tvCustomerId.text = "ID: PAMS ${id_pelanggan}" ?: "-"
        b.tvWaterStatus.text = "SUMBER SEHAT#${id_pelanggan ?: 0}"

        // Jalankan realtime jam
        startRealTimeClock()

        // Tampilkan data dari SharedPreferences terlebih dahulu
        showStoredDashboardData()

        if (isFirstLoad) {
            loadDashboardData()
            isFirstLoad = false
        }

        b.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
            fetchUserData()
        }

        b.btnUsageHistory.setOnClickListener {
            val intent = Intent(thisParent, DataRiwayatActivity::class.java)
            startActivity(intent)
        }

        b.btnComplaint.setOnClickListener {
            val intent = Intent(thisParent, DataKeluhanActivity::class.java)
            startActivity(intent)
        }

//        b.btnComplaint.setOnClickListener {
//            val intent = Intent(requireContext(), DataKeluhanActivity::class.java)
//            startActivity(intent)
//        }
//
//        b.btnUsageHistory.setOnClickListener {
//            val intent = Intent(requireContext(), DataRiwayatActivity::class.java)
//            startActivity(intent)
//        }

        return v
    }

    private fun startRealTimeClock() {
        handler.post(timeRunnable)
    }

    private fun loadDashboardData() {
        val token = sharedPreferences.getString("token", null)
        if (token.isNullOrEmpty()) {
            showError("Token tidak ditemukan.")
            b.swipeRefresh.isRefreshing = false
            return
        }

        ApiClient.getInstance(requireContext(), token).getDashboardPelanggan()
            .enqueue(object : retrofit2.Callback<PelangganBerandaResponse> {
                override fun onResponse(
                    call: retrofit2.Call<PelangganBerandaResponse>,
                    response: retrofit2.Response<PelangganBerandaResponse>
                ) {
                    if (!isAdded) return
                    b.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()!!.data
                        val transaksi = data.transaksi_terbaru
                        val keluhan = data.keluhan_terbaru

                        // Handle Transaksi
                        if (transaksi == null || transaksi.total_tagihan == null) {
                            b.tvBillAmount.text = "Rp -"
                            b.tvWaterUsage.text = "- m³"
                            b.tvWater.text = "- m³"
                            b.tvPaymentStatus.text = "-"
                            b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            b.tvMonth.text = "Tagihan Bulan Ini - -"
                        } else {
                            val formattedDate = try {
                                formatDate(transaksi.tanggal_pencatatan ?: "-")
                            } catch (e: Exception) {
                                "-"
                            }
                            b.tvMonth.text = "Tagihan Bulan Ini - $formattedDate"
                            b.tvBillAmount.text = "Rp ${formatCurrency(transaksi.total_tagihan ?: 0)}"
                            b.tvWaterUsage.text = "${transaksi.jumlah_pemakaian ?: "-"} m³"
                            b.tvWater.text = "${transaksi.meter_akhir ?: "-"} m³"

                            val statusPembayaran = transaksi.status_pembayaran ?: "-"
                            if (statusPembayaran != "-" && statusPembayaran.isNotEmpty()) {
                                b.tvPaymentStatus.text = "Lunas"
                                b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                            } else {
                                b.tvPaymentStatus.text = "Belum Lunas"
                                b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                            }
                        }

                        // Handle Keluhan
                        if (keluhan == null || keluhan.id_keluhan == null) {
                            b.cvPengaduan.visibility = View.GONE
                            b.tvNoData.visibility = View.VISIBLE
                        } else {
                            b.cvPengaduan.visibility = View.VISIBLE
                            b.tvNoData.visibility = View.GONE

                            val formattedKeluhanDate = try {
                                formatDate(keluhan.tanggal ?: "-")
                            } catch (e: Exception) {
                                "-"
                            }

                            b.txIdPengaduan.text = keluhan.id_keluhan.toString()
                            b.txTanggalPengaduan.text = "$formattedKeluhanDate"
                            b.txKeterangan.text = keluhan.keterangan ?: "-"
                            b.txTanggapan.text = keluhan.tanggapan ?: "-"
                            b.txStatus.text = keluhan.status ?: "-"

                            val fotoKeluhan = keluhan.foto_keluhan ?: "-"
                            if (fotoKeluhan.isNotEmpty() && fotoKeluhan != "-") {
                                Glide.with(b.ivFoto.context)
                                    .load(fotoKeluhan)
                                    .placeholder(R.drawable.baseline_camera_alt_24)
                                    .error(R.drawable.baseline_camera_alt_24)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(b.ivFoto)
                            } else {
                                b.ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
                            }

                            val color = when (keluhan.status) {
                                "Terkirim" -> Color.GRAY
                                "Dibaca" -> Color.parseColor("#FFA500")
                                "Diproses" -> Color.GREEN
                                else -> Color.TRANSPARENT
                            }
                            b.colorStatus.setBackgroundColor(color)
                        }

                        // Simpan ke SharedPreferences
                        saveDashboardDataToPreferences(transaksi, keluhan)

                    } else {
                        showError("Gagal mengambil data: ${response.message()}")
                    }
                }

                override fun onFailure(call: retrofit2.Call<PelangganBerandaResponse>, t: Throwable) {
                    if (!isAdded) return
                    b.swipeRefresh.isRefreshing = false
                    showError("Kesalahan jaringan: ${t.localizedMessage}")
                }
            })
    }

    // Fungsi util untuk format tanggal
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            "-"
        }
    }

    private fun saveDashboardDataToPreferences(
        transaksiTerbaru: Transaksi?,
        keluhanTerbaru: Keluhan?
    ) {

        val transaksiJson = transaksiTerbaru?.let {
            JSONObject().apply {
                put("id_pemakaian", it.id_pemakaian)
                put("nama_pelanggan", it.nama_pelanggan)
                put("tanggal_pencatatan", it.tanggal_pencatatan)
                put("meter_awal", it.meter_awal)
                put("meter_akhir",it.meter_akhir)
                put("jumlah_pemakaian",it.jumlah_pemakaian)
                put("denda", it.denda)
                put("total_tagihan",it.total_tagihan)
                put("status_pembayaran", it.status_pembayaran)
                put("foto_meteran", it.foto_meteran)
            }
        }

        val keluhanJson = keluhanTerbaru?.let {
            JSONObject().apply {
                put("id_keluhan", it.id_keluhan ?: "-")
                put("keterangan", it.keterangan ?: "-")
                put("tanggapan", it.tanggapan ?: "-")
                put("tanggal", it.tanggal ?: "-")
                put("status", it.status ?: "-")
                put("foto_keluhan", it.foto_keluhan ?: "-")
            }
        }

        val json = JSONObject().apply {
            put("transaksi_terbaru", transaksiJson)
            keluhanJson?.let { put("keluhan_terbaru", it) }
        }

        sharedPreferences.edit()
            .putString("dashboard_data", json.toString())
            .apply()
    }

    private fun showStoredDashboardData() {
        val dataJson = sharedPreferences.getString("dashboard_data", null) ?: return

        try {
            val json = JSONObject(dataJson)
            val transaksiTerbaru = json.optJSONObject("transaksi_terbaru")
            val keluhanTerbaru = json.optJSONObject("keluhan_terbaru")

            // === HANDLE TRANSAKSI TERBARU ===
            if (transaksiTerbaru == null || transaksiTerbaru.length() == 0) {
                b.tvBillAmount.text = "Rp -"
                b.tvWaterUsage.text = "- m³"
                b.tvWater.text = "- m³"
                b.tvPaymentStatus.text = "-"
                b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            } else {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                val formattedDate = transaksiTerbaru.optString("tanggal_pencatatan")?.let {
                    try {
                        inputFormat.parse(it)?.let { date -> outputFormat.format(date) } ?: "-"
                    } catch (e: Exception) {
                        "-"
                    }
                } ?: "-"

                b.tvMonth.text = "Tagihan Bulan Ini - $formattedDate"
                b.tvBillAmount.text = "Rp ${formatCurrency(transaksiTerbaru.optInt("total_tagihan", 0))}"
                b.tvWaterUsage.text = "${transaksiTerbaru.optString("jumlah_pemakaian", "-")} m³"
                b.tvWater.text = "${transaksiTerbaru.optString("meter_akhir", "-")} m³"

                val statusPembayaran = transaksiTerbaru.optString("status_pembayaran", "-")
                if (statusPembayaran != "-" && statusPembayaran.isNotEmpty()) {
                    b.tvPaymentStatus.text = "Lunas"
                    b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                } else {
                    b.tvPaymentStatus.text = "Belum Lunas"
                    b.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
            }

            // === HANDLE KELUHAN TERBARU ===
            if (keluhanTerbaru == null || keluhanTerbaru.length() == 0 || !keluhanTerbaru.has("id_keluhan")) {
                b.cvPengaduan.visibility = View.GONE
                b.tvNoData.visibility = View.VISIBLE
            } else {
                b.cvPengaduan.visibility = View.VISIBLE
                b.tvNoData.visibility = View.GONE

                val inputFormatKeluhan = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormatKeluhan = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                val formattedKeluhanDate = keluhanTerbaru.optString("tanggal")?.let {
                    try {
                        inputFormatKeluhan.parse(it)?.let { date -> outputFormatKeluhan.format(date) } ?: "-"
                    } catch (e: Exception) {
                        "-"
                    }
                } ?: "-"

                b.txIdPengaduan.text = keluhanTerbaru.optString("id_keluhan", "-")
                b.txTanggalPengaduan.text = "$formattedKeluhanDate"
                b.txKeterangan.text = keluhanTerbaru.optString("keterangan", "-")
                b.txTanggapan.text = keluhanTerbaru.optString("tanggapan", "-")
                b.txStatus.text = keluhanTerbaru.optString("status", "-")

                val fotoKeluhan = keluhanTerbaru.optString("foto_keluhan", "-")
                if (fotoKeluhan.isNotEmpty() && fotoKeluhan != "-") {
                    Glide.with(b.ivFoto.context)
                        .load(fotoKeluhan)
                        .placeholder(R.drawable.baseline_camera_alt_24)
                        .error(R.drawable.baseline_camera_alt_24)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(b.ivFoto)
                } else {
                    b.ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
                }

                val statusColor = when (keluhanTerbaru.optString("status", "-")) {
                    "Terkirim" -> Color.GRAY
                    "Dibaca" -> Color.parseColor("#FFA500") // Oranye
                    "Diproses" -> Color.GREEN
                    else -> Color.TRANSPARENT
                }
                b.colorStatus.setBackgroundColor(statusColor)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback jika parsing gagal
            b.tvBillAmount.text = "Rp -"
            b.tvWaterUsage.text = "- m³"
            b.tvWater.text = "- m³"
            b.tvPaymentStatus.text = "-"
            b.cvPengaduan.visibility = View.GONE
            b.tvNoData.visibility = View.VISIBLE
        }
    }

    fun fetchUserData() {
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Token tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.getInstance(thisParent, token).getUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    val data = userResponse

                    if (data != null) {
                        val editor = sharedPreferences.edit()
                        editor.putString("id", data.id_users)
                        editor.putString("nama", data.nama)
                        editor.putString("alamat", data.alamat)
                        editor.putString("no_hp", data.no_hp)
                        editor.putString("username", data.username)
                        editor.putString("role", data.role)
                        editor.putString("foto_profile", data.foto_profile)
                        editor.apply()

                        showStoredUserData()
                    }
                } else {
                    Toast.makeText(context, "Gagal perbarui data pengguna.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(context, "Kesalahan jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showStoredUserData() {
        val nama = sharedPreferences.getString("nama", null)
        b.tvCustomerName.text = "Halo, ${nama}" ?: "Tidak Ditemukan"
    }

    private fun showError(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}