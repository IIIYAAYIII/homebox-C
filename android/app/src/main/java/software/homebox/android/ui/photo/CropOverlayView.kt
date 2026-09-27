package software.homebox.android.ui.photo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // 70% black overlay
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF") // 50% white grid
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#52B788") // Primary accent green
        style = Paint.Style.STROKE
        strokeWidth = 5f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    var imageBounds: RectF = RectF()
        set(value) {
            field = value
            initCropRect()
            invalidate()
        }

    val cropRect: RectF = RectF()
    var hasUserModifiedCrop: Boolean = false

    var targetAspectRatio: Float? = null // null = Free, 1f = 1:1, 4f/3f = 4:3, 16f/9f = 16:9
        set(value) {
            field = value
            applyAspectRatio()
            invalidate()
        }

    private val touchTolerance = 30f * resources.displayMetrics.density
    private val cornerLength = 22f * resources.displayMetrics.density
    private val minCropSize = 60f * resources.displayMetrics.density

    private var activeHandle = TouchHandle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    enum class TouchHandle {
        NONE, CENTER,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        LEFT, TOP, RIGHT, BOTTOM
    }

    fun initCropRect() {
        if (imageBounds.width() <= 0 || imageBounds.height() <= 0) return

        val insetX = imageBounds.width() * 0.05f
        val insetY = imageBounds.height() * 0.05f

        cropRect.set(
            imageBounds.left + insetX,
            imageBounds.top + insetY,
            imageBounds.right - insetX,
            imageBounds.bottom - insetY
        )

        applyAspectRatio()
    }

    fun resetCrop(ratio: Float? = null) {
        this.targetAspectRatio = ratio
        this.hasUserModifiedCrop = (ratio != null)
        initCropRect()
        invalidate()
    }

    fun hasPendingCrop(): Boolean {
        if (!hasUserModifiedCrop || imageBounds.width() <= 0 || imageBounds.height() <= 0) return false
        val norm = getCropRectNormalized()
        return norm.left > 0.01f || norm.top > 0.01f || norm.right < 0.99f || norm.bottom < 0.99f
    }

    private fun applyAspectRatio() {
        val ratio = targetAspectRatio ?: return
        if (imageBounds.width() <= 0 || imageBounds.height() <= 0) return

        val cx = cropRect.centerX()
        val cy = cropRect.centerY()

        var w = cropRect.width()
        var h = w / ratio

        if (h > imageBounds.height() * 0.9f) {
            h = imageBounds.height() * 0.9f
            w = h * ratio
        }
        if (w > imageBounds.width() * 0.9f) {
            w = imageBounds.width() * 0.9f
            h = w / ratio
        }

        cropRect.set(
            (cx - w / 2f).coerceIn(imageBounds.left, imageBounds.right - w),
            (cy - h / 2f).coerceIn(imageBounds.top, imageBounds.bottom - h),
            (cx + w / 2f).coerceIn(imageBounds.left + w, imageBounds.right),
            (cy + h / 2f).coerceIn(imageBounds.top + h, imageBounds.bottom)
        )
    }

    fun getCropRectNormalized(): RectF {
        if (imageBounds.width() <= 0 || imageBounds.height() <= 0) {
            return RectF(0f, 0f, 1f, 1f)
        }
        val l = ((cropRect.left - imageBounds.left) / imageBounds.width()).coerceIn(0f, 1f)
        val t = ((cropRect.top - imageBounds.top) / imageBounds.height()).coerceIn(0f, 1f)
        val r = ((cropRect.right - imageBounds.left) / imageBounds.width()).coerceIn(0f, 1f)
        val b = ((cropRect.bottom - imageBounds.top) / imageBounds.height()).coerceIn(0f, 1f)
        return RectF(l, t, r, b)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (imageBounds.width() <= 0 || imageBounds.height() <= 0) return

        // 1. Draw 4 dark surrounding rectangles
        // Top
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, maskPaint)
        // Bottom
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), maskPaint)
        // Left
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, maskPaint)
        // Right
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, maskPaint)

        // 2. Draw 3x3 grid lines
        val stepX = cropRect.width() / 3f
        val stepY = cropRect.height() / 3f
        canvas.drawLine(cropRect.left + stepX, cropRect.top, cropRect.left + stepX, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + stepX * 2, cropRect.top, cropRect.left + stepX * 2, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + stepY, cropRect.right, cropRect.top + stepY, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + stepY * 2, cropRect.right, cropRect.top + stepY * 2, gridPaint)

        // 3. Draw border
        canvas.drawRect(cropRect, borderPaint)

        // 4. Draw Corner handles
        val cl = cornerLength
        // Top-Left
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cl, cropRect.top, cornerPaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cl, cornerPaint)
        // Top-Right
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - cl, cropRect.top, cornerPaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cl, cornerPaint)
        // Bottom-Left
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cl, cropRect.bottom, cornerPaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - cl, cornerPaint)
        // Bottom-Right
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - cl, cropRect.bottom, cornerPaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - cl, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = getHitHandle(x, y)
                lastTouchX = x
                lastTouchY = y
                return activeHandle != TouchHandle.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != TouchHandle.NONE) {
                    hasUserModifiedCrop = true
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    onMoveHandle(dx, dy)
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = TouchHandle.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getHitHandle(x: Float, y: Float): TouchHandle {
        val tol = touchTolerance
        return when {
            // Corners
            isNear(x, y, cropRect.left, cropRect.top, tol) -> TouchHandle.TOP_LEFT
            isNear(x, y, cropRect.right, cropRect.top, tol) -> TouchHandle.TOP_RIGHT
            isNear(x, y, cropRect.left, cropRect.bottom, tol) -> TouchHandle.BOTTOM_LEFT
            isNear(x, y, cropRect.right, cropRect.bottom, tol) -> TouchHandle.BOTTOM_RIGHT

            // Edges
            abs(x - cropRect.left) < tol && y in cropRect.top..cropRect.bottom -> TouchHandle.LEFT
            abs(x - cropRect.right) < tol && y in cropRect.top..cropRect.bottom -> TouchHandle.RIGHT
            abs(y - cropRect.top) < tol && x in cropRect.left..cropRect.right -> TouchHandle.TOP
            abs(y - cropRect.bottom) < tol && x in cropRect.left..cropRect.right -> TouchHandle.BOTTOM

            // Center drag
            cropRect.contains(x, y) -> TouchHandle.CENTER

            else -> TouchHandle.NONE
        }
    }

    private fun isNear(x: Float, y: Float, targetX: Float, targetY: Float, tolerance: Float): Boolean {
        return abs(x - targetX) <= tolerance && abs(y - targetY) <= tolerance
    }

    private fun onMoveHandle(dx: Float, dy: Float) {
        if (activeHandle == TouchHandle.CENTER) {
            val width = cropRect.width()
            val height = cropRect.height()

            var newLeft = cropRect.left + dx
            var newTop = cropRect.top + dy

            newLeft = newLeft.coerceIn(imageBounds.left, imageBounds.right - width)
            newTop = newTop.coerceIn(imageBounds.top, imageBounds.bottom - height)

            cropRect.set(newLeft, newTop, newLeft + width, newTop + height)
            return
        }

        var l = cropRect.left
        var t = cropRect.top
        var r = cropRect.right
        var b = cropRect.bottom

        when (activeHandle) {
            TouchHandle.TOP_LEFT -> {
                l += dx
                t += dy
            }
            TouchHandle.TOP_RIGHT -> {
                r += dx
                t += dy
            }
            TouchHandle.BOTTOM_LEFT -> {
                l += dx
                b += dy
            }
            TouchHandle.BOTTOM_RIGHT -> {
                r += dx
                b += dy
            }
            TouchHandle.LEFT -> l += dx
            TouchHandle.RIGHT -> r += dx
            TouchHandle.TOP -> t += dy
            TouchHandle.BOTTOM -> b += dy
            else -> {}
        }

        // Clamp to min size
        if (r - l < minCropSize) {
            if (activeHandle == TouchHandle.LEFT || activeHandle == TouchHandle.TOP_LEFT || activeHandle == TouchHandle.BOTTOM_LEFT) {
                l = r - minCropSize
            } else {
                r = l + minCropSize
            }
        }
        if (b - t < minCropSize) {
            if (activeHandle == TouchHandle.TOP || activeHandle == TouchHandle.TOP_LEFT || activeHandle == TouchHandle.TOP_RIGHT) {
                t = b - minCropSize
            } else {
                b = t + minCropSize
            }
        }

        // Clamp inside image bounds
        l = l.coerceIn(imageBounds.left, imageBounds.right - minCropSize)
        r = r.coerceIn(imageBounds.left + minCropSize, imageBounds.right)
        t = t.coerceIn(imageBounds.top, imageBounds.bottom - minCropSize)
        b = b.coerceIn(imageBounds.top + minCropSize, imageBounds.bottom)

        // If aspect ratio is locked, adjust
        targetAspectRatio?.let { ratio ->
            var newW = r - l
            var newH = newW / ratio
            if (t + newH > imageBounds.bottom) {
                newH = imageBounds.bottom - t
                newW = newH * ratio
            }
            r = l + newW
            b = t + newH
        }

        cropRect.set(l, t, r, b)
    }
}
