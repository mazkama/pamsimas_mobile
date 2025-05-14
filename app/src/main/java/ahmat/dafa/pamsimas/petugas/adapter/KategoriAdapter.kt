package ahmat.dafa.pamsimas.petugas.adapter

import KategoriBiaya
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ahmat.dafa.pamsimas.R
import android.util.Log

class KategoriAdapter(private val kategoriList: List<KategoriBiaya>) :
    RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    inner class KategoriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTarif: TextView = itemView.findViewById(R.id.tvTarif)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvTierFee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KategoriViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rincian_kategori, parent, false)
        return KategoriViewHolder(view)
    }

    override fun onBindViewHolder(holder: KategoriViewHolder, position: Int) {
        val kategori = kategoriList[position]
        Log.d("KategoriAdapter", "Binding Kategori: ${kategori.tarif}, ${kategori.volume}, ${kategori.subtotal}")

        // Menampilkan tarif, volume, dan subtotal
        holder.tvTarif.text = "${kategori.volume} m³ × Rp ${formatCurrency(kategori.tarif)}"
        holder.tvSubtotal.text = "Rp ${formatCurrency(kategori.subtotal)}"
    }

    fun formatCurrency(value: Int): String {
        return String.format("%,d", value).replace(',', '.')
    }

    override fun getItemCount(): Int = kategoriList.size
}
