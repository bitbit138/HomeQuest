package dev.tombit.homequest.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Client-side image compression utility.
 * Contract (Section 6.2):
 *   - Max dimension: 1280px longest side (downscale only)
 *   - Format: JPEG
 *   - Quality: 80%. Retry at 60% if result > 200 KB.
 *   - Max file size: < 200 KB
 *
 * Uses only BitmapFactory + compress loop — no extra dependency.
 */
object ImageCompressor {

    /**
     * Compresses an image from the given URI and returns a compressed JPEG byte array.
     * Returns null if the image cannot be decoded.
     */
    fun compress(context: Context, uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val corrected = correctOrientation(context, uri, original)
        val scaled = scaleDown(corrected)

        // Primary attempt at 80%
        val primary = toJpeg(scaled, Constants.Storage.JPEG_QUALITY_PRIMARY)
        if (primary.size <= Constants.Storage.MAX_FILE_SIZE_BYTES) {
            if (scaled != corrected) corrected.recycle()
            if (corrected != original) original.recycle()
            return primary
        }

        // Fallback attempt at 60%
        val fallback = toJpeg(scaled, Constants.Storage.JPEG_QUALITY_FALLBACK)
        if (scaled != corrected) corrected.recycle()
        if (corrected != original) original.recycle()
        return fallback
    }

    /**
     * Reads EXIF orientation from the image and rotates/flips the bitmap accordingly.
     * Fixes photos that appear rotated 90 degrees due to camera orientation metadata.
     */
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        return try {
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                return bitmap
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f)
                    matrix.preScale(-1f, 1f)
                }
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            inputStream.close()
            bitmap
        }
    }

    /**
     * Compresses and writes to a temp file, returning the File.
     * The caller is responsible for deleting the file after upload.
     */
    fun compressToFile(context: Context, uri: Uri, outputName: String): File? {
        val bytes = compress(context, uri) ?: return null
        val file = File(context.cacheDir, outputName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxDim = Constants.Storage.MAX_IMAGE_DIMENSION
        val width = bitmap.width
        val height = bitmap.height
        val longestSide = maxOf(width, height)

        if (longestSide <= maxDim) return bitmap // Already within bounds

        val scale = maxDim.toFloat() / longestSide
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun toJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}
