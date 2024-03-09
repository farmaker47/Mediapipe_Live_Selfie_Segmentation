/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.imagesegmenter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    companion object {
        const val ALPHA_COLOR = 128
    }

    private var scaleBitmap: Bitmap? = null
    private var runningMode: RunningMode = RunningMode.IMAGE

    fun clear() {
        scaleBitmap = null
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        scaleBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
    }

    /*fun setResults(
        originalBitmap: Bitmap? = null,
        byteBuffer: ByteBuffer,
        outputWidth: Int,
        outputHeight: Int
    ) {
        // Create the mask bitmap with colors and the set of detected labels.
        Log.v("Size_bitmap", originalBitmap?.height.toString())
        Log.v("Size_bitmap_output", outputWidth.toString())
        val pixels = IntArray(byteBuffer.capacity())
        for (i in pixels.indices) {
            // Using unsigned int here because selfie segmentation returns 0 or 255U (-1 signed)
            // with 0 being the found person, 255U for no label.
            // Deeplab uses 0 for background and other labels are 1-19,
            // so only providing 20 colors from ImageSegmenterHelper -> labelColors
            val index = byteBuffer.get(i).toUInt() % 20U
            //Log.v("pixel_integers", index.toInt().toString())
            val color = if (index.toInt() == 0) Color.BLACK else Color.TRANSPARENT //ImageSegmenterHelper.labelColors[index.toInt()].toAlphaColor()
            pixels[i] = color
        }
        val mask = Bitmap.createBitmap(
            pixels,
            outputWidth,
            outputHeight,
            Bitmap.Config.ARGB_8888
        )

        // Scale below
        val scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / outputWidth, height * 1f / outputHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / outputWidth, height * 1f / outputHeight)
            }
        }

        val scaleWidth = (outputWidth * scaleFactor).toInt()
        val scaleHeight = (outputHeight * scaleFactor).toInt()

        //val originalBitmapScaled = originalBitmap?.let { Bitmap.createScaledBitmap(it, scaleWidth, scaleHeight, false) }
        //val maskScaled = Bitmap.createScaledBitmap(mask, scaleWidth, scaleHeight, false)
        // val smallBitmap = originalBitmap?.let { cropBitmapWithMask(it, mask, "agray.jpg") }
        val smallMask = Bitmap.createScaledBitmap(mask, 240, 320, false)
        val smallOriginalBitmap = originalBitmap?.let { Bitmap.createScaledBitmap(it, 240, 320, false) }

        val smallBitmap = smallOriginalBitmap?.let { cropBitmapWithMask(it, smallMask, "blur2.jpg") }

        scaleBitmap = smallBitmap?.let { Bitmap.createScaledBitmap(it, scaleWidth, scaleHeight, false) }
        //scaleBitmap = smallBitmap?.let { OpenCVUtils.resizeBitmap(it, scaleWidth, scaleHeight) }

        invalidate()
    }*/
    fun setResults(
        originalBitmap: Bitmap? = null,
        byteBuffer: ByteBuffer,
        outputWidth: Int,
        outputHeight: Int
    ) {
        if (originalBitmap == null) {
            return
        }

        val pixels = IntArray(byteBuffer.capacity())
        for (i in pixels.indices) {
            val index = byteBuffer.get(i).toUInt() % 20U
            val color = if (index.toInt() == 0) Color.BLACK else Color.TRANSPARENT
            pixels[i] = color
        }

        val mask = Bitmap.createBitmap(
            pixels,
            outputWidth,
            outputHeight,
            Bitmap.Config.ARGB_8888
        )

        val scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / outputWidth, height * 1f / outputHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / outputWidth, height * 1f / outputHeight)
            }
        }

        val scaleWidth = (outputWidth * scaleFactor).toInt()
        val scaleHeight = (outputHeight * scaleFactor).toInt()

        // Crop the original bitmap with the mask and apply blur style
        val smallBitmap = originalBitmap
            .let { Bitmap.createScaledBitmap(it, outputWidth, outputHeight, false) }
            .let { cropBitmapWithMask(it, mask, "blur2.jpg") }

        ////////////
        // Or make smaller bitmaps

        /*val smallMask = Bitmap.createScaledBitmap(mask, 240, 320, false)
        val smallOriginalBitmap = originalBitmap?.let { Bitmap.createScaledBitmap(it, 240, 320, false) }

        val smallBitmap = smallOriginalBitmap?.let { cropBitmapWithMask(it, smallMask, "blur2.jpg") }

        scaleBitmap = smallBitmap?.let { Bitmap.createScaledBitmap(it, scaleWidth, scaleHeight, false) }*/

        ////////////

        // Scale the resulting bitmap to fit the view
        scaleBitmap = smallBitmap?.let { Bitmap.createScaledBitmap(it, scaleWidth, scaleHeight, false) }

        //invalidate()
    }


    /*private fun cropBitmapWithMask(original: Bitmap, mask: Bitmap?, style: String): Bitmap? {
        if (original == null || mask == null
        ) {
            return null
        }
        // Log.i("ORIGINAL_WIDTH", original.width.toString())
        // Log.i("ORIGINAL_HEIGHT", original.height.toString())
        // Log.i("MASK_WIDTH", original.width.toString())
        // Log.i("MASK_HEIGHT", original.height.toString())
        val w = original.width
        val h = original.height
        if (w <= 0 || h <= 0) {
            return null
        }

        // Generate colored foreground with transparent background
        val cropped: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawBitmap(original, 0f, 0f, null)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        // Generate final bitmap with colored foreground and B/W background
        *//*val croppedFinal: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvasFinal = Canvas(croppedFinal)
        val paintFinal = Paint(Paint.ANTI_ALIAS_FLAG)
        paintFinal.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        when (style) {
            "agray.jpg" -> canvasFinal.drawBitmap(androidGrayScale(original), 0f, 0f, null)
            "blur1.jpg" -> canvasFinal.drawBitmap(blurImage(original, 5), 0f, 0f, null)
            "blur2.jpg" -> canvasFinal.drawBitmap(blurImage(original, 10), 0f, 0f, null)
            "blur3.jpg" -> canvasFinal.drawBitmap(blurImage(original, 15), 0f, 0f, null)
            "sepia.jpg" -> canvasFinal.drawBitmap(setSepiaColorFilter(original), 0f, 0f, null)
        }
        //canvasFinal.drawBitmap(setSepiaColorFilter(original), 0f, 0f, null)
        canvasFinal.drawBitmap(cropped, 0f, 0f, paint)
        paintFinal.xfermode = null*//*



        var finalBitmap: Bitmap? = null
        when (style) {
            "agray.jpg" -> finalBitmap = androidGrayScale(cropped)
            "blur1.jpg" -> finalBitmap = blurImage(cropped, 5)
            "blur2.jpg" -> finalBitmap = blurImage(cropped, 10)
            "blur3.jpg" -> finalBitmap = blurImage(cropped, 15)
            "sepia.jpg" -> finalBitmap = setSepiaColorFilter(cropped)
        }

        return finalBitmap
    }*/

    private fun cropBitmapWithMask(original: Bitmap, mask: Bitmap?, style: String): Bitmap? {
        if (original == null || mask == null) {
            return null
        }

        val w = original.width
        val h = original.height
        if (w <= 0 || h <= 0) {
            return null
        }

        // Reuse cropped bitmap to avoid creating a new one each time
        val cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)

        // Draw the original bitmap
        canvas.drawBitmap(original, 0f, 0f, null)

        // Draw the mask
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        // Apply style based on input
        return when (style) {
            "agray.jpg" -> androidGrayScale(cropped)
            "blur1.jpg" -> blurImage(cropped, 5)
            "blur2.jpg" -> blurImage(cropped, 10)
            "blur3.jpg" -> blurImage(cropped, 15)
            "sepia.jpg" -> setSepiaColorFilter(cropped)
            else -> cropped // If no style is applied, return the cropped bitmap
        }
    }

    private fun blurImage(input: Bitmap, number: Int): Bitmap {
        return try {
            val rsScript = RenderScript.create(context)
            val alloc = Allocation.createFromBitmap(rsScript, input)
            val blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript))

            // Set different values for different blur effect
            blur.setRadius(number.toFloat())
            blur.setInput(alloc)

            // Reuse the input bitmap for the result
            val result = Bitmap.createBitmap(input)
            val outAlloc = Allocation.createFromBitmap(rsScript, result)
            blur.forEach(outAlloc)
            outAlloc.copyTo(result)

            rsScript.destroy()
            result
        } catch (e: Exception) {
            // TODO: handle exception
            input
        }
    }

    private fun androidGrayScale(bmpOriginal: Bitmap): Bitmap {
        val height: Int = bmpOriginal.height
        val width: Int = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()

        // The conversion is based on OpenCV RGB to GRAY conversion
        // https://docs.opencv.org/master/de/d25/imgproc_color_conversions.html#color_convert_rgb_gray
        // The luminance of each pixel is calculated as the weighted sum of the 3 RGB values
        // Y = 0.299R + 0.587G + 0.114B
        val matrix = floatArrayOf(
            0.299f, 0.587f, 0.114f, 0.0f, 0.0f,
            0.299f, 0.587f, 0.114f, 0.0f, 0.0f,
            0.299f, 0.587f, 0.114f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        val colorMatrixFilter = ColorMatrixColorFilter(matrix)

        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }

    /*private fun blurImage(input: Bitmap, number: Int): Bitmap {
        return try {
            val rsScript = RenderScript.create(context)
            val alloc = Allocation.createFromBitmap(rsScript, input)
            val blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript))
            // Set different values for different blur effect
            blur.setRadius(number.toFloat())
            blur.setInput(alloc)
            val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
            val outAlloc = Allocation.createFromBitmap(rsScript, result)
            blur.forEach(outAlloc)
            outAlloc.copyTo(result)
            rsScript.destroy()
            result
        } catch (e: Exception) {
            // TODO: handle exception
            input
        }
    }*/

    private fun setSepiaColorFilter(bmpOriginal: Bitmap): Bitmap {
        val height: Int = bmpOriginal.height
        val width: Int = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val matrixA = ColorMatrix()
        // making image B&W
        matrixA.setSaturation(0f)
        val matrixB = ColorMatrix()
        // applying scales for RGB color values
        //matrixB.setScale(1f, .95f, .82f, 1.0f)
        matrixB.setScale(1f, .90f, .77f, 1.0f)
        matrixA.setConcat(matrixB, matrixA)
        val colorMatrixFilter = ColorMatrixColorFilter(matrixA)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }

}

fun Int.toAlphaColor(): Int {
    return Color.argb(
        OverlayView.ALPHA_COLOR,
        Color.red(this),
        Color.green(this),
        Color.blue(this)
    )
}