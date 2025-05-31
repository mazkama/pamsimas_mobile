package ahmat.dafa.pamsimas.petugas

import KategoriBiaya
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ActivityRiwayatKeluhanBinding
import ahmat.dafa.pamsimas.model.Keluhan
import ahmat.dafa.pamsimas.model.KeluhanResponse
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.adapter.DataKeluhanAdapter
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataKeluhanActivity : AppCompatActivity() {

    private lateinit var b: ActivityRiwayatKeluhanBinding
    private lateinit var dialog: Dialog
    private lateinit var keluhanAdapter: DataKeluhanAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val keluhanList = mutableListOf<Keluhan>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var searchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRiwayatKeluhanBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        refreshData()

        setupRecyclerView()
        setupListeners()
        fetchKeluhanData("")

        b.closePreviewButton.setOnClickListener {
            b.previewOverlay.visibility = View.GONE
            b.cardSearch.setVisibility(View.VISIBLE);
        }

        b.btnKembali.setOnClickListener {
            finish()
        }

    }

    private fun setupRecyclerView() {
        keluhanAdapter = DataKeluhanAdapter(
            keluhanList,
            onItemClick = { Keluhan ->
                // Aksi saat item diklik
            },
            onImageClick = { imagePath ->
                if (imagePath != null) {
                    // Konfigurasi Glide untuk kualitas tinggi seperti di adapter
                    val requestOptions = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache semua versi gambar
                        .placeholder(R.drawable.baseline_camera_alt_24)
                        .error(R.drawable.baseline_camera_alt_24)
                        .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // Gunakan ukuran asli
                        .dontTransform() // Jangan transform gambar untuk menjaga kualitas

                    Glide.with(this)
                        .load(imagePath)
                        .apply(requestOptions)
                        .into(b.fullscreenImageView)

                    b.previewOverlay.visibility = View.VISIBLE
                    b.cardSearch.setVisibility(View.GONE);
                }
            }
        )
        with(b.rvKeluhan) {
            layoutManager = LinearLayoutManager(this@DataKeluhanActivity)
            adapter = keluhanAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    if (!isLoading && !isLastPage && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                        fetchKeluhanData(b.edCari.text.toString())
                    }
                }
            })
        }
    }

    private fun fetchKeluhanData(keyword: String) {
        if (isLoading) return
        isLoading = true
        b.swipeRefreshLayout.isRefreshing = true

        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            b.swipeRefreshLayout.isRefreshing = false
            return
        }

        ApiClient.getInstance(this,token).getKeluhan(keyword, currentPage).enqueue(object :
            Callback<KeluhanResponse> {
            override fun onResponse(call: Call<KeluhanResponse>, response: Response<KeluhanResponse>) {
                isLoading = false
                b.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val data = response.body()?.data?.data?: emptyList()
                    Log.d("dataKeluhan", "Jumlah data: ${data.size}")

                    if (currentPage == 1) keluhanList.clear()
                    keluhanList.addAll(data)
                    keluhanAdapter.notifyDataSetChanged()

                    isLastPage = currentPage >= (response.body()?.data?.last_page ?: 0)
                    if (!isLastPage) currentPage++

                    b.tvNoData.visibility = if (keluhanList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.e("keluhan data", "Gagal: ${response.code()} - ${response.message()}")
                }
            }


            override fun onFailure(call: Call<KeluhanResponse>, t: Throwable) {
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
        fetchKeluhanData(b.edCari.text.toString())
    }

}