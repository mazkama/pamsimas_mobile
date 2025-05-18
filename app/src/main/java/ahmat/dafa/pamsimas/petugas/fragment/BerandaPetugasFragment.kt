package ahmat.dafa.pamsimas.petugas.fragment

import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.FragmentBerandaPetugasBinding
import ahmat.dafa.pamsimas.model.PetugasBerandaResponse
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.model.UserResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.DataKeluhanActivity
import ahmat.dafa.pamsimas.petugas.DataRiwayatActivity
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class BerandaPetugasFragment : Fragment() {

    private lateinit var b: FragmentBerandaPetugasBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private var isFirstLoad = true

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            val currentTime = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID"))
            b.edTanggalJamD.text = dateFormat.format(currentTime)

            handler.postDelayed(this, 1000) // Update setiap 1 detik
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisParent = activity as MainActivity
        b = FragmentBerandaPetugasBinding.inflate(inflater, container, false)
        v = b.root

        sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        showStoredUserData()

        // Jalankan realtime jam
        startRealTimeClock()

        // Tampilkan data dari SharedPreferences terlebih dahulu
        showStoredDashboardData()

        if (isFirstLoad) {
            loadDashboardData()
            isFirstLoad = false
        }

        b.swipeRefresh.setOnRefreshListener {
            fetchUserData()
            loadDashboardData()
        }

        b.btnKeluhan.setOnClickListener {
            val intent = Intent(requireContext(), DataKeluhanActivity::class.java)
            startActivity(intent)
        }

        b.btnRiwayat.setOnClickListener {
            val intent = Intent(requireContext(), DataRiwayatActivity::class.java)
            startActivity(intent)
        }

        b.ivProfilePic.setOnClickListener {
            val fotoProfile = sharedPreferences.getString("foto_profile", null)
            if (!fotoProfile.isNullOrEmpty()) {
                val uri = Uri.parse(fotoProfile)
                Glide.with(requireContext())
                    .load(uri)
                    .into(b.fullscreenImageView)

                b.previewOverlay.visibility = View.VISIBLE
            } else {
                Toast.makeText(requireContext(), "Gambar belum tersedia.", Toast.LENGTH_SHORT).show()
            }
        }

        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
        }

        return v
    }

    private fun startRealTimeClock() {
        handler.post(timeRunnable)
    }

    private fun loadDashboardData() {
        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            showError("Token tidak ditemukan.")
            b.swipeRefresh.isRefreshing = false
            return
        }

        ApiClient.getInstance(requireContext(), token).getDashboardPetugas()
            .enqueue(object : retrofit2.Callback<PetugasBerandaResponse> {
                override fun onResponse(
                    call: retrofit2.Call<PetugasBerandaResponse>,
                    response: retrofit2.Response<PetugasBerandaResponse>
                ) {
                    if (isAdded) {
                        b.swipeRefresh.isRefreshing = false

                        if (response.isSuccessful && response.body()?.success == true) {
                            try {
                                val data = response.body()!!.data

                                b.tvTotalPengaduan.text = data.jumlah_keluhan?.toString() ?: "0"
                                b.tvTotalHarusCatat.text = data.total_harus_dicatat_bulan_ini?.toString() ?: "0"
                                b.tvTotalBelumCatat.text = data.total_belum_dicatat_bulan_ini?.toString() ?: "0"
                                b.tvTotalUang.text = formatCurrency(data.total_uang_bulan_ini ?: 0)

                                val transaksi = data.transaksi_terbaru
                                if (transaksi != null && transaksi.id_pemakaian != null) {
                                    b.cvPenugasan.visibility = View.VISIBLE
                                    b.tvNoData.visibility = View.GONE

                                    b.txIdPencatatan.text = "ID Pencatatan: ${transaksi.id_pemakaian}"
                                    b.txNamaPelanggan.text = "Nama: ${transaksi.nama_pelanggan ?: "-"}"
                                    b.txTanggalCatat.text = "Tanggal Catat: ${transaksi.tanggal_pencatatan ?: "-"}"
                                    b.txMeter.text = "Meter: ${transaksi.meter_awal ?: "-"} m³ - ${transaksi.meter_akhir ?: "-"} m³"
                                    b.txJumlahPemakaian.text = "Pemakaian: ${transaksi.jumlah_pemakaian ?: "-"} m³"
                                    b.txJumlahDenda.text = "Denda: Rp. ${formatCurrency(transaksi.denda ?: 0)}"
                                    b.txJumlahRp.text = "Jumlah Bayar: Rp. ${formatCurrency(transaksi.total_tagihan ?: 0)}"

                                    if (!transaksi.status_pembayaran.isNullOrEmpty() && transaksi.status_pembayaran != "-") {
                                        b.txStatusPembayaran.text = "Lunas"
                                        b.txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_lunas)
                                    } else {
                                        b.txStatusPembayaran.text = "Belum Lunas"
                                        b.txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_belum)
                                    }

                                    val fotoMeteran = transaksi.foto_meteran
                                    if (!fotoMeteran.isNullOrEmpty() && fotoMeteran != "-") {
                                        Glide.with(b.ivFoto.context)
                                            .load(fotoMeteran)
                                            .placeholder(R.drawable.baseline_camera_alt_24)
                                            .error(R.drawable.baseline_camera_alt_24)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(b.ivFoto)
                                    } else {
                                        b.ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
                                    }

                                } else {
                                    b.cvPenugasan.visibility = View.GONE
                                    b.tvNoData.visibility = View.VISIBLE
                                }

                                // Simpan ke SharedPreferences
                                saveDashboardDataToPreferences(
                                    data.jumlah_keluhan ?: 0,
                                    data.total_harus_dicatat_bulan_ini ?: 0,
                                    data.total_belum_dicatat_bulan_ini ?: 0,
                                    formatCurrency(data.total_uang_bulan_ini ?: 0),
                                    transaksi)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showError("Terjadi kesalahan saat memproses data.")
                            }
                        } else {
                            showError("Gagal mengambil data.")
                        }
                    }
                }

                override fun onFailure(call: retrofit2.Call<PetugasBerandaResponse>, t: Throwable) {
                    if (isAdded) {
                        b.swipeRefresh.isRefreshing = false
                        showError("Kesalahan jaringan: ${t.localizedMessage}")
                    }
                }
            })
    }

    private fun saveDashboardDataToPreferences(
        keluhan: Int,
        harusCatat: Int,
        belumCatat: Int,
        totalUang: String,
        transaksiTerbaru: Transaksi?
    ) {
        val transaksiJson = JSONObject().apply {
            // Hanya masukkan data jika transaksiTerbaru tidak null
            transaksiTerbaru?.let {
                putOpt("id_pemakaian", it.id_pemakaian)
                putOpt("nama_pelanggan", it.nama_pelanggan)
                putOpt("tanggal_pencatatan", it.tanggal_pencatatan)
                putOpt("meter_awal", it.meter_awal)
                putOpt("meter_akhir", it.meter_akhir)
                putOpt("jumlah_pemakaian", it.jumlah_pemakaian)
                putOpt("denda", it.denda)
                putOpt("total_tagihan", it.total_tagihan)
                putOpt("status_pembayaran", it.status_pembayaran)
                putOpt("foto_meteran", it.foto_meteran)
            }
        }

        val json = JSONObject().apply {
            put("keluhan", keluhan)
            put("harus_catat", harusCatat)
            put("belum_catat", belumCatat)
            put("total_uang", totalUang)
            put("transaksi_terbaru", transaksiJson)
        }

        sharedPreferences.edit()
            .putString("dashboard_data", json.toString())
            .apply()
    }

    private fun showStoredDashboardData() {
        val dataJson = sharedPreferences.getString("dashboard_data", null) ?: return
        try {
            val json = JSONObject(dataJson)
            val keluhan = json.optInt("keluhan", 0)
            val harusCatat = json.optInt("harus_catat", 0)
            val belumCatat = json.optInt("belum_catat", 0)
            val totalUang = json.optString("total_uang", "-")
            val transaksiTerbaru = json.optJSONObject("transaksi_terbaru")
            b.tvTotalPengaduan.text = keluhan.toString()

            if (transaksiTerbaru != null && transaksiTerbaru.has("id_pemakaian") && !transaksiTerbaru.isNull("id_pemakaian")) {
                b.cvPenugasan.visibility = View.VISIBLE
                b.tvNoData.visibility = View.GONE

                b.tvTotalHarusCatat.text = harusCatat.toString()
                b.tvTotalBelumCatat.text = belumCatat.toString()
                b.tvTotalUang.text = totalUang

                b.txIdPencatatan.text = "ID Pencatatan: ${transaksiTerbaru.optInt("id_pemakaian", 0)}"
                b.txNamaPelanggan.text = "Nama: ${transaksiTerbaru.optString("nama_pelanggan", "-")}"
                b.txTanggalCatat.text = "Tanggal Catat: ${transaksiTerbaru.optString("tanggal_pencatatan", "-")}"
                b.txMeter.text = "Meter: ${transaksiTerbaru.optString("meter_awal", "-")} m³ - ${transaksiTerbaru.optString("meter_akhir", "-")} m³"
                b.txJumlahPemakaian.text = "Pemakaian: ${transaksiTerbaru.optString("jumlah_pemakaian", "-")} m³"
                b.txJumlahDenda.text = "Denda: Rp. ${formatCurrency(transaksiTerbaru.optInt("denda", 0))}"
                b.txJumlahRp.text = "Jumlah Bayar: Rp. ${formatCurrency(transaksiTerbaru.optInt("total_tagihan", 0))}"

                val statusPembayaran = transaksiTerbaru.optString("status_pembayaran", "-")
                if (statusPembayaran != "-" && statusPembayaran.isNotEmpty()) {
                    b.txStatusPembayaran.text = "Lunas"
                    b.txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_lunas)
                } else {
                    b.txStatusPembayaran.text = "Belum Lunas"
                    b.txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_belum)
                }

                val fotoMeteran = transaksiTerbaru.optString("foto_meteran", "-")
                if (fotoMeteran != "-" && fotoMeteran.isNotEmpty()) {
                    Glide.with(b.ivFoto.context)
                        .load(fotoMeteran)
                        .placeholder(R.drawable.baseline_camera_alt_24)
                        .error(R.drawable.baseline_camera_alt_24)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(b.ivFoto)
                } else {
                    b.ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
                }
            } else {
                b.cvPenugasan.visibility = View.GONE
                b.tvNoData.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Jika terjadi error parsing JSON, tampilkan state kosong
            b.cvPenugasan.visibility = View.GONE
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
        val foto_profile = sharedPreferences.getString("foto_profile", null)
        val nama = sharedPreferences.getString("nama", null)
        b.tvNamaPetugas.text = nama ?: "Tidak Ditemukan"
        if (!foto_profile.isNullOrEmpty()) {
            Glide.with(this)
                .load(foto_profile)
                .placeholder(R.drawable.person_svgrepo_com) // gambar default saat loading
                .error(R.drawable.person_svgrepo_com) // gambar default kalau error
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(b.ivProfilePic)
        } else {
            // Jika foto_profile null atau kosong, pakai default
            b.ivProfilePic.setImageResource(R.drawable.person_svgrepo_com)
        }
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
