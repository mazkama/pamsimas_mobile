package ahmat.dafa.pamsimas.petugas

import KategoriBiaya
import TransaksiBayarResponse
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ActivityCetakBinding
import ahmat.dafa.pamsimas.databinding.ActivityDetailKeluhanBinding
import ahmat.dafa.pamsimas.model.Keluhan
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.petugas.adapter.KategoriAdapter
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class DetailKeluhanActivity : AppCompatActivity() {

    private lateinit var b: ActivityDetailKeluhanBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetailKeluhanBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        val detaKeluhan = intent.getSerializableExtra("data_keluhan") as? Keluhan


        detaKeluhan?.let { keluhan ->

            b.tvComplaintId.text = "${keluhan.id_keluhan ?: "-"}"
            b.tvComplaintDate.text = "${keluhan.tanggal ?: "-"}"
            b.tvComplaintStatus.text = "${keluhan.status ?: "-"}"
            b.tvReporterName.text = "${keluhan.nama_pelapor ?: "-"}"
            b.tvReporterPhone.text = "${keluhan.no_hp ?: "-"}"
            b.tvComplaintDescription.text = "${keluhan.keterangan ?: "-"}"
            b.tvResponseContent.text = "${keluhan.tanggapan ?: "-"}"

            // Set warna berdasarkan status
            val color = when (keluhan.status) {
                "Terkirim" -> Color.GRAY
                "Dibaca" -> Color.parseColor("#FFA500")  // Oranye
                "Diproses" -> Color.GREEN
                else -> Color.TRANSPARENT  // default jika status tak dikenal
            }
            b.tvComplaintStatus.setBackgroundColor(color)

            val foto_keluhan = keluhan.foto_keluhan
            if (!foto_keluhan.isNullOrEmpty() && foto_keluhan != "-") {
                // Konfigurasi Glide untuk kualitas tinggi pada ImageView biasa
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.baseline_camera_alt_24)
                    .error(R.drawable.baseline_camera_alt_24)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                    .dontTransform()

                Glide.with(b.ivComplaintPhoto.context)
                    .load(foto_keluhan)
                    .apply(requestOptions)
                    .into(b.ivComplaintPhoto)
            } else {
                // Jika URL null, kosong, atau "-", set default image
                b.ivComplaintPhoto.setImageResource(R.drawable.baseline_camera_alt_24)
            }

            b.ivComplaintPhoto.setOnClickListener {
                if (!foto_keluhan.isNullOrEmpty() && foto_keluhan != "-") {
                    // Konfigurasi Glide untuk kualitas tinggi pada fullscreen
                    val fullscreenRequestOptions = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.baseline_camera_alt_24)
                        .error(R.drawable.baseline_camera_alt_24)
                        .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                        .dontTransform()

                    Glide.with(this)
                        .load(foto_keluhan) // Gunakan URL asli, bukan drawable
                        .apply(fullscreenRequestOptions)
                        .into(b.fullscreenImageView)

                    b.previewOverlay.visibility = View.VISIBLE
                }
            }

            b.closePreviewButton.setOnClickListener {
                b.previewOverlay.visibility = View.GONE
            }
        }

        b.btnBack.setOnClickListener {
            finish()
        }

    }
}