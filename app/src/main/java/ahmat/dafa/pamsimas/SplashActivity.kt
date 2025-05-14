package ahmat.dafa.pamsimas

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_splash)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        val nama = sharedPreferences.getString("nama", null)
        val username = sharedPreferences.getString("username", null)
        val role = sharedPreferences.getString("role", null)

        Handler(Looper.getMainLooper()).postDelayed({
            if (nama != null && username != null && role != null) {
                moveToDashboard()
            } else {
                // Jika belum login, langsung ke LoginActivity
                moveToLogin()
            }
        }, 1500)
    }

    private fun moveToDashboard() {
        val role = sharedPreferences.getString("role", "") ?: ""
        val intent = when (role) {
            "petugas", "pelanggan" -> Intent(this, MainActivity::class.java)
            else -> {
                Toast.makeText(this, "Gagal login, role tidak tersedia", Toast.LENGTH_SHORT).show()
                moveToLogin()
                return
            }
        }
        startActivity(intent)
        finish()
    }

    private fun moveToLogin() {
        // Jika belum login, langsung ke LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showUserDialog(nama: String, username: String, role: String) {
        AlertDialog.Builder(this)
            .setTitle("Selamat Datang, $nama!")
            .setMessage(
                "Username: $username\n" +
                        "Role: $role\n\n" +
                        "Apakah kamu ingin logout atau lanjutkan?"
            )
            .setPositiveButton("Logout") { _, _ ->
                // Hapus session
                sharedPreferences.edit().clear().apply()

                // Kembali ke login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
//            .setNegativeButton("Lanjutkan") { _, _ ->
//                // Masuk ke halaman utama
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()
//            }
            .setCancelable(false)
            .show()
    }
}
