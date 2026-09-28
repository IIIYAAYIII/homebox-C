package software.homebox.android.ui.items

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Response
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.CreateEntityRequest
import software.homebox.android.data.api.FlatLocationItem
import software.homebox.android.data.api.flattenLocations
import software.homebox.android.databinding.ActivityItemCreateBinding
import software.homebox.android.databinding.BottomSheetLocationPickerBinding
import software.homebox.android.ui.photo.BitmapUtils
import software.homebox.android.ui.photo.PhotoEditorActivity
import java.io.File

class ItemCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemCreateBinding

    private var currentPhotoPath: String? = null
    private var cameraTempUri: Uri? = null

    // Location selection state
    private var selectedLocationId: String? = null
    private var selectedLocationName: String? = null
    private var selectedLocationTreeString: String? = null
    private val locationList = mutableListOf<FlatLocationItem>()
    private var isLocationsLoaded = false

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

        // Initialize location if pre-passed via intent (e.g. browsing a location)
        val initParentId = intent.getStringExtra("extra_parent_id")
        val initParentName = intent.getStringExtra("extra_parent_name")
        if (!initParentId.isNullOrBlank()) {
            selectedLocationId = initParentId
            selectedLocationName = initParentName
            selectedLocationTreeString = initParentName
            updateLocationUI()
        }

        // Load full location tree in background
        loadLocations()

        // Location Selector Card clicks
        binding.cardLocationSelector.setOnClickListener {
            showLocationPickerDialog()
        }

        binding.btnClearLocation.setOnClickListener {
            clearSelectedLocation()
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

    private fun loadLocations() {
        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val resp = withContext(Dispatchers.IO) {
                    service.getLocationTree()
                }
                if (resp.isSuccessful && resp.body() != null) {
                    val tree = resp.body()!!
                    locationList.clear()
                    locationList.addAll(tree.flattenLocations())
                    isLocationsLoaded = true

                    // If a location was already selected, update its full breadcrumb path
                    if (selectedLocationId != null) {
                        val matched = locationList.firstOrNull { it.id == selectedLocationId }
                        if (matched != null) {
                            selectedLocationName = matched.name
                            selectedLocationTreeString = matched.treeString
                            updateLocationUI()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLocationUI() {
        val locText = selectedLocationTreeString ?: selectedLocationName
        if (!locText.isNullOrBlank()) {
            binding.tvSelectedLocation.text = locText
            binding.tvSelectedLocation.setTextColor(ContextCompat.getColor(this, R.color.on_surface))
            binding.btnClearLocation.visibility = View.VISIBLE
            binding.ivLocationDropdownArrow.visibility = View.GONE
        } else {
            binding.tvSelectedLocation.text = getString(R.string.location_selector_hint)
            binding.tvSelectedLocation.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
            binding.btnClearLocation.visibility = View.GONE
            binding.ivLocationDropdownArrow.visibility = View.VISIBLE
        }
    }

    private fun clearSelectedLocation() {
        selectedLocationId = null
        selectedLocationName = null
        selectedLocationTreeString = null
        updateLocationUI()
    }

    private fun showLocationPickerDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetLocationPickerBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        val adapter = LocationPickerAdapter { selectedItem ->
            if (selectedItem == null) {
                clearSelectedLocation()
            } else {
                selectedLocationId = selectedItem.id
                selectedLocationName = selectedItem.name
                selectedLocationTreeString = selectedItem.treeString
                updateLocationUI()
            }
            bottomSheetDialog.dismiss()
        }

        sheetBinding.rvLocations.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvLocations.adapter = adapter
        adapter.setData(locationList, selectedLocationId)

        sheetBinding.btnSheetClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sheetBinding.etLocationSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString().orEmpty()
                adapter.filter(q)
                sheetBinding.tvLocationEmpty.visibility =
                    if (adapter.itemCount <= 1 && q.isNotBlank()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (!isLocationsLoaded) {
            sheetBinding.pbLocationLoading.visibility = View.VISIBLE
            lifecycleScope.launch {
                loadLocations()
                sheetBinding.pbLocationLoading.visibility = View.GONE
                adapter.setData(locationList, selectedLocationId)
            }
        }

        bottomSheetDialog.show()
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
            parentId = selectedLocationId,
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

                        val uploadError = withContext(Dispatchers.IO) {
                            try {
                                val file = File(photoPath)
                                val safeName = "photo_${System.currentTimeMillis()}.jpg"
                                val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

                                val multipartBody = MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("name", safeName)
                                    .addFormDataPart("type", "photo")
                                    .addFormDataPart("primary", "true")
                                    .addFormDataPart("file", safeName, fileBody)
                                    .build()

                                val uploadResp = service.uploadAttachment(createdEntity.id, multipartBody)
                                if (uploadResp.isSuccessful) {
                                    null
                                } else {
                                    extractErrorMessage(uploadResp)
                                }
                            } catch (e: Exception) {
                                e.localizedMessage ?: "上传网络异常"
                            }
                        }

                        if (uploadError != null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ItemCreateActivity, getString(R.string.photo_upload_failed, uploadError), Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    Toast.makeText(this@ItemCreateActivity, R.string.item_created_success, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    val errorMsg = extractErrorMessage(response)
                    Toast.makeText(this@ItemCreateActivity, "录入失败: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemCreateActivity, "录入出错: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun extractErrorMessage(response: Response<*>): String {
        try {
            val raw = response.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                val json = JSONObject(raw)
                if (json.has("error")) {
                    val err = json.getString("error")
                    if (err.isNotBlank()) return err
                }
                if (json.has("message")) {
                    val msg = json.getString("message")
                    if (msg.isNotBlank()) return msg
                }
                return raw
            }
        } catch (_: Exception) {}
        val msg = response.message()
        return if (msg.isNotBlank()) msg else "HTTP ${response.code()}"
    }
}
