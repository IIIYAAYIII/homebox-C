package software.homebox.android.ui.items

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.CreateEntityRequest
import software.homebox.android.databinding.ActivityItemCreateBinding
import software.homebox.android.ui.photo.BitmapUtils
import software.homebox.android.ui.photo.PhotoEditorActivity
import java.io.File

class ItemCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemCreateBinding

    private var currentPhotoPath: String? = null
    private var cameraTempUri: Uri? = null

    private val scanBarcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.etSerial.setText(result.contents)
            Toast.makeText(this, "条形码已填入: ${result.contents}", Toast.LENGTH_SHORT).show()
        }
    }

    private val editPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedPath = result.data?.getStringExtra("extra_photo_path")
            if (!editedPath.isNullOrBlank()) {
                currentPhotoPath = editedPath
                renderPhotoPreview(editedPath)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraTempUri
        if (success && uri != null) {
            openPhotoEditor(uri)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            openPhotoEditor(uri)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "需要相机权限以拍摄物品照片", Toast.LENGTH_SHORT).show()
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

        binding.btnAddPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        binding.btnEditPhoto.setOnClickListener {
            currentPhotoPath?.let { path ->
                openPhotoEditor(Uri.fromFile(File(path)))
            }
        }

        binding.btnRemovePhoto.setOnClickListener {
            currentPhotoPath = null
            binding.layoutPhotoPreview.visibility = View.GONE
            binding.btnAddPhoto.visibility = View.VISIBLE
        }

        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf(
            getString(R.string.photo_source_camera),
            getString(R.string.photo_source_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.photo_select_source)
            .setItems(options) { _, which ->
                if (which == 0) {
                    checkCameraPermissionAndLaunch()
                } else {
                    pickPhotoLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val cacheImagesDir = File(cacheDir, "camera").apply { mkdirs() }
            val tempFile = File(cacheImagesDir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", tempFile)
            cameraTempUri = uri
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "启动相机失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhotoEditor(uri: Uri) {
        val intent = Intent(this, PhotoEditorActivity::class.java).apply {
            putExtra("extra_image_uri", uri)
        }
        editPhotoLauncher.launch(intent)
    }

    private fun renderPhotoPreview(path: String) {
        binding.btnAddPhoto.visibility = View.GONE
        binding.layoutPhotoPreview.visibility = View.VISIBLE

        Glide.with(this)
            .load(File(path))
            .centerCrop()
            .into(binding.ivPhotoThumbnail)

        val file = File(path)
        val sizeStr = BitmapUtils.formatFileSize(file.length())
        binding.tvPhotoInfo.text = "已处理优化照片: $sizeStr"
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
                    val createdEntity = response.body()!!

                    // If user attached a photo, upload it now
                    val photoPath = currentPhotoPath
                    if (!photoPath.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ItemCreateActivity, R.string.photo_uploading, Toast.LENGTH_SHORT).show()
                        }

                        val uploadSuccess = withContext(Dispatchers.IO) {
                            try {
                                val file = File(photoPath)
                                val multipartBody = MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("name", file.name)
                                    .addFormDataPart("type", "photo")
                                    .addFormDataPart("primary", "true")
                                    .addFormDataPart(
                                        "file",
                                        file.name,
                                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    )
                                    .build()

                                val uploadResp = service.uploadAttachment(createdEntity.id, multipartBody)
                                uploadResp.isSuccessful
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                        }

                        if (!uploadSuccess) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ItemCreateActivity, "物品已创建，但照片上传失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

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
