package ahmat.dafa.pamsimas.petugas

import ahmat.dafa.pamsimas.databinding.ActivityRiwayatBinding
import ahmat.dafa.pamsimas.databinding.ActivityRiwayatKeluhanBinding
import ahmat.dafa.pamsimas.model.Keluhan
import ahmat.dafa.pamsimas.model.KeluhanResponse
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.model.TransaksiResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.adapter.DataRiwayatAdapter
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataRiwayatActivity : AppCompatActivity() {

    private lateinit var b: ActivityRiwayatBinding
    private lateinit var dialog: Dialog
    private lateinit var riwayatAdapter: DataRiwayatAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val riwayatList = mutableListOf<Transaksi>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var searchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        refreshData()

        setupRecyclerView()
        setupListeners()
        fetchRiwayatData("")

        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
            b.cardSearch.setVisibility(View.VISIBLE);
        }

        b.btnKembali.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        riwayatAdapter = DataRiwayatAdapter(
            riwayatList,
            onItemClick = { riwayat ->
                // Aksi saat item diklik

            },
            onImageClick = { imagePath ->
                if (imagePath != null) {
                    Glide.with(this)
                        .load(imagePath)
                        .into(b.fullscreenImageView)
                    b.previewOverlay.visibility = View.VISIBLE
                    b.cardSearch.setVisibility(View.GONE);
                }
            }
        )

        with(b.rvPelanggan) {
            layoutManager = LinearLayoutManager(this@DataRiwayatActivity)
            adapter = riwayatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    if (!isLoading && !isLastPage && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                        fetchRiwayatData(b.edCari.text.toString())
                    }
                }
            })
        }
    }


    private fun fetchRiwayatData(keyword: String) {
        if (isLoading) return
        isLoading = true
        b.swipeRefreshLayout.isRefreshing = true
        val token = sharedPreferences.getString("token", null)

        ApiClient.getInstance(this, token).getTransaksi(keyword, currentPage).enqueue(object :
            Callback<TransaksiResponse> {
            override fun onResponse(call: Call<TransaksiResponse>, response: Response<TransaksiResponse>) {
                isLoading = false
                b.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val data = response.body()?.data?.data ?: emptyList()
                    Log.d("dataRiwayat", "Jumlah data: ${data.size}")

                    if (currentPage == 1) riwayatList.clear()
                    riwayatList.addAll(data)
                    riwayatAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= (response.body()?.data?.last_page ?: 0)
                    if (!isLastPage) currentPage++

                    b.tvNoData.visibility = if (riwayatList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.e("riwayat data", "Gagal: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<TransaksiResponse>, t: Throwable) {
                isLoading = false
                b.swipeRefreshLayout.isRefreshing = false
                if (currentPage == 1) b.tvNoData.visibility = View.VISIBLE
            }
        })
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

    private fun refreshData() {
        currentPage = 1
        isLastPage = false
        b.tvNoData.visibility = View.GONE
        fetchRiwayatData(b.edCari.text.toString())
    }
}
