package ahmat.dafa.pamsimas.petugas.adapter

import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.databinding.ItemPelangganBinding
import ahmat.dafa.pamsimas.model.Pemakaian
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import ahmat.dafa.pamsimas.utils.QRCodeHelper
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DataPemakaianAdapter(
    private var pemakaianList: MutableList<Pemakaian>,
    private val onItemClick: (Pemakaian) -> Unit
) : RecyclerView.Adapter<DataPemakaianAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPelangganBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPelangganBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = pemakaianList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pemakaianList[position]
        holder.binding.apply {
            txIdPelangganValue.text = item.id_users.toString()
            txNamaPelangganValue.text = item.nama
            txNomorHpValue.text = item.no_hp
            txAlamatPelangganValue.text = "${item.alamat},"
            txRt.text = " RT ${item.rt}"
            txRw.text = " RW ${item.rw}"
            txJumlahPemakaianValue.text = "${item.meter_akhir} m³" ?: "${item.jumlah_air} m³"

            val qrBitmap = QRCodeHelper.generateQRCode(item.id_users.toString())
            if (qrBitmap != null) {
                qrImageView.setImageBitmap(qrBitmap)
            }

            if(item.sudah_dicatat_bulan_ini == true){
                btnCatat.isEnabled = false
                btnCatat.setBackgroundColor(Color.GRAY)
            }else{
                btnCatat.isEnabled = true
                btnCatat.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
            }

            // Event klik item
            btnCatat.setOnClickListener {
                onItemClick(item)
            }
        }
    }

}
