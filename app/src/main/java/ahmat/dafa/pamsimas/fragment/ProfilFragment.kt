package ahmat.dafa.pamsimas.fragment

import ahmat.dafa.pamsimas.R
import ahmat.dafa.pamsimas.LoginActivity
import ahmat.dafa.pamsimas.MainActivity
import ahmat.dafa.pamsimas.UbahPasswordActivity
import ahmat.dafa.pamsimas.databinding.FragmentMenuProfileBinding
import ahmat.dafa.pamsimas.petugas.LanjutBayarActivity
import ahmat.dafa.pamsimas.petugas.PencatatanActivity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File


class ProfilFragment : Fragment() {

    private lateinit var b: FragmentMenuProfileBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedImageFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisParent = activity as MainActivity
        b = FragmentMenuProfileBinding.inflate(inflater, container, false)
        v = b.root

        sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        val username = sharedPreferences.getString("username", null)
        val nama = sharedPreferences.getString("nama", null)
        val alamat = sharedPreferences.getString("alamat", null)
        val no_hp = sharedPreferences.getString("no_hp", null)
        val foto_profile = sharedPreferences.getString("foto_profile", null)
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

        b.tvName.text = nama
        b.tvUsername.text = username
        b.tvAddress.text = alamat
        b.tvPhone.text = no_hp

        b.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Logout") { dialog, _ ->
                    handleLogout() // Fungsi logout kamu
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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

        b.btnUbahPassword.setOnClickListener {
            val intent = Intent(thisParent, UbahPasswordActivity::class.java)
            startActivity(intent)
        }


        return v
    }

    private fun handleLogout() {
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all stored data
        editor.apply()

        // Redirect to LoginActivity after logout
        val intent = Intent(thisParent, LoginActivity::class.java)
        startActivity(intent)
        thisParent.finish() // Finish the current activity to prevent the user from returning to the logged-in screen
    }
}
