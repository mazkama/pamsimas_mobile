package ahmat.dafa.pamsimas

import ahmat.dafa.pamsimas.databinding.ActivityUbahPasswordBinding
import ahmat.dafa.pamsimas.network.ApiClient
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UbahPasswordActivity : AppCompatActivity() {

    private lateinit var b: ActivityUbahPasswordBinding
    private lateinit var dialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUbahPasswordBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        dialog = Dialog(this)
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        b.btnBack.setOnClickListener {
            finish()
        }

        b.btnSave.setOnClickListener {
            val currentPassword = b.etOldPassword.text.toString().trim()
            val newPassword = b.etNewPassword.text.toString().trim()
            val confirmPassword = b.etConfirmPassword.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ubahPassword(currentPassword, newPassword, confirmPassword)
        }
    }

    private fun ubahPassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        dialog.show()
        val token = sharedPreferences.getString("token", null)
        if (token == null) {
            return
        }

        ApiClient.getInstance(this, token).ubahPassword(
            currentPassword,
            newPassword,
            confirmPassword
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                dialog.dismiss()
                if (response.isSuccessful) {
                    val responseString = response.body()?.string()
                    try {
                        val json = JSONObject(responseString)
                        val success = json.optBoolean("success")
                        val message = json.optString("message")

                        Toast.makeText(this@UbahPasswordActivity, message, Toast.LENGTH_SHORT).show()

                        if (success) {
                            finish()  // Kembali ke halaman sebelumnya jika sukses
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@UbahPasswordActivity, "Gagal memproses response", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorString = response.errorBody()?.string()
                    try {
                        val json = JSONObject(errorString)
                        val message = json.optString("message")
                        Toast.makeText(this@UbahPasswordActivity, "Password lama tidak sesuai", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@UbahPasswordActivity, "Gagal memproses error response", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@UbahPasswordActivity, "Terjadi kesalahan: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
