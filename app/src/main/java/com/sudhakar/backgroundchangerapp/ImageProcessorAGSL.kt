package com.sudhakar.backgroundchangerapp

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap

// Your AGSL shader code
const val AGSL_SHADER_CODE = """
    uniform shader inputShader;
    uniform shader fgdShader;
    uniform shader maskShader;

    half4 main(float2 coords) {
        half4 bgd = inputShader.eval(coords);
        half4 fgd = fgdShader.eval(coords);
        half4 msk = maskShader.eval(coords);

        half4 outColor = mix(bgd, fgd, msk);
        return outColor;
    }
"""

class ImageProcessorAGSL {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applyAgslBlend(bgdBitmap: Bitmap, fgdBitmap: Bitmap, maskBitmap: Bitmap, width: Int, height: Int, config: Bitmap.Config): Bitmap {
        // 1. Create an ImageReader for off-screen rendering
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)

        // Get the Surface from the ImageReader
        val surface = imageReader.surface

        // 2. Create the RuntimeShader
        val shader = RuntimeShader(AGSL_SHADER_CODE)

        // Set the uniform shaders (input bitmaps as textures)
        shader.setInputShader("inputShader", bgdBitmap.toShader())
        shader.setInputShader("fgdShader", fgdBitmap.toShader())
        shader.setInputShader("maskShader", maskBitmap.toShader())

        // Create a Paint and apply the shader
        val paint = Paint()
        paint.shader = shader

        // 3. Create a RenderNode for the drawing operations
        val renderNode = RenderNode("AGSL Render Node")
        renderNode.setPosition(0, 0, width, height)

        // 4. Get the drawing Canvas from the RenderNode
        val renderCanvas = renderNode.beginRecording()

        // 5. Draw your shader-based content onto the RenderNode's Canvas
        renderCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 6. End recording
        renderNode.endRecording()

        // 7. Create a HardwareRenderer for off-screen rendering
        val hardwareRenderer = HardwareRenderer()
        hardwareRenderer.setSurface(surface)
        hardwareRenderer.setContentRoot(renderNode)
        hardwareRenderer.setLightSourceAlpha(0f, 0f)
        hardwareRenderer.setLightSourceGeometry(0f, 0f, 0f, 0f)
        hardwareRenderer.setOpaque(false)

        var outputBitmap: Bitmap? = null
        try {
            // Use synchronous rendering with FrameRenderRequest
            val renderRequest = hardwareRenderer.createRenderRequest()
            renderRequest.setWaitForPresent(true)
            val syncResult = renderRequest.syncAndDraw()

            // Check the result (optional, for debugging)
            if (syncResult != HardwareRenderer.SYNC_OK) {
                Log.w("ImageProcessor", "Sync and draw result: $syncResult")
            }

            // Acquire the rendered image from ImageReader
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                // Convert Image to Bitmap
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bufferBitmapWidth = width + rowPadding / pixelStride

                outputBitmap = createBitmap(bufferBitmapWidth, height, config)
                outputBitmap.copyPixelsFromBuffer(buffer)
                image.close()
            } else {
                Log.e("ImageProcessor", "Failed to acquire image from ImageReader.")
            }
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Error during AGSL blend with ImageReader: ${e.message}", e)
            throw e
        } finally {
            // Release resources
            hardwareRenderer.destroy()
            imageReader.close()
            surface.release()
        }

        return outputBitmap ?: throw IllegalStateException("AGSL blend failed to produce output bitmap")
    }

    // Extension function to easily convert Bitmap to BitmapShader
    fun Bitmap.toShader(): Shader {
        return BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
}