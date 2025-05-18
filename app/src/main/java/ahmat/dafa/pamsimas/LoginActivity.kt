package ahmat.dafa.pamsimas

import ahmat.dafa.pamsimas.databinding.ActivityLoginBinding
import android.R.drawable
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        b.btnLogin.setOnClickListener {
            val username = b.edUsername.text.toString().trim()
            val password = b.edPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show()
            } else {
                b.progressBar.visibility = View.VISIBLE
                handleLogin(username, password)
            }
        }
        var isPasswordVisible = false

        b.togglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                b.edPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                b.togglePasswordVisibility.setImageResource(R.drawable.melek) // ganti sesuai drawable mata terbuka
            } else {
                b.edPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                b.togglePasswordVisibility.setImageResource(R.drawable.merem) // ganti sesuai drawable mata tertutup
            }

            // Agar cursor tetap di akhir teks
            b.edPassword.setSelection(b.edPassword.text?.length ?: 0)
        }
    }

    private fun handleLogin(username: String, password: String) {
        val url = "https://dev.airtenggerlor.biz.id/api/auth/login"

        val formBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    b.progressBar.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "Gagal koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)

                    val success = json.optBoolean("success", false)
                    val message = json.optString("message")

                    if (success) {
                        val data = json.getJSONObject("data")
                        val id = data.optString("id")
                        val nama = data.optString("nama")
                        val alamat = data.optString("alamat")
                        val no_hp = data.optString("no_hp")
                        val username = data.optString("username")
                        val role = data.optString("role")
                        val foto_profile = data.optString("foto_profile")
                        val token = json.optString("token", "") // Ambil token dari luar objek "data"

                        val editor = sharedPreferences.edit()
                        editor.putString("id", id)
                        editor.putString("nama", nama)
                        editor.putString("alamat", alamat)
                        editor.putString("no_hp", no_hp)
                        editor.putString("username", username)
                        editor.putString("role", role)
                        editor.putString("foto_profile", foto_profile)
                        editor.putString("token", token) // Simpan token juga
                        editor.apply()

                        Handler(Looper.getMainLooper()).post {
                            b.progressBar.visibility = View.GONE
                            redirectToDashboard()
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            b.progressBar.visibility = View.GONE
                            Toast.makeText(this@LoginActivity, "Username atau password salah !", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        b.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, "Username atau password salah !", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun redirectToDashboard() {
        val role = sharedPreferences.getString("role", "") ?: ""
        val intent = when (role) {
            "petugas", "pelanggan" -> Intent(this, MainActivity::class.java)
            else -> {
                Toast.makeText(this@LoginActivity, "Gagal login, role tidak tersedia", Toast.LENGTH_SHORT).show()
                return
            }
        }
        startActivity(intent)
        finish()
    }
}
