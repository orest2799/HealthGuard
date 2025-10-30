package com.example.healthguard.presentation.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

import java.io.FileInputStream

/** Decode a file and rotate it according to EXIF so it’s upright. */
fun decodeAndFixOrientation(path: String): Bitmap {
    val stream = FileInputStream(path)
    val bmp = BitmapFactory.decodeStream(stream).also { stream.close() }
    val exif = ExifInterface(path)
    val degrees = when (exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
    if (degrees == 0) return bmp
    val m = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

/** Keep aspect ratio; limit the longest side to [maxSide]. */
fun Bitmap.downscaleMaxSide(maxSide: Int): Bitmap {
    val maxDim = maxOf(width, height)
    if (maxDim <= maxSide) return this
    val scale = maxSide.toFloat() / maxDim.toFloat()
    val newW = (width * scale).toInt().coerceAtLeast(1)
    val newH = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, newW, newH, true)
}
