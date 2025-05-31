package ahmat.dafa.pamsimas.petugas.adapter

import TransaksiBayarResponse
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ItemKeluhanBinding
import ahmat.dafa.pamsimas.model.Keluhan
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.CetakActivity
import ahmat.dafa.pamsimas.petugas.DetailKeluhanActivity
import ahmat.dafa.pamsimas.petugas.LanjutBayarActivity
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class DataKeluhanAdapter (
    private var keluhanList: MutableList<Keluhan>,
    private val onItemClick: (Keluhan) -> Unit,
    private val onImageClick: (String?) -> Unit // Ubah parameter untuk pass URL asli
) : RecyclerView.Adapter<DataKeluhanAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemKeluhanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKeluhanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = keluhanList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = keluhanList[position]
        holder.binding.apply {
            txIdPengaduan.text = item.id_keluhan.toString()
            txKeterangan.text = item.keterangan
            txStatus.text = item.status
            txTanggapan.text = item.tanggapan ?: "-"

            // Format tanggal
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = try {
                val date = inputFormat.parse(item.tanggal)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                "-"  // fallback jika parsing gagal
            }

            txTanggalPengaduan.text = formattedDate

            val foto_keluhan = item.foto_keluhan
            if (!foto_keluhan.isNullOrEmpty() && foto_keluhan != "-") {
                // Konfigurasi Glide untuk kualitas tinggi
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache semua versi gambar
                    .placeholder(R.drawable.baseline_camera_alt_24)
                    .error(R.drawable.baseline_camera_alt_24)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // Gunakan ukuran asli
                    .dontTransform() // Jangan transform gambar untuk menjaga kualitas

                Glide.with(ivFoto.context)
                    .load(foto_keluhan)
                    .apply(requestOptions)
                    .into(ivFoto)
            } else {
                // Jika URL null, kosong, atau "-", set default image
                ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
            }

            // Set warna berdasarkan status
            val color = when (item.status) {
                "Terkirim" -> Color.GRAY
                "Dibaca" -> Color.parseColor("#FFA500")  // Oranye
                "Diproses" -> Color.GREEN
                else -> Color.TRANSPARENT  // default jika status tak dikenal
            }
            colorStatus.setBackgroundColor(color)

            // BAGIAN PEMBESARAN FOTO - Pass URL asli untuk kualitas maksimal
            ivFoto.setOnClickListener {
                if (!foto_keluhan.isNullOrEmpty() && foto_keluhan != "-") {
                    onImageClick(foto_keluhan) // Pass URL asli
                } else {
                    onImageClick(null)
                }
            }

            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, DetailKeluhanActivity::class.java)
                intent.putExtra("data_keluhan", item)
                context.startActivity(intent)
            }
        }
    }
}