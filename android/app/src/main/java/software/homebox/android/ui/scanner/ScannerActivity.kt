package software.homebox.android.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import software.homebox.android.R
import software.homebox.android.databinding.ActivityScannerBinding
import software.homebox.android.ui.items.ItemDetailActivity

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var isScanned = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "需要相机权限以进行扫码", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanning() {
        binding.barcodeScannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && !isScanned) {
                    val text = result.text.trim()
                    if (text.isNotBlank()) {
                        isScanned = true
                        handleBarcodeResult(text)
                    }
                }
            }
        })
    }

    private fun handleBarcodeResult(code: String) {
        // Check if the barcode text contains an entity UUID or URL
        val uuidRegex = Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
        val match = uuidRegex.find(code)

        if (match != null) {
            val entityId = match.value
            val intent = Intent(this, ItemDetailActivity::class.java).apply {
                putExtra("extra_entity_id", entityId)
            }
            startActivity(intent)
            finish()
        } else {
            // Return barcode to caller or show toast
            val resultIntent = Intent().apply {
                putExtra("scanned_code", code)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "扫描到条码: $code", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScannerView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScannerView.pause()
    }
}
