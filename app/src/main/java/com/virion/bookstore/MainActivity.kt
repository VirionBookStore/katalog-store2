package com.virion.bookstore

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val appUrl = "https://virionbookstore.github.io/katalog-store2/"

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else null
        fileCallback?.onReceiveValue(results)
        fileCallback = null
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (granted) request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        else request?.deny()
    }

    private var pendingPermissionRequest: PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("https://wa.me/") || url.startsWith("whatsapp://")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, request.url)) } catch (_: Exception) {}
                    return true
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = params?.createIntent() ?: return false
                return try {
                    filePicker.launch(intent)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                        pendingPermissionRequest = request
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    } else {
                        request.grant(request.resources)
                    }
                }
            }
        }

        // Mengatur logika tombol kembali (Back Button) agar aman dan memunculkan peringatan
        setupBackPressHandler()

        webView.loadUrl(appUrl)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                // Jika WebView masih punya riwayat halaman sebelumnya, kembali ke halaman dalam web
                webView.goBack()
            } else {
                // Jika sudah di halaman utama/terdepan, tampilkan dialog konfirmasi keluar
                showExitConfirmationDialog()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar Aplikasi")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi Virion Book?")
            .setCancelable(true)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                // Menutup aplikasi secara bersih
                finish()
            }
            .show()
    }
}

