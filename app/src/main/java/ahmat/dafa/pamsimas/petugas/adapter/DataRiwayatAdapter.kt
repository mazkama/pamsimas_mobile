package ahmat.dafa.pamsimas.petugas.adapter

import TransaksiBayarResponse
import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ItemRiwayatBinding
import ahmat.dafa.pamsimas.model.Transaksi
import ahmat.dafa.pamsimas.network.ApiClient
import ahmat.dafa.pamsimas.petugas.CetakActivity
import ahmat.dafa.pamsimas.utils.CurrencyHelper.formatCurrency
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Response

class DataRiwayatAdapter(
    private var transaksiList: MutableList<Transaksi>,
    private val onItemClick: (Transaksi) -> Unit,
    private val onImageClick: (String?) -> Unit // Ubah parameter untuk pass URL asli
) : RecyclerView.Adapter<DataRiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Transaksi) {
            binding.apply {
                txIdPencatatan.text = "ID Pencatatan: ${item.id_pemakaian.toString() }"
                txNamaPelanggan.text = "Nama: ${item.nama_pelanggan}"
                txTanggalCatat.text = "Tanggal Catat: ${item.tanggal_pencatatan ?: " - "}"
                txMeter.text = "Meter: ${item.meter_awal ?: "-"} m³ - ${item.meter_akhir ?: "-"} m³"
                txJumlahPemakaian.text = "Pemakaian: ${item.jumlah_pemakaian ?: "-"} m³"
                txJumlahDenda.text = "Denda: Rp. ${formatCurrency(( item.denda)) ?: 0}"
                txJumlahRp.text = "Jumlah Bayar: Rp. ${formatCurrency(( item.total_tagihan)) ?: 0}"

                if (!item.status_pembayaran.isNullOrEmpty() && item.status_pembayaran != "-") {
                    txStatusPembayaran.text = "Lunas"
                    txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_lunas)
                } else {
                    txStatusPembayaran.text = "Belum Lunas"
                    txStatusPembayaran.setBackgroundResource(R.drawable.bg_status_pembayaran_belum)
                }

                val foto_meteran = item.foto_meteran
                if (!foto_meteran.isNullOrEmpty() && foto_meteran != "-") {
                    // Konfigurasi Glide untuk kualitas tinggi
                    val requestOptions = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache semua versi gambar
                        .placeholder(R.drawable.baseline_camera_alt_24)
                        .error(R.drawable.baseline_camera_alt_24)
                        .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // Gunakan ukuran asli
                        .dontTransform() // Jangan transform gambar untuk menjaga kualitas

                    Glide.with(ivFoto.context)
                        .load(foto_meteran)
                        .apply(requestOptions)
                        .into(ivFoto)
                } else {
                    // Jika URL null, kosong, atau "-", set default image
                    ivFoto.setImageResource(R.drawable.baseline_camera_alt_24)
                }

                // BAGIAN PEMBESARAN FOTO - Pass URL asli untuk kualitas maksimal
                ivFoto.setOnClickListener {
                    if (!foto_meteran.isNullOrEmpty() && foto_meteran != "-") {
                        onImageClick(foto_meteran) // Pass URL asli
                    } else {
                        onImageClick(null)
                    }
                }

                root.setOnClickListener {
                    // Cek status transaksi
                    val isLunas = item.status_pembayaran == "Lunas"

                    if (isLunas) {
                        val dialog = Dialog(root.context).apply {
                            setContentView(R.layout.progress_dialog)
                            setCancelable(false)
                        }

                        // Tampilkan dialog saat mulai request
                        dialog.show()
                        val sharedPreferences = root.context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        val token = sharedPreferences.getString("token", null)

                        if (token.isNullOrEmpty()) {
                            Toast.makeText(root.context, "Token tidak ditemukan. Silakan login ulang.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        ApiClient.getInstance(root.context, token).getDetailTransaksi(item.id_transaksi.toString()).enqueue(object : retrofit2.Callback<TransaksiBayarResponse> {
                            override fun onResponse(call: Call<TransaksiBayarResponse>, response: Response<TransaksiBayarResponse>) {
                                dialog.dismiss()  // Tutup dialog
                                if (response.isSuccessful && response.body() != null) {
                                    val data = response.body()

                                    // Kirim ke CetakActivity
                                    val intent = Intent(root.context, CetakActivity::class.java)
                                    intent.putExtra("data_transaksi", data)  // Kirim sebagai JSON string
                                    root.context.startActivity(intent)
                                } else {
                                    Toast.makeText(root.context, "Gagal mengambil data transaksi.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<TransaksiBayarResponse>, t: Throwable) {
                                dialog.dismiss()  // Tutup dialog
                                Toast.makeText(root.context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
//                        // Belum lunas: Intent ke PaymentGatewayActivity
//                        val intent = Intent(root.context, PaymentGatewayAdapter::class.java)
//                        Log.d("DataTransaksiKirim", item.toString())
//                        intent.putExtra("data_transaksi", item)
//                        root.context.startActivity(intent)
//                        Toast.makeText(root.context, "Transaksi belum dilakukan pembayaran.", Toast.LENGTH_SHORT).show()
                        // Belum lunas
                        Toast.makeText(root.context, "Silahkan Bayar Di Kantor PAMSIMAS.", Toast.LENGTH_SHORT).show()
//
                    }
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = transaksiList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transaksiList[position])
    }

    fun setData(newData: List<Transaksi>) {
        transaksiList.clear()
        transaksiList.addAll(newData)
        notifyDataSetChanged()
    }
}