package ahmat.dafa.pamsimas

import ahmat.dafa.pamsimas.databinding.ActivityMainBinding
import ahmat.dafa.pamsimas.fragment.ProfilFragment
import ahmat.dafa.pamsimas.pelanggan.fragment.BerandaPelangganFragment
import ahmat.dafa.pamsimas.pelanggan.fragment.TambahKeluhanFragment
import ahmat.dafa.pamsimas.petugas.fragment.BerandaPetugasFragment
import ahmat.dafa.pamsimas.petugas.fragment.DataPemakaianFragment
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var b: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    lateinit var fdashboardpetugas : BerandaPetugasFragment
    lateinit var fdashboardpelanggan : BerandaPelangganFragment
    lateinit var fpencatatanpetugas : DataPemakaianFragment
    lateinit var ftambahpengaduan : TambahKeluhanFragment
    lateinit var fprofil : ProfilFragment
    lateinit var ft : FragmentTransaction
    lateinit var role : String

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Paksa tema terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        role = sharedPreferences.getString("role", "") ?: ""

        // Cek role dan ubah title jika role = pelanggan
        if (role == "pelanggan") {
            val menuItem = b.bottomNavigationView.menu.findItem(R.id.nav_pencatatan)
            menuItem.title = "Pengaduan"
        }

        b.bottomNavigationView.setOnItemSelectedListener(this)

        fdashboardpetugas = BerandaPetugasFragment()
        fdashboardpelanggan = BerandaPelangganFragment()
        fpencatatanpetugas = DataPemakaianFragment()
        ftambahpengaduan = TambahKeluhanFragment()
        fprofil = ProfilFragment()

        b.bottomNavigationView.setSelectedItemId(R.id.nav_dashboard)

        ft = supportFragmentManager.beginTransaction()
        val selectedFragment = if (role == "petugas") fdashboardpetugas else fdashboardpelanggan
        ft.replace(R.id.frameLayout, selectedFragment).commit()
        b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
        b.frameLayout.visibility = View.VISIBLE

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        ft = supportFragmentManager.beginTransaction()

        val selectedFragment = when (item.itemId) {
            R.id.nav_dashboard -> if (role == "petugas") fdashboardpetugas else fdashboardpelanggan
            R.id.nav_pencatatan -> if (role == "petugas") fpencatatanpetugas else ftambahpengaduan
            R.id.nav_profile -> fprofil
            else -> null
        }

        selectedFragment?.let {
            ft.replace(R.id.frameLayout, it).commit()
            b.frameLayout.setBackgroundColor(Color.argb(255, 255, 255, 255))
            b.frameLayout.visibility = View.VISIBLE
        }

        return true
    }

    override fun onStart() {
        super.onStart()

        // Memeriksa izin untuk notifikasi hanya di Android 13 dan lebih baru
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Jika izin belum diberikan, minta izin
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // Memeriksa dan meminta izin kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100 // kode request bebas, gunakan nilai unik
            )
        }
    }

}