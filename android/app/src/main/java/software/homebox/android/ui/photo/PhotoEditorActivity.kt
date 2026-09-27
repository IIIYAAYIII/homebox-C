package software.homebox.android.ui.photo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.homebox.android.R
import software.homebox.android.databinding.ActivityPhotoEditorBinding
import java.io.File
import kotlin.math.max
import kotlin.math.min

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditorBinding

    private var currentBitmap: Bitmap? = null
    private var originalByteSize: Long = 0L

    // Compressed JPEG byte array history stack (up to 50 steps)
    private val historyStack = ArrayList<ByteArray>()
    private var currentIndex = -1

    private enum class ActiveTab {
        CROP, ROTATE, FILTER, RESOLUTION, ADJUST
    }

    private var currentTab = ActiveTab.CROP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUri = intent.getParcelableExtra<Uri>("extra_image_uri")
        val imagePath = intent.getStringExtra("extra_image_path")

        if (imageUri == null && imagePath.isNullOrBlank()) {
            Toast.makeText(this, "未找到图片路径", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initListeners()

        val targetUri = imageUri ?: Uri.fromFile(File(imagePath!!))
        loadInitialImage(targetUri)
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnUndo.setOnClickListener {
            undo()
        }

        binding.btnRedo.setOnClickListener {
            redo()
        }

        binding.btnReset.setOnClickListener {
            resetToOriginal()
        }

        binding.btnDone.setOnClickListener {
            saveAndFinish()
        }

        // Tabs
        binding.tabCrop.setOnClickListener { switchTab(ActiveTab.CROP) }
        binding.tabRotate.setOnClickListener { switchTab(ActiveTab.ROTATE) }
        binding.tabFilter.setOnClickListener { switchTab(ActiveTab.FILTER) }
        binding.tabResolution.setOnClickListener { switchTab(ActiveTab.RESOLUTION) }
        binding.tabAdjust.setOnClickListener { switchTab(ActiveTab.ADJUST) }

        // Crop ratios
        binding.btnRatioFree.setOnClickListener { binding.cropOverlay.resetCrop(null) }
        binding.btnRatio1_1.setOnClickListener { binding.cropOverlay.resetCrop(1f) }
        binding.btnRatio4_3.setOnClickListener { binding.cropOverlay.resetCrop(4f / 3f) }
        binding.btnRatio16_9.setOnClickListener { binding.cropOverlay.resetCrop(16f / 9f) }
        binding.btnApplyCrop.setOnClickListener { applyCrop() }

        // Rotate
        binding.btnRotateCW.setOnClickListener { rotate(90f) }
        binding.btnRotateCCW.setOnClickListener { rotate(-90f) }
        binding.btnFlipH.setOnClickListener { flip(horizontal = true, vertical = false) }
        binding.btnFlipV.setOnClickListener { flip(horizontal = false, vertical = true) }

        // Filters
        binding.btnFilterColor.setOnClickListener { applyFilter(0) }
        binding.btnFilterBW.setOnClickListener { applyFilter(1) }
        binding.btnFilterDoc.setOnClickListener { applyFilter(2) }

        // Resolution
        binding.btnRes720.setOnClickListener { scaleResolution(1280) }
        binding.btnRes1080.setOnClickListener { scaleResolution(1920) }
        binding.btnRes1600.setOnClickListener { scaleResolution(1600) }
        binding.btnResOrig.setOnClickListener {
            Toast.makeText(this, R.string.photo_res_already_lower, Toast.LENGTH_SHORT).show()
        }

        // Adjust SeekBars
        binding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 50
                binding.tvBrightnessLabel.text = getString(R.string.photo_brightness, value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sbContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 50
                binding.tvContrastLabel.text = getString(R.string.photo_contrast, value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnAutoEnhance.setOnClickListener {
            binding.sbBrightness.progress = 65 // +15
            binding.sbContrast.progress = 70 // +20
            applyAdjust()
        }

        binding.btnApplyAdjust.setOnClickListener {
            applyAdjust()
        }
    }

    private fun loadInitialImage(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val rawSize = contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
                    originalByteSize = if (rawSize > 0) rawSize else 1024L * 1024L
                } catch (_: Exception) {}

                BitmapUtils.decodeSampledBitmap(this@PhotoEditorActivity, uri, 2048)
            }

            if (bitmap == null) {
                Toast.makeText(this@PhotoEditorActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val initialBytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(bitmap, 85)
            }
            if (originalByteSize <= 0) {
                originalByteSize = initialBytes.size.toLong()
            }

            pushState(initialBytes, bitmap)
            switchTab(ActiveTab.CROP)
        }
    }

    private fun switchTab(tab: ActiveTab) {
        currentTab = tab

        binding.panelCrop.visibility = if (tab == ActiveTab.CROP) View.VISIBLE else View.GONE
        binding.panelRotate.visibility = if (tab == ActiveTab.ROTATE) View.VISIBLE else View.GONE
        binding.panelFilter.visibility = if (tab == ActiveTab.FILTER) View.VISIBLE else View.GONE
        binding.panelResolution.visibility = if (tab == ActiveTab.RESOLUTION) View.VISIBLE else View.GONE
        binding.panelAdjust.visibility = if (tab == ActiveTab.ADJUST) View.VISIBLE else View.GONE

        // Crop overlay is only visible when user is in CROP tab
        binding.cropOverlay.visibility = if (tab == ActiveTab.CROP) View.VISIBLE else View.GONE

        // Update tab styling
        val activeColor = Color.parseColor("#52B788")
        val inactiveColor = Color.parseColor("#888888")

        binding.ivTabCrop.setColorFilter(if (tab == ActiveTab.CROP) activeColor else inactiveColor)
        binding.tvTabCrop.setTextColor(if (tab == ActiveTab.CROP) activeColor else inactiveColor)

        binding.ivTabRotate.setColorFilter(if (tab == ActiveTab.ROTATE) activeColor else inactiveColor)
        binding.tvTabRotate.setTextColor(if (tab == ActiveTab.ROTATE) activeColor else inactiveColor)

        binding.ivTabFilter.setColorFilter(if (tab == ActiveTab.FILTER) activeColor else inactiveColor)
        binding.tvTabFilter.setTextColor(if (tab == ActiveTab.FILTER) activeColor else inactiveColor)

        binding.ivTabResolution.setColorFilter(if (tab == ActiveTab.RESOLUTION) activeColor else inactiveColor)
        binding.tvTabResolution.setTextColor(if (tab == ActiveTab.RESOLUTION) activeColor else inactiveColor)

        binding.ivTabAdjust.setColorFilter(if (tab == ActiveTab.ADJUST) activeColor else inactiveColor)
        binding.tvTabAdjust.setTextColor(if (tab == ActiveTab.ADJUST) activeColor else inactiveColor)

        if (tab == ActiveTab.CROP) {
            updateCropOverlayBounds()
        }
    }

    private fun updateCropOverlayBounds() {
        binding.ivPreview.post {
            val bitmap = currentBitmap ?: return@post
            val vWidth = binding.ivPreview.width.toFloat()
            val vHeight = binding.ivPreview.height.toFloat()
            if (vWidth <= 0 || vHeight <= 0) return@post

            val bWidth = bitmap.width.toFloat()
            val bHeight = bitmap.height.toFloat()

            val scale = min(vWidth / bWidth, vHeight / bHeight)
            val drawnWidth = bWidth * scale
            val drawnHeight = bHeight * scale

            val left = (vWidth - drawnWidth) / 2f
            val top = (vHeight - drawnHeight) / 2f

            binding.cropOverlay.imageBounds = RectF(left, top, left + drawnWidth, top + drawnHeight)
        }
    }

    private fun pushState(bytes: ByteArray, bitmap: Bitmap) {
        // Discard any forward redo history if we are in the middle of stack
        while (historyStack.size > currentIndex + 1) {
            historyStack.removeAt(historyStack.size - 1)
        }

        // Limit stack to 50 steps
        if (historyStack.size >= 50) {
            historyStack.removeAt(0)
            currentIndex--
        }

        historyStack.add(bytes)
        currentIndex = historyStack.size - 1
        currentBitmap = bitmap

        binding.ivPreview.setImageBitmap(bitmap)
        updateCropOverlayBounds()
        updateHistoryUI()
    }

    private fun updateHistoryUI() {
        binding.btnUndo.isEnabled = currentIndex > 0
        binding.btnUndo.alpha = if (currentIndex > 0) 1.0f else 0.35f

        binding.btnRedo.isEnabled = currentIndex < historyStack.size - 1
        binding.btnRedo.alpha = if (currentIndex < historyStack.size - 1) 1.0f else 0.35f

        binding.btnReset.isEnabled = currentIndex > 0
        binding.btnReset.alpha = if (currentIndex > 0) 1.0f else 0.35f

        binding.tvStepBadge.text = getString(R.string.photo_step, currentIndex + 1, historyStack.size)

        // Live file size & space savings calculation
        if (currentIndex in historyStack.indices) {
            val currentSize = historyStack[currentIndex].size.toLong()
            val sizeStr = BitmapUtils.formatFileSize(currentSize)

            if (originalByteSize > currentSize) {
                val savedPct = (((originalByteSize - currentSize).toDouble() / originalByteSize.toDouble()) * 100.0).toInt().coerceIn(1, 99)
                binding.tvSizeIndicator.text = getString(R.string.photo_saved_space, sizeStr, savedPct)
            } else {
                binding.tvSizeIndicator.text = sizeStr
            }
        }
    }

    private fun undo() {
        if (currentIndex > 0) {
            currentIndex--
            applyStateAt(currentIndex)
        }
    }

    private fun redo() {
        if (currentIndex < historyStack.size - 1) {
            currentIndex++
            applyStateAt(currentIndex)
        }
    }

    private fun resetToOriginal() {
        if (currentIndex > 0) {
            currentIndex = 0
            applyStateAt(0)
        }
    }

    private fun applyStateAt(index: Int) {
        val bytes = historyStack[index]
        val decoded = BitmapUtils.decodeFromBytes(bytes) ?: return
        currentBitmap = decoded
        binding.ivPreview.setImageBitmap(decoded)
        updateCropOverlayBounds()
        updateHistoryUI()
    }

    // --- Action Handlers ---

    private fun applyCrop() {
        val bitmap = currentBitmap ?: return
        val normRect = binding.cropOverlay.getCropRectNormalized()

        lifecycleScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                BitmapUtils.cropBitmap(bitmap, normRect)
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(cropped, 85)
            }
            pushState(bytes, cropped)
            binding.cropOverlay.resetCrop(binding.cropOverlay.targetAspectRatio)
            Toast.makeText(this@PhotoEditorActivity, R.string.photo_crop_applied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotate(degrees: Float) {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                BitmapUtils.rotateBitmap(bitmap, degrees)
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(rotated, 85)
            }
            pushState(bytes, rotated)
        }
    }

    private fun flip(horizontal: Boolean, vertical: Boolean) {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            val flipped = withContext(Dispatchers.Default) {
                BitmapUtils.flipBitmap(bitmap, horizontal, vertical)
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(flipped, 85)
            }
            pushState(bytes, flipped)
        }
    }

    private fun applyFilter(filterType: Int) {
        // 0 = Color (restore state 0's color or keep current), 1 = B&W, 2 = Document Scan
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            val processed = withContext(Dispatchers.Default) {
                when (filterType) {
                    1 -> BitmapUtils.applyGrayscale(bitmap)
                    2 -> BitmapUtils.applyDocumentMode(bitmap)
                    else -> {
                        // Restore initial color bitmap if available
                        BitmapUtils.decodeFromBytes(historyStack[0]) ?: bitmap
                    }
                }
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(processed, 85)
            }
            pushState(bytes, processed)
        }
    }

    private fun scaleResolution(maxDim: Int) {
        val bitmap = currentBitmap ?: return
        val currentMax = max(bitmap.width, bitmap.height)
        if (currentMax <= maxDim) {
            Toast.makeText(this, R.string.photo_res_already_lower, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val scaled = withContext(Dispatchers.Default) {
                BitmapUtils.scaleToMaxDimension(bitmap, maxDim)
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(scaled, 85)
            }
            pushState(bytes, scaled)
        }
    }

    private fun applyAdjust() {
        val bitmap = currentBitmap ?: return
        val b = (binding.sbBrightness.progress - 50).toFloat()
        val c = (binding.sbContrast.progress - 50).toFloat()

        lifecycleScope.launch {
            val adjusted = withContext(Dispatchers.Default) {
                BitmapUtils.adjustBrightnessContrast(bitmap, b, c)
            }
            val bytes = withContext(Dispatchers.IO) {
                BitmapUtils.compressToJpeg(adjusted, 85)
            }
            pushState(bytes, adjusted)
            // Reset seekbars to center after applying
            binding.sbBrightness.progress = 50
            binding.sbContrast.progress = 50
        }
    }

    private fun saveAndFinish() {
        if (currentIndex !in historyStack.indices) {
            finish()
            return
        }

        val finalBytes = historyStack[currentIndex]
        lifecycleScope.launch {
            val outputFile = withContext(Dispatchers.IO) {
                val dir = File(cacheDir, "photos").apply { mkdirs() }
                val file = File(dir, "photo_edited_${System.currentTimeMillis()}.jpg")
                BitmapUtils.saveBytesToFile(finalBytes, file)
                file
            }

            val resultIntent = Intent().apply {
                putExtra("extra_photo_path", outputFile.absolutePath)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
