package com.example.healthguard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.healthguard.presentation.utils.Detection

fun drawDetectionsOnBitmap(original: Bitmap, detections: List<Detection>): Bitmap {
    val resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val boxPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }

    for (detection in detections) {
        canvas.drawRect(detection.boundingBox, boxPaint)
        canvas.drawText(
            "${detection.label} %.2f".format(detection.confidence),
            detection.boundingBox.left,
            detection.boundingBox.top - 10f,
            textPaint
        )
    }

    return resultBitmap
}
