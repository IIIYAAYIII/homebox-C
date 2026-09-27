package software.homebox.android.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtils {

    /**
     * Decode a bitmap from URI with sampled bounds to avoid OOM on huge camera shots.
     * Default max dimension is 2048 px.
     */
    fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        var input: InputStream? = null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            input = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var inSampleSize = 1
            val maxSide = max(srcWidth, srcHeight)
            if (maxSide > maxDimension) {
                inSampleSize = (maxSide.toFloat() / maxDimension.toFloat()).roundToInt()
                if (inSampleSize < 1) inSampleSize = 1
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            input = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }
    }

    fun decodeFromBytes(bytes: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressToJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    fun cropBitmap(source: Bitmap, normRect: RectF): Bitmap {
        val left = (normRect.left.coerceIn(0f, 1f) * source.width).roundToInt()
        val top = (normRect.top.coerceIn(0f, 1f) * source.height).roundToInt()
        val right = (normRect.right.coerceIn(0f, 1f) * source.width).roundToInt()
        val bottom = (normRect.bottom.coerceIn(0f, 1f) * source.height).roundToInt()

        val width = max(10, right - left)
        val height = max(10, bottom - top)

        val safeLeft = left.coerceIn(0, source.width - width)
        val safeTop = top.coerceIn(0, source.height - height)

        return Bitmap.createBitmap(source, safeLeft, safeTop, width, height)
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        matrix.postScale(sx, sy)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Document & Receipt enhancement mode:
     * Increases contrast and sharpening threshold so text/barcodes/labels pop clearly.
     */
    fun applyDocumentMode(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Grayscale + high contrast matrix
        val contrast = 1.6f
        val brightness = -20f
        val t = (1.0f - contrast) / 2.0f * 255.0f

        val cm = ColorMatrix(
            floatArrayOf(
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, t + brightness,
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, t + brightness,
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, t + brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Brightness (-50 to +50) and Contrast (-50 to +50).
     */
    fun adjustBrightnessContrast(source: Bitmap, brightness: Float, contrastFactor: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Scale contrast: 0 factor -> 1.0; -50 -> 0.5; +50 -> 1.5
        val c = 1.0f + (contrastFactor / 50f) * 0.5f
        val b = brightness * 1.5f
        val t = (1.0f - c) / 2.0f * 255.0f

        val cm = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t + b,
                0f, c, 0f, 0f, t + b,
                0f, 0f, c, 0f, t + b,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Downscale bitmap if its longest edge exceeds maxDimension.
     */
    fun scaleToMaxDimension(source: Bitmap, maxDimension: Int): Bitmap {
        val currentMax = max(source.width, source.height)
        if (currentMax <= maxDimension) return source

        val scale = maxDimension.toFloat() / currentMax.toFloat()
        val targetWidth = (source.width * scale).roundToInt()
        val targetHeight = (source.height * scale).roundToInt()

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun saveBytesToFile(bytes: ByteArray, targetFile: File): Boolean {
        return try {
            if (targetFile.exists()) targetFile.delete()
            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { it.write(bytes) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes.toFloat() / (1024 * 1024))
        }
    }
}
