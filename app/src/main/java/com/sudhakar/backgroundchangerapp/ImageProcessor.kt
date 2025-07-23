package com.sudhakar.backgroundchangerapp

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageProcessor {

    // Load the native library for OpenGL ES
    init {
        // Only load if potentially needed (i.e., below API 33)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                System.loadLibrary("image_processor_jni")
                Log.d("ImageProcessor", "Native library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ImageProcessor", "Failed to load native library: ${e.message}")
                // Handle the case where the native library isn't available (e.g., in a non-NDK build)
            }
        }
    }

    // Native method declaration (for OpenGL ES)
    private external fun nativeApplyOpenGLESBlend(
        bgdBitmap: Bitmap,
        fgdBitmap: Bitmap,
        maskBitmap: Bitmap,
        outputBitmap: Bitmap
    )

    // Instance of the AGSL processor (if API 33+)
    private val agslProcessor: ImageProcessorAGSL? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ImageProcessorAGSL() // Re-use the class itself if the AGSL methods are in here
        } else {
            null
        }
    }


    /**
     * Applies the image blending operation using the most performant method available for the device's API level.
     * @param bgdBitmap The background bitmap.
     * @param fgdBitmap The foreground bitmap.
     * @param maskBitmap The mask bitmap.
     * @return The blended output bitmap.
     */
    suspend fun applyBlendOperation(
        bgdBitmap: Bitmap,
        fgdBitmap: Bitmap,
        maskBitmap: Bitmap
    ): Bitmap = withContext(Dispatchers.Default) { // Use Dispatchers.Default for CPU-bound work
        val outputBitmap = Bitmap.createBitmap(bgdBitmap.width, bgdBitmap.height, bgdBitmap.config?:Bitmap.Config.ARGB_8888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("ImageProcessor", "Using AGSL for image blending (API 33+)")
            agslProcessor?.applyAgslBlend(bgdBitmap, fgdBitmap, maskBitmap,bgdBitmap.width,bgdBitmap.height,bgdBitmap.config?:Bitmap.Config.ARGB_8888) ?: outputBitmap
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24 (Android 7.0) minimum for good NDK/OpenGL ES support
            Log.d("ImageProcessor", "Using OpenGL ES (NDK) for image blending (API < 33)")
            try {
                nativeApplyOpenGLESBlend(bgdBitmap, fgdBitmap, maskBitmap, outputBitmap)
                outputBitmap
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ImageProcessor", "NDK not available or failed to load. Falling back to CPU blend. Error: ${e.message}")
                // Fallback to CPU blend if NDK fails (e.g., emulator without NDK support)
                applyCpuBlend(bgdBitmap, fgdBitmap, maskBitmap, outputBitmap)
            } catch (e: Exception) {
                Log.e("ImageProcessor", "Error during NDK OpenGL ES blend: ${e.message}", e)
                // Fallback to CPU blend on other errors
                applyCpuBlend(bgdBitmap, fgdBitmap, maskBitmap, outputBitmap)
            }
        } else {
            Log.d("ImageProcessor", "Using CPU for image blending (API < 24 or NDK fallback)")
            // Fallback for very old devices or if NDK is not available/fails
            applyCpuBlend(bgdBitmap, fgdBitmap, maskBitmap, outputBitmap)
        }
    }

    // AGSL methods go here (from previous section)
    // ... (copy applyAgslBlend and Bitmap.toShader extension from above) ...

    // Helper to read raw text (can be in a companion object or a utility class)
    companion object {
        // This companion object should be within the ImageProcessor class,
        // or ensure AppContextHolder.context is initialized.
        fun Int.readText(): String {
            val context = AppContextHolder.context // Replace with your actual Context access
            return context.resources.openRawResource(this).bufferedReader().use { it.readText() }
        }
    }


    // --- Fallback CPU Blend (Less performant, but always works) ---
    // This should only be used as a fallback if GPU options are not available or fail.
    private fun applyCpuBlend(
        bgdBitmap: Bitmap,
        fgdBitmap: Bitmap,
        maskBitmap: Bitmap,
        outputBitmap: Bitmap
    ): Bitmap {
        val width = bgdBitmap.width
        val height = bgdBitmap.height

        val bgdPixels = IntArray(width * height)
        val fgdPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)

        bgdBitmap.getPixels(bgdPixels, 0, width, 0, 0, width, height)
        fgdBitmap.getPixels(fgdPixels, 0, width, 0, 0, width, height)
        maskBitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x

                val bgd = bgdPixels[index]
                val fgd = fgdPixels[index]
                val msk = maskPixels[index]

                val bgdA = (bgd shr 24) and 0xFF
                val bgdR = (bgd shr 16) and 0xFF
                val bgdG = (bgd shr 8) and 0xFF
                val bgdB = bgd and 0xFF

                val fgdA = (fgd shr 24) and 0xFF
                val fgdR = (fgd shr 16) and 0xFF
                val fgdG = (fgd shr 8) and 0xFF
                val fgdB = fgd and 0xFF

                // For the mask, we'll use the red channel as a general blend factor.
                // Your RS code used mix(bgd, fgd, msk) which means channel-wise.
                // Here, let's simplify by taking one channel from mask and applying it
                // across all RGB channels for blending. Using the red channel as RS did `msk.r`.
                // Convert mask R from 0-255 to 0.0-1.0 float
                val maskRedFloat = ((msk shr 16) and 0xFF) / 255.0f

                // Mix function: out = bgd * (1.0 - msk) + fgd * msk
                val outR = (bgdR * (1.0f - maskRedFloat) + fgdR * maskRedFloat).toInt().coerceIn(0, 255)
                val outG = (bgdG * (1.0f - maskRedFloat) + fgdG * maskRedFloat).toInt().coerceIn(0, 255)
                val outB = (bgdB * (1.0f - maskRedFloat) + fgdB * maskRedFloat).toInt().coerceIn(0, 255)

                // For alpha, you might blend it or take it from foreground/background depending on intent.
                // Simple alpha mix based on mask alpha (if mask has alpha) or just foreground alpha
                val outA = (bgdA * (1.0f - maskRedFloat) + fgdA * maskRedFloat).toInt().coerceIn(0, 255)


                outputPixels[index] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }
}

// Global object to hold application context, needs to be initialized in your Application class
object AppContextHolder {
    lateinit var context: Context
}
