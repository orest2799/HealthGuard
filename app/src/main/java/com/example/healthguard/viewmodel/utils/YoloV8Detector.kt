package com.example.healthguard.viewmodel.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class Detection(
    val boundingBox: RectF,
    val label: String,
    val confidence: Float
)

class YoloV8Detector(context: Context) {


    private val interpreter: Interpreter by lazy {
        val opts = Interpreter.Options().apply {

            setUseXNNPACK(true)

            setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
        }
        Interpreter(loadModelFile(context, "best3.tflite"), opts)
    }

    private val labels: List<String> by lazy {
        context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val afd = context.assets.openFd(filename)
        FileInputStream(afd.fileDescriptor).use { fis ->
            val channel = fis.channel
            val mapped = channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength
            )
            afd.close()
            return mapped
        }
    }

    fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.5f,
        iouThreshold: Float = 0.5f
    ): List<Detection> {
        val inputSize = 640
        val resizedBitmap = bitmap.scale(inputSize, inputSize)

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255f
            val g = (pixel shr 8 and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        val output = Array(1) { Array(5) { FloatArray(8400) } } // shape: [1, 5, 8400]
        interpreter.run(inputBuffer, output)

        val rawOutput = Array(5) { FloatArray(8400) }
        for (i in 0 until 5) {
            for (j in 0 until 8400) {
                rawOutput[i][j] = output[0][i][j]
            }
        }

        val detections = mutableListOf<Detection>()
        val widthRatio = bitmap.width.toFloat()
        val heightRatio = bitmap.height.toFloat()

        for (i in 0 until 8400) {
            val x = rawOutput[0][i]
            val y = rawOutput[1][i]
            val w = rawOutput[2][i]
            val h = rawOutput[3][i]
            val conf = rawOutput[4][i]

            if (conf >= confidenceThreshold) {
                Log.d("YOLO", "Detection: x=$x y=$y w=$w h=$h conf=$conf")
                val left = (x - w / 2f)
                val top = (y - h / 2f)
                val right = (x + w / 2f)
                val bottom = (y + h / 2f)

                val rect = RectF(left, top, right, bottom)
                val label = labels.getOrElse(0) { "medicine_box" } // single class
                detections.add(Detection(rect, label, conf))
            }
        }

        return applyNMS(detections, iouThreshold)
    }


    fun close() {
        try {
            interpreter.close()
        } catch (_: Exception) {

        }
    }


    private fun applyNMS(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val kept = mutableListOf<Detection>()
        val sorted = detections.sortedByDescending { it.confidence }
        val used = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (used[i]) continue
            val detA = sorted[i]
            kept.add(detA)
            for (j in i + 1 until sorted.size) {
                if (used[j]) continue
                val detB = sorted[j]
                if (iou(detA.boundingBox, detB.boundingBox) > iouThreshold) {
                    used[j] = true
                }
            }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)

        val interW = maxOf(0f, right - left)
        val interH = maxOf(0f, bottom - top)
        val intersection = interW * interH

        val areaA = a.width() * a.height()
        val areaB = b.width() * b.height()
        val union = areaA + areaB - intersection
        return if (union <= 0f) 0f else intersection / union
    }
}
