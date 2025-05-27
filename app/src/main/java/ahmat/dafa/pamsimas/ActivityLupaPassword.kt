package ahmat.dafa.pamsimas

import ahmat.dafa.pamsimas.UbahPasswordActivity
import ahmat.dafa.pamsimas.databinding.ActivityLupaUsernamePasswordBinding
import ahmat.dafa.pamsimas.databinding.ActivityUbahPasswordBinding
import ahmat.dafa.pamsimas.model.ForgotPasswordRequest
import ahmat.dafa.pamsimas.model.ForgotPasswordResponse
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

class ActivityLupaPassword : AppCompatActivity() {

    private lateinit var b: ActivityLupaUsernamePasswordBinding
    private lateinit var dialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLupaUsernamePasswordBinding.inflate(layoutInflater)
        setContentView(b.root)

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        dialog = Dialog(this)
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)

        b.btnBack.setOnClickListener {
            finish()
        }

        b.btnSend.setOnClickListener {
            val nomorHp = b.etPhoneNumber.text.toString().trim()

            if (nomorHp.isEmpty()) {
                Toast.makeText(this, "Nomor hp wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lupaPassword(nomorHp)
        }
    }

    private fun lupaPassword(noHp: String) {
        val request = ForgotPasswordRequest(no_hp = "${noHp}")


        ApiClient.getInstance(this, "").forgotPassword(request).enqueue(object : Callback<ForgotPasswordResponse> {
            override fun onResponse(
                call: Call<ForgotPasswordResponse>,
                response: Response<ForgotPasswordResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        when {
                            !responseBody.success && responseBody.code == 404 -> {
                                Toast.makeText(this@ActivityLupaPassword, responseBody.message, Toast.LENGTH_LONG).show()
                            }
                            responseBody.success && responseBody.code == 200 -> {
                                Toast.makeText(this@ActivityLupaPassword, responseBody.message, Toast.LENGTH_LONG).show()
                                finish()
                            }
                            !responseBody.success && responseBody.code == 422 -> {
                                Toast.makeText(this@ActivityLupaPassword, "Validation error: ${responseBody.message}", Toast.LENGTH_LONG).show()
                            }
                            !responseBody.success && responseBody.code == 500 -> {
                                Toast.makeText(this@ActivityLupaPassword, "Server error: ${responseBody.message}", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@ActivityLupaPassword, responseBody.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@ActivityLupaPassword, "Response body kosong", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ActivityLupaPassword, "HTTP error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                Toast.makeText(this@ActivityLupaPassword, "Gagal koneksi: ${t.message}", Toast.LENGTH_LONG).show()
                t.printStackTrace()
            }
        })
    }
}
