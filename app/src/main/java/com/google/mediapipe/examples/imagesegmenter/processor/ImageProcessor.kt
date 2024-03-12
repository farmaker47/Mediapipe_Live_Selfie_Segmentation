package com.google.mediapipe.examples.imagesegmenter.processor

import android.graphics.Bitmap

// Defines the interface of an image processor that is able to apply HUE rotation and blur effects
// to a bitmap. This interface is implemented with both RenderScript and Vulkan backend as a
// comparison. The RenderScript backend may choose to use intrinsics or compiled scripts.
//
// The image processor is optimized for performing multiple filters on the same input image. The
// caller must configure the input and output images by configureInputAndOutput before invoking any
// filter kernel.
interface ImageProcessor {
    val name: String

    // Set the input image from bitmap and allocate the memories for output images. The user may
    // invoke this method multiple times to switch between different input images.
    fun configureInputAndOutput(inputImage: Bitmap, numberOfOutputImages: Int)

    // Apply hue rotation directly to the input RGBA image. This is equivalent to the combination of
    // RGB->HSV transform * HUE rotation * HSV->RGB transform.
    fun rotateHue(radian: Float, outputIndex: Int): Bitmap

    // Apply gaussian blur to the input image. The radius must be within the range of [1.0, 25.0].
    fun blur(radius: Float, outputIndex: Int, inputImage: Bitmap): Bitmap

    // Frees up any underlying native resources. After calling this method, this image processor
    // can not be used in any way.
    fun cleanup()
}
