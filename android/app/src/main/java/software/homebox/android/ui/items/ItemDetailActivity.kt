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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.EntityItem
import software.homebox.android.databinding.ActivityItemDetailBinding
import software.homebox.android.ui.photo.PhotoEditorActivity
import java.io.File

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private var entity: EntityItem? = null
    private var cameraTempUri: Uri? = null

    private val editPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedPath = result.data?.getStringExtra("extra_photo_path")
            if (!editedPath.isNullOrBlank() && entity != null) {
                uploadEditedPhoto(entity!!.id, editedPath)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraTempUri != null) {
            openPhotoEditor(cameraTempUri!)
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
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        entity = intent.getSerializableExtra("extra_entity") as? EntityItem

        if (entity != null) {
            bindEntity(entity!!)
        } else {
            val entityId = intent.getStringExtra("extra_entity_id")
            if (!entityId.isNullOrBlank()) {
                loadEntityById(entityId)
            } else {
                finish()
            }
        }

        binding.btnAddDetailPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
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

    private fun uploadEditedPhoto(entityId: String, photoPath: String) {
        binding.btnAddDetailPhoto.isEnabled = false
        Toast.makeText(this, R.string.photo_uploading, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val file = File(photoPath)
                val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                val namePart = file.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val typePart = "photo".toRequestBody("text/plain".toMediaTypeOrNull())
                val primaryPart = "true".toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = withContext(Dispatchers.IO) {
                    service.uploadAttachment(entityId, part, namePart, typePart, primaryPart)
                }

                if (resp.isSuccessful && resp.body() != null) {
                    Toast.makeText(this@ItemDetailActivity, R.string.photo_upload_success, Toast.LENGTH_SHORT).show()
                    bindEntity(resp.body()!!)
                    setResult(RESULT_OK)
                } else {
                    val errMsg = resp.message().ifBlank { "状态码 ${resp.code()}" }
                    Toast.makeText(this@ItemDetailActivity, getString(R.string.photo_upload_failed, errMsg), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemDetailActivity, getString(R.string.photo_upload_failed, e.localizedMessage), Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnAddDetailPhoto.isEnabled = true
            }
        }
    }

    private fun bindEntity(item: EntityItem) {
        this.entity = item
        binding.tvDetailName.text = item.name

        val loc = item.locationName ?: item.parentName
        if (!loc.isNullOrBlank()) {
            binding.tvDetailLocation.visibility = View.VISIBLE
            binding.tvDetailLocation.text = getString(R.string.item_location, loc)
        } else {
            binding.tvDetailLocation.visibility = View.GONE
        }

        if (!item.description.isNullOrBlank()) {
            binding.tvDetailDesc.visibility = View.VISIBLE
            binding.tvDetailDesc.text = item.description
        } else {
            binding.tvDetailDesc.visibility = View.GONE
        }

        binding.tvDetailQuantity.text = getString(R.string.item_quantity, item.quantity ?: 1)
        binding.tvDetailModel.text = "型号: " + (item.modelNumber ?: "-")
        binding.tvDetailSerial.text = "序列号/SN: " + (item.serialNumber ?: "-")
        binding.tvDetailPrice.text = "购入价格: " + if (item.purchasePrice != null) "¥ ${item.purchasePrice}" else "-"

        val photo = item.attachments?.firstOrNull()
        if (photo != null) {
            val url = ApiClient.getAttachmentUrl(item.id, photo.id)
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_nav_items)
                .error(R.drawable.ic_nav_items)
                .centerCrop()
                .into(binding.ivDetailPhoto)

            binding.btnAddDetailPhoto.setText(R.string.photo_edit)
        } else {
            binding.ivDetailPhoto.setImageResource(R.drawable.ic_nav_items)
            binding.btnAddDetailPhoto.setText(R.string.photo_add)
        }
    }

    private fun loadEntityById(id: String) {
        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val resp = withContext(Dispatchers.IO) { service.getEntity(id) }
                if (resp.isSuccessful && resp.body() != null) {
                    bindEntity(resp.body()!!)
                } else {
                    Toast.makeText(this@ItemDetailActivity, "未找到该资产信息", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemDetailActivity, "加载失败: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun confirmDelete() {
        val current = entity ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(R.string.item_delete_confirm)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                performDelete(current.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performDelete(id: String) {
        binding.btnDelete.isEnabled = false
        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val resp = withContext(Dispatchers.IO) { service.deleteEntity(id) }
                if (resp.isSuccessful) {
                    Toast.makeText(this@ItemDetailActivity, R.string.item_deleted_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ItemDetailActivity, "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemDetailActivity, "删除出错: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnDelete.isEnabled = true
            }
        }
    }
}
