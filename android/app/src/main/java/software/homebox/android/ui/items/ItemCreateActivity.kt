package software.homebox.android.ui.items

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.CreateEntityRequest
import software.homebox.android.databinding.ActivityItemCreateBinding

class ItemCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemCreateBinding

    private val scanBarcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.etSerial.setText(result.contents)
            Toast.makeText(this, "条形码已填入: ${result.contents}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnScanSerial.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt(getString(R.string.scanner_prompt))
                setBeepEnabled(true)
                setOrientationLocked(true)
            }
            scanBarcodeLauncher.launch(options)
        }

        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        val name = binding.etName.text.toString().trim()
        if (name.isBlank()) {
            binding.etName.error = getString(R.string.item_name_hint)
            return
        }

        val description = binding.etDescription.text.toString().trim().ifBlank { null }
        val quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 1
        val model = binding.etModel.text.toString().trim().ifBlank { null }
        val serial = binding.etSerial.text.toString().trim().ifBlank { null }
        val price = binding.etPrice.text.toString().toDoubleOrNull()

        binding.btnSave.isEnabled = false
        binding.pbLoading.visibility = View.VISIBLE

        val request = CreateEntityRequest(
            name = name,
            description = description,
            quantity = quantity,
            modelNumber = model,
            serialNumber = serial,
            purchasePrice = price
        )

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val response = withContext(Dispatchers.IO) {
                    service.createEntity(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@ItemCreateActivity, R.string.item_created_success, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ItemCreateActivity, "录入失败: " + response.message(), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemCreateActivity, "录入出错: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.pbLoading.visibility = View.GONE
            }
        }
    }
}
