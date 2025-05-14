package ahmat.dafa.pamsimas.petugas.fragment

import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.MyCaptureActivity
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.petugas.adapter.DataPemakaianAdapter
import ahmat.dafa.pamsimas.databinding.FragmentDataPemakaianBinding
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.model.PemakaianResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class DataPemakaianFragment : Fragment() {

    private lateinit var b: FragmentDataPemakaianBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var pemakaianAdapter: DataPemakaianAdapter
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>
    private lateinit var pencatatanLauncher: ActivityResultLauncher<Intent>
    private val pemakaianList = mutableListOf<Pemakaian>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private lateinit var dialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences
    private var searchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentDataPemakaianBinding.inflate(inflater, container, false)
        v = b.root

        sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        dialog = Dialog(thisParent)
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        refreshData()

        setupRecyclerView()
        setupListeners()
        fetchPemakaianData("")

        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val scannedValue = result.contents
                if (scannedValue.matches(Regex("^\\d+$"))) {
                    Toast.makeText(thisParent, "Scan Pelanggan ID#$scannedValue", Toast.LENGTH_SHORT).show()
                    fetchPemakaianById(scannedValue)
                } else {
                    Toast.makeText(thisParent, "QR tidak valid (tidak sesuai format ID Pelanggan)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        pencatatanLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshData()
            }
        }


        b.btnScan.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(thisParent, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    startQRCodeScanner()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    Toast.makeText(thisParent, "Aplikasi membutuhkan akses kamera untuk scan QR", Toast.LENGTH_SHORT).show()
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        return v
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startQRCodeScanner()
        } else {
            Toast.makeText(thisParent, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startQRCodeScanner() {
        val options = ScanOptions().apply {
            setPrompt("Scan QR Code")
            setBeepEnabled(true)
            setOrientationLocked(true) // Kunci ke portrait
            captureActivity = MyCaptureActivity::class.java // Gunakan custom activity
        }
        barcodeLauncher.launch(options)
    }

    private fun setupRecyclerView() {
        pemakaianAdapter = DataPemakaianAdapter(pemakaianList) { pemakaian ->
            // Aksi saat item diklik
            val intent = Intent(thisParent, PencatatanActivity::class.java)
            intent.putExtra("data_pemakaian", pemakaian)
            pencatatanLauncher.launch(intent)
        }
        with(b.rvPelanggan) {
            layoutManager = LinearLayoutManager(thisParent)
            adapter = pemakaianAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    if (!isLoading && !isLastPage && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                        fetchPemakaianData(b.edCari.text.toString())
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        with(b) {
            swipeRefreshLayout.setOnRefreshListener {
                swipeRefreshLayout.isRefreshing = true
                refreshData()
            }
            edCari.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    searchRunnable?.let { handler.removeCallbacks(it) }
                    searchRunnable = Runnable { refreshData() }
                    handler.postDelayed(searchRunnable!!, 500) // 500ms debounce
                }
            })
        }
    }

    private fun fetchPemakaianById(id: String) {
        dialog.show()
        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            Toast.makeText(thisParent, "Token tidak ditemukan.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        ApiClient.getInstance(thisParent,token).getPemakaianById(id.toInt()).enqueue(object : Callback<PemakaianResponse> {
            override fun onResponse(call: Call<PemakaianResponse>, response: Response<PemakaianResponse>) {
                dialog.dismiss()

                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (!data?.data.isNullOrEmpty()) {
                        val item = data.data[0]
                        if (item.sudah_dicatat_bulan_ini == true) {
                            Toast.makeText(thisParent, "Sudah tercatat pada bulan ini", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }else{
                            val intent = Intent(context, PencatatanActivity::class.java)
                            intent.putExtra("data_pemakaian", item) // langsung kirim objek
                            startActivity(intent)
                        }
                    }else {
                        Toast.makeText(thisParent, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (response.code() == 403) {
                        Toast.makeText(thisParent, "Tidak memiliki akses pada pelanggan.", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(
                            thisParent,
                            "Gagal mengambil data (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<PemakaianResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(thisParent, "Kesalahan jaringan: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPemakaianData(keyword: String) {
        if (isLoading) return
        isLoading = true
        b.swipeRefreshLayout.isRefreshing = true

        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            Toast.makeText(thisParent, "Token tidak ditemukan.", Toast.LENGTH_SHORT).show()
            isLoading = false
            b.swipeRefreshLayout.isRefreshing = false
            return
        }

        ApiClient.getInstance(thisParent,token).getPemakaian(keyword, currentPage).enqueue(object :
            Callback<PemakaianResponse> {
            override fun onResponse(call: Call<PemakaianResponse>, response: Response<PemakaianResponse>) {
                isLoading = false
                b.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val data = response.body()?.data?.data ?: emptyList()
                    Log.d("PencatatanFragment", "Jumlah data: ${data.size}")

                    if (currentPage == 1) pemakaianList.clear()
                    pemakaianList.addAll(data)
                    pemakaianAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= (response.body()?.data?.last_page ?: 0)
                    if (!isLastPage) currentPage++

                    b.tvNoData.visibility = if (pemakaianList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    if (response.code() == 403) {
                        Toast.makeText(thisParent, "Tidak memiliki akses pada pelanggan.", Toast.LENGTH_SHORT).show()
                        pemakaianList.clear()
                        pemakaianAdapter.notifyDataSetChanged()
                        b.tvNoData.visibility = View.VISIBLE
                    } else {
                        Log.e("PencatatanFragment", "Gagal: ${response.code()} - ${response.message()}")

                        // Debugging error body
                        val errorBody = response.errorBody()?.string()
                        Log.e("PencatatanFragment", "Error body: $errorBody")
                    }
                }
            }

            override fun onFailure(call: Call<PemakaianResponse>, t: Throwable) {
                isLoading = false
                b.swipeRefreshLayout.isRefreshing = false
                if (currentPage == 1) b.tvNoData.visibility = View.VISIBLE
                Log.e("PencatatanFragment", "Gagal: ${t.message}")
            }
        })
    }

    private fun refreshData() {
        currentPage = 1
        isLastPage = false
        b.tvNoData.visibility = View.GONE
        fetchPemakaianData(b.edCari.text.toString())
    }
}
