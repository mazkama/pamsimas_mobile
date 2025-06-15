package ahmat.dafa.pamsimas.petugas.adapter

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import ahmat.dafa.pamsimas.databinding.ActivityPaymentBinding // Ganti dengan package name Anda

class PaymentGatewayAdapter : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupBackButton()

        // Contoh URL payment gateway - ganti dengan URL yang sesuai
        val paymentUrl = "https://dev.xen.to/WDDyoaCP"
        loadPaymentGateway(paymentUrl)
    }

    private fun setupWebView() {
        with(binding.webViewPayment) {
            // Mengaktifkan JavaScript
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            // Set WebView Client untuk handle loading dan redirect
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    showLoading(true)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    showLoading(false)

                    // Check jika pembayaran berhasil atau gagal berdasarkan URL
                    url?.let { checkPaymentStatus(it) }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    showLoading(false)
                    // Handle error - bisa tampilkan pesan error
                }
            }

            // Set WebChrome Client untuk handle JavaScript alerts dan progress
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress < 100) {
                        showLoading(true)
                    } else {
                        showLoading(false)
                    }
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            if (binding.webViewPayment.canGoBack()) {
                binding.webViewPayment.goBack()
            } else {
                finish()
            }
        }
    }

    private fun loadPaymentGateway(url: String) {
        binding.webViewPayment.loadUrl(url)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkPaymentStatus(url: String) {
        when {
            url.contains("success") || url.contains("settlement") -> {
                // Pembayaran berhasil
                handlePaymentSuccess()
            }
            url.contains("cancel") || url.contains("failure") -> {
                // Pembayaran dibatalkan atau gagal
                handlePaymentFailure()
            }
            url.contains("pending") -> {
                // Pembayaran pending
                handlePaymentPending()
            }
        }
    }

    private fun handlePaymentSuccess() {
        // Set result untuk activity sebelumnya
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun handlePaymentFailure() {
        // Set result untuk activity sebelumnya
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun handlePaymentPending() {
        // Handle pembayaran pending
        // Bisa redirect ke halaman status atau tetap di sini
    }

    override fun onBackPressed() {
        if (binding.webViewPayment.canGoBack()) {
            binding.webViewPayment.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
